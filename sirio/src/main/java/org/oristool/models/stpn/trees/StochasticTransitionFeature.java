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
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.tpn.TimedTransitionFeature;
import org.oristool.petrinet.TransitionFeature;

/**
 * Transition feature encoding the distribution and weight.
 */
public class StochasticTransitionFeature implements TransitionFeature {

    private final PartitionedFunction density;
    private final MarkingExpr weight;
    private final MarkingExpr rate;

    /**
     * Returns the firing time density of this instance.
     *
     * @return firing time PDF
     */
    public PartitionedFunction density() {
        return this.density;
    }

    /**
     * Returns the weight this instance. The weight is used to resolve random
     * switches between immediate and deterministic transitions.
     *
     * @return transition weight
     */
    public MarkingExpr weight() {
        return this.weight;
    }

    /**
     * Returns the rate used to decrease the firing time of this transitions. Most
     * analysis methods require rate equal to {@code MarkingExpr.ONE}.
     *
     * @return transition rate
     */
    public MarkingExpr rate() {
        return this.rate;
    }

    private StochasticTransitionFeature(PartitionedFunction density,
            MarkingExpr weight, MarkingExpr rate) {

        this.density = density;
        this.weight = weight;
        this.rate = rate;
    }

    public static StochasticTransitionFeature of(PartitionedFunction density) {
        return new StochasticTransitionFeature(density, MarkingExpr.ONE, MarkingExpr.ONE);
    }

    public static StochasticTransitionFeature of(PartitionedFunction density,
            MarkingExpr weight, MarkingExpr rate) {

        return new StochasticTransitionFeature(density, weight, rate);
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
                new BigDecimal(eft),
                new BigDecimal(lft));
    }

    /**
     * Builds the stochastic feature of a transition with uniformly distributed
     * timer. Weight and rate are equal to 1.
     *
     * @param eft minimum firing time
     * @param lft maximum firing time
     * @return a stochastic feature with uniform distribution
     */
    public static StochasticTransitionFeature newUniformInstance(
            BigDecimal eft, BigDecimal lft) {
        return StochasticTransitionFeature.of(GEN.newUniform(
                new OmegaBigDecimal(eft), new OmegaBigDecimal(lft)));
    }

    /**
     * Builds the stochastic feature of a transition with uniformly distributed
     * timer. Weight is equal to 1.
     *
     * @param eft minimum firing time
     * @param lft maximum firing time
     * @param scalingRate scaling rate (depends on the state marking)
     * @return a stochastic feature with uniform distribution
     */
    public static StochasticTransitionFeature newUniformInstance(
            BigDecimal eft, BigDecimal lft, MarkingExpr scalingRate) {
        return StochasticTransitionFeature.of(GEN.newUniform(
                new OmegaBigDecimal(eft), new OmegaBigDecimal(lft)), MarkingExpr.ONE, scalingRate);
    }

    /**
     * Builds the stochastic feature of a transition with deterministic timer. The
     * rate and weight are equal to 1.
     *
     * @param value timer value
     * @return a stochastic feature with deterministic distribution
     */
    public static StochasticTransitionFeature newDeterministicInstance(
            String value) {
        return StochasticTransitionFeature.newDeterministicInstance(
                new BigDecimal(value), MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with deterministic timer. The
     * rate and weight are equal to 1.
     *
     * @param value timer value
     * @return a stochastic feature with deterministic distribution
     */
    public static StochasticTransitionFeature newDeterministicInstance(
            BigDecimal value) {
        return StochasticTransitionFeature.newDeterministicInstance(value, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with deterministic timer. The
     * rate is always equal to 1.
     *
     * @param value timer value
     * @param weight weight of the transition
     * @return a stochastic feature with deterministic distribution
     */
    public static StochasticTransitionFeature newDeterministicInstance(
            BigDecimal value, MarkingExpr weight) {

        return StochasticTransitionFeature.of(GEN.newDeterministic(value),
                weight, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with deterministic timer. The
     * rate is always equal to 1.
     *
     * @param value timer value
     * @param weight weight of the transition
     * @param scalingRate scaling rate (depends on the state marking)
     * @return a stochastic feature with deterministic distribution
     */
    public static StochasticTransitionFeature newDeterministicInstance(
            BigDecimal value, MarkingExpr weight, MarkingExpr scalingRate) {

        return StochasticTransitionFeature.of(GEN.newDeterministic(value),
                weight, scalingRate);
    }

    /**
     * Builds the stochastic feature of a transition with exponentially distributed
     * timer. The transition rate does not depend on the current marking.
     *
     * @param expRate rate of the exponential
     * @return a stochastic feature with exponential distribution
     */
    public static StochasticTransitionFeature newExponentialInstance(
            String expRate) {
        return StochasticTransitionFeature.newExponentialInstance(new BigDecimal(expRate),
                MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with exponentially distributed
     * timer. The transition rate does not depend on the current marking.
     *
     * @param expRate rate of the exponential
     * @return a stochastic feature with exponential distribution
     */
    public static StochasticTransitionFeature newExponentialInstance(
            BigDecimal expRate) {
        return StochasticTransitionFeature.newExponentialInstance(expRate, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with exponentially distributed
     * timer and variable rate. The rate of the exponential is rescaled in each
     * state after evaluating the input rate parameter.
     *
     * @param expRate rate of the exponential
     * @param scalingRate scaling rate (depends on the state marking)
     * @return a stochastic feature with exponential distribution and variable rate
     */
    public static StochasticTransitionFeature newExponentialInstance(BigDecimal expRate,
            MarkingExpr scalingRate) {

        return StochasticTransitionFeature.of(new EXP(Variable.X, expRate),
                MarkingExpr.ONE, scalingRate);
    }

    /**
     * Builds the stochastic feature of a transition with Erlang distributed timer.
     * Weight and rescaling rate are set to 1.
     *
     * @param rate rate of the exponentials in the Erlang (rate)
     * @param k number of exponentials in the Erlang (shape)
     * @return a stochastic feature with Erlang distribution
     */
    public static StochasticTransitionFeature newErlangInstance(int k, String rate) {
        return StochasticTransitionFeature.newErlangInstance(k, new BigDecimal(rate));
    }

    /**
     * Builds the stochastic feature of a transition with Erlang distributed timer.
     * Weight and rescaling rate are set to 1.
     *
     * @param rate rate of the exponentials in the Erlang (rate)
     * @param k number of exponentials in the Erlang (shape)
     * @return a stochastic feature with Erlang distribution
     */
    public static StochasticTransitionFeature newErlangInstance(int k, BigDecimal rate) {
        return StochasticTransitionFeature.of(new Erlang(Variable.X, k, rate),
                MarkingExpr.ONE, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition with Erlang distributed timer.
     * The weight is set to 1.
     *
     * @param rate rate of the exponentials in the Erlang (rate)
     * @param k number of exponentials in the Erlang (shape)
     * @param scalingRate scaling rate (depends on the state marking)
     * @return a stochastic feature with Erlang distribution
     */
    public static StochasticTransitionFeature newErlangInstance(int k, BigDecimal rate,
            MarkingExpr scalingRate) {
        return StochasticTransitionFeature.of(new Erlang(Variable.X, k, rate),
                MarkingExpr.ONE, scalingRate);
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * a shifted exponential. Weight and rescaling rate are set to 1.
     *
     * @param shift maximum firing time
     * @param rate rate of the exponential
     * @return a stochastic feature with truncated exponential distribution
     */
    public static StochasticTransitionFeature newShiftedExp(BigDecimal shift, BigDecimal rate) {

        return StochasticTransitionFeature.of(GEN.newShiftedExp(shift, rate),
                MarkingExpr.ONE, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * an expolynomial. Weight and rescaling rate are set to 1.
     *
     * @param density density expression
     * @param eft minimum firing time
     * @param lft maximum firing time
     * @return a stochastic feature with expolynomial distribution
     */
    public static StochasticTransitionFeature newExpolynomial(String density,
            OmegaBigDecimal eft, OmegaBigDecimal lft) {

        return StochasticTransitionFeature.of(GEN.newExpolynomial(density, eft, lft),
                MarkingExpr.ONE, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * hyper-exponential (mixture of exponentials). Weight and rescaling rate are
     * set to 1.
     *
     * @param probs probability distribution over exponential PDFs
     * @param rates rates of exponential PDFs
     * @return mixture distribution
     */
    public static StochasticTransitionFeature newHyperExp(List<BigDecimal> probs,
            List<BigDecimal> rates) {

        return StochasticTransitionFeature.of(GEN.newHyperExp(probs, rates),
                MarkingExpr.ONE, MarkingExpr.ONE);
    }

    /**
     * Builds the stochastic feature of a transition where the timer distribution is
     * hypo-exponential (sum of exponentials). Weight and rescaling rate are set to 1.
     *
     * @param rate1 rate of the first exponential
     * @param rate2 rate of the second exponential
     * @return transition feature distributed as the sum of the exponentials
     */
    public static StochasticTransitionFeature newHypoExp(BigDecimal rate1, BigDecimal rate2) {

        return StochasticTransitionFeature.of(GEN.newHypoExp(rate1, rate2),
                MarkingExpr.ONE, MarkingExpr.ONE);
    }

    /**
     * Checks if this feature represents an immediate transition.
     *
     * @return true if this feature is immediate
     */
    public boolean isIMM() {
        if (density().getDensities().size() != 1 || this.density().getDomains().size() != 1)
            return false;

        Expolynomial f = density().getDensities().get(0);
        DBMZone z = density().getDomains().get(0);

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

    /**
     * Checks if this feature represents an exponential transition.
     *
     * @return true if this feature is exponential
     */
    public boolean isEXP() {

        return density() instanceof EXP;
    }

    /**
     * Returns a TPN transition feature encoding only the minimum and maximum value
     * for a transition timer.
     *
     * @return a timed transition feature discarding the PDF
     */
    public TimedTransitionFeature asTimedTransitionFeature() {

        return new TimedTransitionFeature(
                density().getDomainsEFT(), density().getDomainsLFT());
    }
}
