/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
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
import java.math.MathContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.EXP;
import org.oristool.math.function.StateDensityFunction;
import org.oristool.models.pn.MarkingUpdater;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.PetriSuccessionEvaluator;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Builder of successor states for stochastic time Petri nets.
 */
public class StochasticSuccessionEvaluator implements
        SuccessionEvaluator<PetriNet, Transition> {

    private PetriSuccessionEvaluator petriSuccessionEvaluator;
    private boolean transientAnalysis;

    /**
     * Builds a succession evaluator with a given token remover and adder.
     *
     * <p>If {@code checkNewlyEnabled} is true, states with the same marking are
     * considered different if they have a different set of newly-enabled
     * transitions.
     *
     * <p>If {@code excludeZeroProb} is true, transition firings with zero
     * probability are excluded.
     *
     * @param transientAnalysis whether to include {@code Variable.AGE}
     * @param tokensRemover the object used to add tokens after a firing
     * @param tokensAdder the object used to remove tokens after a firing
     * @param checkNewlyEnabled whether to compare the sets of newly-enabled
     *        transitions of states
     * @param tauAgeLimit time bound for the successors
     */
    public StochasticSuccessionEvaluator(boolean transientAnalysis,
            MarkingUpdater tokensRemover, MarkingUpdater tokensAdder,
            boolean checkNewlyEnabled, OmegaBigDecimal tauAgeLimit) {

        this.petriSuccessionEvaluator = new PetriSuccessionEvaluator(
                tokensRemover, tokensAdder, checkNewlyEnabled);

        this.transientAnalysis = transientAnalysis;
    }

    @Override
    public Succession computeSuccession(PetriNet petriNet, State state,
            Transition fired) {

        final Succession succession = petriSuccessionEvaluator.computeSuccession(
                petriNet, state, fired);
        final PetriStateFeature prevPetriStateFeature = succession.getParent()
                .getFeature(PetriStateFeature.class);
        final PetriStateFeature nextPetriStateFeature = succession.getChild()
                .getFeature(PetriStateFeature.class);
        final StochasticStateFeature prevStochasticStateFeature = succession
                .getParent().getFeature(StochasticStateFeature.class);
        final StochasticStateFeature nextStochasticStateFeature = new StochasticStateFeature(
                prevStochasticStateFeature);
        final StateDensityFunction prevStateDensity = prevStochasticStateFeature
                .getStateDensity();
        final StateDensityFunction nextStateDensity = nextStochasticStateFeature
                .getStateDensity();

        Variable firedVar = new Variable(fired.getName());

        // EXPs are not part of the StateDensity
        Set<Variable> otherNonExpVars = prevStochasticStateFeature
                .getFiringVariables();
        otherNonExpVars.remove(firedVar);

        BigDecimal prob = BigDecimal.ONE;

        // adds EXP variables to the partitionedGEN
        Variable minEXP = new Variable("minEXP");
        if (prevStochasticStateFeature.getEXPVariables().size() > 0) {
            // multiplies every GEN by a minEXP (which might be truncated in the
            // domain conditioning)
            nextStochasticStateFeature.addTruncatedExp(minEXP,
                    prevStochasticStateFeature.getTotalExpRate(),
                    OmegaBigDecimal.POSITIVE_INFINITY);
            otherNonExpVars.add(minEXP);
        }

        // turns an EXP firing into a GEN firing
        if (prevStochasticStateFeature.getEXPVariables().contains(firedVar)) {

            // multiplies the firing probability by the rate switch probability
            prob = prob.multiply(
                    prevStochasticStateFeature.getEXPRate(firedVar)).divide(
                    prevStochasticStateFeature.getTotalExpRate(),
                    MathContext.DECIMAL128);

            // the fired variable is now minEXP
            nextStochasticStateFeature.removeExpVariable(firedVar);
            firedVar = minEXP;

        } else {

            // computes the random switch probability
            Set<Variable> nullDelayVariables = prevStateDensity.getNullDelayVariables(firedVar);
            nullDelayVariables.remove(Variable.AGE);

            if (nullDelayVariables.size() > 0) {
                Set<Transition> nullDelayTransitions = new HashSet<>();
                nullDelayTransitions.add(fired);
                for (Variable v : nullDelayVariables)
                    nullDelayTransitions.add(petriNet.getTransition(v.toString()));

                Set<Transition> maxPriority = Priority.maxPriority(nullDelayTransitions);
                if (!maxPriority.contains(fired))
                    return null;

                if (maxPriority.size() > 1) {
                    Map<Transition, Double> switchProbs = StochasticTransitionFeature.weightProbs(
                            maxPriority, prevPetriStateFeature.getMarking());
                    prob = prob.multiply(new BigDecimal(switchProbs.get(fired)));
                }
            }
        }

        // conditioning the state density
        BigDecimal minProbability = nextStateDensity.conditionAllToBound(
                firedVar, otherNonExpVars, OmegaBigDecimal.ZERO);
        if (minProbability.compareTo(BigDecimal.ZERO) == 0)
            return null;

        prob = prob.multiply(minProbability);

        // time advancement and projection (fired is either DET or a
        // PartitionedGEN variable)
        nextStateDensity.shiftAndProject(firedVar);

        // removes minEXP
        if (prevStochasticStateFeature.getEXPVariables().size() > 0
                && !firedVar.equals(minEXP))
            nextStateDensity.marginalizeVariable(minEXP);

        // disabling
        for (Variable v : Transition
                .newVariableSetInstance(nextPetriStateFeature.getDisabled())) {
            if (nextStochasticStateFeature.getEXPVariables().contains(v))
                nextStochasticStateFeature.removeExpVariable(v);
            else
                nextStateDensity.marginalizeVariable(v);
        }

        // adding the transient succession feature
        if (transientAnalysis) {
            TransientStochasticStateFeature tssf = new TransientStochasticStateFeature();
            tssf.setReachingProbability(state
                    .getFeature(TransientStochasticStateFeature.class)
                    .getReachingProbability().multiply(prob, MathContext.DECIMAL128));

            if (nextStateDensity.getVariables().equals(
                    Collections.singleton(Variable.AGE)))
                // optimization: can save a copy of the state density as the `age` density
                tssf.setEnteringTimeDensity(new StateDensityFunction(nextStateDensity));

            succession.getChild().addFeature(tssf);
        }

        // newly enabling
        for (Transition t : nextPetriStateFeature.getNewlyEnabled()) {
            nextStochasticStateFeature.addVariable(new Variable(t.getName()), t
                    .getFeature(StochasticTransitionFeature.class)
                    .density());
        }

        // updating rates of all EXPs with a RateExpressionFeature
        for (Transition t : nextPetriStateFeature.getEnabled()) {
            StochasticTransitionFeature tf = t.getFeature(StochasticTransitionFeature.class);
            if (tf.isEXP() && !tf.clockRate().equals(MarkingExpr.ONE)) {
                BigDecimal clockRate = new BigDecimal(tf.clockRate()
                        .evaluate(nextPetriStateFeature.getMarking()));
                BigDecimal rate = ((EXP)tf.density()).getLambda();
                nextStochasticStateFeature.setEXPRate(new Variable(t.getName()),
                        rate.multiply(clockRate));
            }
        }

        // flags update
        Set<Transition> nextEnabled = nextPetriStateFeature.getEnabled();
        boolean hasEnabledImm = false;
        for (Entry<Variable, BigDecimal> e : nextStateDensity
                .getDeterministicValues()) {
            if (!e.getKey().equals(Variable.AGE)
                    && e.getValue().compareTo(BigDecimal.ZERO) == 0) {
                hasEnabledImm = true;
                break;
            }
        }

        nextStochasticStateFeature.setVanishing(hasEnabledImm);
        nextStochasticStateFeature.setAbsorbing(nextEnabled.isEmpty());

        // adding the stochastic succession feature
        StochasticSuccessionFeature ssf = new StochasticSuccessionFeature(prob);
        succession.addFeature(ssf);

        succession.getChild().addFeature(nextStochasticStateFeature);

        return succession;
    }
}
