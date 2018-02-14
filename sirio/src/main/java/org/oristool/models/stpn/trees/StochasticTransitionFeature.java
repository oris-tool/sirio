/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2018 The ORIS Authors.
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
import java.util.List;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.EXP;
import org.oristool.math.function.Erlang;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.models.tpn.TimedTransitionFeature;
import org.oristool.petrinet.TransitionFeature;

/**
 * Transition feature encoding the distribution and weight.
 */
public class StochasticTransitionFeature implements TransitionFeature {

    private PartitionedFunction firingTimeDensity;
    private BigDecimal weight;

    public PartitionedFunction getFiringTimeDensity() {
        return firingTimeDensity;
    }

    public void setFiringTimeDensity(PartitionedFunction firingTimeDensity) {
        this.firingTimeDensity = firingTimeDensity;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof StochasticTransitionFeature))
            return false;

        StochasticTransitionFeature o = (StochasticTransitionFeature) obj;

        return this.weight.equals(o.weight)
                && this.firingTimeDensity.equals(o.firingTimeDensity);
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + firingTimeDensity.hashCode();
        result = 31 * result + weight.hashCode();

        return result;
    }

    /**
     * Builds the stochastic feature of a transition with uniformly distributed
     * timer.
     *
     * @param eft minimum firing time (as a string)
     * @param lft maximum firing time (as a string)
     * @return a stochastic feature with uniform distribution
     */
    public static StochasticTransitionFeature newUniformInstance(
            String eft, String lft) {

        return StochasticTransitionFeature.newUniformInstance(
                new OmegaBigDecimal(eft),
                new OmegaBigDecimal(lft),
                BigDecimal.ONE);
    }

    public static StochasticTransitionFeature newUniformInstance(
            OmegaBigDecimal eft, OmegaBigDecimal lft) {
        return StochasticTransitionFeature.newUniformInstance(eft, lft,
                BigDecimal.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with uniformly distributed
     * timer.
     *
     * @param eft minimum firing time
     * @param lft maximum firing time
     * @param weight weight of the transition
     * @return a stochastic feature with uniform distribution
     */
    public static StochasticTransitionFeature newUniformInstance(
            OmegaBigDecimal eft, OmegaBigDecimal lft, BigDecimal weight) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();

        newFeature.firingTimeDensity = GEN.newUniform(eft, lft);
        newFeature.weight = weight;

        return newFeature;
    }

    public static StochasticTransitionFeature newDeterministicInstance(
            BigDecimal value) {
        return StochasticTransitionFeature.newDeterministicInstance(value,
                BigDecimal.ONE);
    }

    public static StochasticTransitionFeature newDeterministicInstance(
            BigDecimal value, String weight) {
        // FIXME: gestire il caso string
        return newDeterministicInstance(value, new BigDecimal(weight));
    }

    /**
     * Builds the stochastic feature of a transition with deterministic timer.
     *
     * @param value timer value
     * @param weight weight of the transition
     * @return a stochastic feature with deterministic distribution
     */
    public static StochasticTransitionFeature newDeterministicInstance(
            BigDecimal value, BigDecimal weight) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();
        newFeature.firingTimeDensity = GEN.newDeterministic(value);
        newFeature.weight = weight;

        return newFeature;
    }

    public static StochasticTransitionFeature newExponentialInstance(
            BigDecimal rate) {
        return StochasticTransitionFeature.newExponentialInstance(rate,
                BigDecimal.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with exponentially distributed
     * timer.
     *
     * @param rate rate of the exponential
     * @param weight weight of the transition
     * @return a stochastic feature with exponential distribution
     */
    public static StochasticTransitionFeature newExponentialInstance(
            BigDecimal rate, BigDecimal weight) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();

        newFeature.firingTimeDensity = new EXP(Variable.X, rate);
        newFeature.weight = weight;

        return newFeature;
    }

    /**
     * Builds the stochastic feature of a transition Erlang distributed timer.
     *
     * @param rate rate of the exponentials in the Erlang
     * @param k number of exponentials in the Erlang
     * @return a stochastic feature with Erlang distribution
     */
    public static StochasticTransitionFeature newErlangInstance(int k,
            BigDecimal rate) {
        return StochasticTransitionFeature.newErlangInstance(rate, k,
                BigDecimal.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with Erlang distributed timer.
     *
     * @param rate rate of the exponentials in the Erlang
     * @param k number of exponentials in the Erlang
     * @param weight weight of the transition
     * @return a stochastic feature with Erlang distribution
     */
    public static StochasticTransitionFeature newErlangInstance(
            BigDecimal rate, int k, BigDecimal weight) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();

        newFeature.firingTimeDensity = new Erlang(Variable.X, k, rate);
        newFeature.weight = weight;

        return newFeature;
    }

    /**
     * Returns a TPN transition feature encoding only the minimum and maximum value
     * for a transition timer.
     *
     * @return a timed transition feature discarding the PDF
     */
    public TimedTransitionFeature asTimedTransitionFeature() {

        return new TimedTransitionFeature(
                firingTimeDensity.getDomainsEFT(), firingTimeDensity.getDomainsLFT());
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * a shifted exponential.
     *
     * @param shift maximum firing time
     * @param rate rate of the exponential
     * @return a stochastic feature with truncated exponential distribution
     */
    public static StochasticTransitionFeature newShiftedExp(BigDecimal shift, BigDecimal rate) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();
        newFeature.firingTimeDensity = GEN.newShiftedExp(shift, rate);
        newFeature.weight = BigDecimal.ONE; // weights are superfluous for continuous PDFs

        return newFeature;
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * an expolynomial.
     *
     * @param density density expression
     * @param eft minimum firing time
     * @param lft maximum firing time
     * @return a stochastic feature with expolynomial distribution
     */
    public static StochasticTransitionFeature newExpolynomial(String density,
            OmegaBigDecimal eft, OmegaBigDecimal lft) {
        return StochasticTransitionFeature.newExpolynomial(density, eft, lft,
                BigDecimal.ONE);
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * an expolynomial.
     *
     * @param density density expression
     * @param eft minimum firing time
     * @param lft maximum firing time
     * @param weight weight of the transition
     * @return a stochastic feature with expolynomial distribution
     */
    public static StochasticTransitionFeature newExpolynomial(String density,
            OmegaBigDecimal eft, OmegaBigDecimal lft, BigDecimal weight) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();
        newFeature.firingTimeDensity = GEN.newExpolynomial(density, eft, lft);
        newFeature.weight = weight;

        return newFeature;
    }


    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * hyper-exponential (mixture of exponentials).
     *
     * @param probs probability distribution over exponential PDFs
     * @param rates rates of exponential PDFs
     * @return mixture distribution
     */
    public static StochasticTransitionFeature newHyperExp(List<BigDecimal> probs,
            List<BigDecimal> rates) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();
        newFeature.firingTimeDensity = GEN.newHyperExp(probs, rates);
        newFeature.weight = BigDecimal.ONE; // weights are superfluous for continuous PDFs

        return newFeature;
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * hypo-exponential (sum of exponentials).
     *
     * @param rate1 rate of the first exponential
     * @param rate2 rate of the second exponential
     * @return transition feature distributed as the sum of the exponentials
     */
    public static StochasticTransitionFeature newHypoExp(BigDecimal rate1, BigDecimal rate2) {

        StochasticTransitionFeature newFeature = new StochasticTransitionFeature();
        newFeature.firingTimeDensity = GEN.newHypoExp(rate1, rate2);
        newFeature.weight = BigDecimal.ONE; // weights are superfluous for continuous PDFs

        return newFeature;
    }

    /** Checks if this feature represents an immediate transitions.
     *
     * @return true if this feature is immediate
     */
    public boolean isIMM() {
        //Check if it is a piecewise
        if (this.firingTimeDensity.getDensities().size() != 1
                || this.firingTimeDensity.getDomains().size() != 1)
            return false;
        Expolynomial f = this.firingTimeDensity.getDensities().get(0);
        DBMZone z = this.firingTimeDensity.getDomains().get(0);

        if (f.getExmonomials().size() != 1
                || f.getExmonomials().get(0).getAtomicTerms().size() > 0)
            return false;

        OmegaBigDecimal c = f.getExmonomials().get(0).getConstantTerm();
        OmegaBigDecimal eft = z.getBound(Variable.TSTAR, Variable.X).negate();
        OmegaBigDecimal lft = z.getBound(Variable.X, Variable.TSTAR);

        return c.equals(OmegaBigDecimal.ONE)
                && eft.equals(OmegaBigDecimal.ZERO)
                && lft.equals(OmegaBigDecimal.ZERO);
    }
}
