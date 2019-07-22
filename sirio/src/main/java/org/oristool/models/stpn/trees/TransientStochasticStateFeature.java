/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2019 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.analyzer.state.StateFeature;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.StateDensityFunction;

/**
 * State feature including the entering time distribution and reaching
 * probability of a state.
 */
public class TransientStochasticStateFeature implements StateFeature {

    private StateDensityFunction enteringTimeDensity;
    private BigDecimal reachingProbability;

    /**
     * Builds an empty state feature.
     */
    public TransientStochasticStateFeature() {

    }

    /**
     * Builds the copy of a state feature.
     *
     * @param other another state feature
     */
    public TransientStochasticStateFeature(TransientStochasticStateFeature other) {
        this.enteringTimeDensity = new StateDensityFunction(
                other.enteringTimeDensity);
        this.reachingProbability = other.reachingProbability;
    }

    public void setEnteringTimeDensity(StateDensityFunction enteringTimeDensity) {
        this.enteringTimeDensity = enteringTimeDensity;
    }

    public BigDecimal getReachingProbability() {
        return reachingProbability;
    }

    public void setReachingProbability(BigDecimal p) {
        this.reachingProbability = p;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("Reaching probability: ");
        b.append(reachingProbability);
        b.append("\n");
        b.append("Entering time density: \n");
        if (enteringTimeDensity != null)
            b.append(enteringTimeDensity.toString().replaceAll("^|(\\n)", "$1" + "  "));

        return b.toString();
    }

    /**
     * Returns the entering time density of a given instance.
     *
     * @param s stochastic state feature
     * @return entering time density
     */
    public StateDensityFunction getEnteringTimeDensity(
            StochasticStateFeature s) {

        if (enteringTimeDensity == null)
            enteringTimeDensity = s.getStateDensity().getMarginalDensity(
                    Variable.AGE);

        return enteringTimeDensity;
    }

    /**
     * Returns the upper bound on the entering time for this transient state
     * class.
     *
     * @param s stochastic state feature
     * @return OmegaBigDecimal upper time bound
     */
    public OmegaBigDecimal getEnteringTimeUpperBound(StochasticStateFeature s) {
        // FIXME: cache value
        return s.getStateDensity().getMaxBound(Variable.TSTAR, Variable.AGE);
    }

    /**
     * Returns the lower bound on the entering time for this transient state
     * class.
     *
     * @param s stochastic state feature
     * @return OmegaBigDecimal upper time bound
     */
    public OmegaBigDecimal getEnteringTimeLowerBound(StochasticStateFeature s) {
        // FIXME: cache value
        return s.getStateDensity().getMaxBound(Variable.AGE, Variable.TSTAR)
                .negate();
    }

    /**
     * After timeUpperBound the probability of being in this class is zero.
     *
     * @param s stochastic state feature
     * @return OmegaBigDecimal upper bound
     */
    public OmegaBigDecimal getTimeUpperBound(StochasticStateFeature s) {

        // FIXME: cache value
        // timeUpperBound is the tightest constraint of the form v - tauAge < b
        return s.getStateDensity().getMinBound(s.getFiringVariables(),
                Collections.singleton(Variable.AGE));
    }

    /**
     * Computes the probability of being in this class at the specified time.
     *
     * @param time time point
     * @param s stochastic state feature
     * @return BigDecimal probability value
     */
    public BigDecimal computeTransientClassProbability(OmegaBigDecimal time,
            StochasticStateFeature s) {

        /*
         * The probability of being in this class is the integral of the density
         * (including DETS) over the DBM domain restricted by the constraints
         * (entering time) <= time (entering time) + (time to fire) > time
         *
         * which is
         *
         * (-age) <= time (-age) + v > time
         *
         * for each time to fire variable v other than age, t*.
         */

        StateDensityFunction restrictedStateDensity = new StateDensityFunction(
                s.getStateDensity());
        Set<Variable> firingVariablesWithExp = new LinkedHashSet<Variable>(
                s.getFiringVariables());

        // adds EXP variables to the partitionedGEN
        if (s.getEXPVariables().size() > 0) {
            // multiplies every GEN by a minEXP (which might get truncated in
            // the domain conditioning)
            Variable minEXP = new Variable("minEXP");
            GEN exp = GEN.newTruncatedExp(Variable.X, s.getTotalExpRate(),
                    OmegaBigDecimal.ZERO, OmegaBigDecimal.POSITIVE_INFINITY);
            restrictedStateDensity.addContinuousVariable(minEXP, exp);

            firingVariablesWithExp.add(minEXP);
        }

        // Adds constraints: tauAge in [-t, 0]
        restrictedStateDensity.imposeBound(Variable.TSTAR, Variable.AGE, time);
        restrictedStateDensity.imposeBound(Variable.AGE, Variable.TSTAR,
                OmegaBigDecimal.ZERO);

        // Adds constraints: (-tauAge) + v > t i.e. tauAge - v < -t
        restrictedStateDensity.imposeBound(Variable.AGE,
                firingVariablesWithExp, time.negate().toLeftNeighborhood());

        return restrictedStateDensity.measure().multiply(reachingProbability);
    }

    /**
     * Computes the probability of reaching this class at a time within the
     * given interval [alpha,beta].
     *
     * @param alpha lower bound
     * @param beta upper bound
     * @param s stochastic state feature
     * @return BigDecimal probability value
     */
    public BigDecimal computeVisitedProbability(OmegaBigDecimal alpha,
            OmegaBigDecimal beta, StochasticStateFeature s) {

        // Adds constraints: tauAge in [-beta, -alpha] (note that tauAge is
        // never EXP)
        return new StateDensityFunction(this.getEnteringTimeDensity(s))
                .conditionToInterval(Variable.AGE, beta.negate(),
                        alpha.negate()).multiply(this.getReachingProbability());
    }
}
