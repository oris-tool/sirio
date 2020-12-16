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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.EXP;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.math.function.StateDensityFunction;
import org.oristool.models.pn.InitialPetriStateBuilder;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * State builder for stochastic time Petri nets.
 */
public final class DeterministicEnablingStateBuilder implements
        StateBuilder<DeterministicEnablingState> {

    private final PetriNet petriNet;
    private final boolean transientAnalysis;
    private final boolean checkNewlyEnabled;
    private final BigDecimal epsilon;
    private final int numSamples;

    public DeterministicEnablingStateBuilder(PetriNet petriNet,
            boolean transientAnalysis) {
        this(petriNet, transientAnalysis, false, BigDecimal.ZERO, 0);
    }

    /**
     * Configures a state builder for STPNs.
     *
     * @param petriNet Petri net
     * @param transientAnalysis whether the state should include
     *        {@code Variable.AGE}
     * @param checkNewlyEnabled whether to compare the sets of newly enabled
     *        transitions of states
     * @param epsilon allowed error when comparing states
     * @param numSamples number of samples used when comparing states
     */
    public DeterministicEnablingStateBuilder(PetriNet petriNet,
            boolean transientAnalysis, boolean checkNewlyEnabled,
            BigDecimal epsilon, int numSamples) {

        this.petriNet = petriNet;
        this.transientAnalysis = transientAnalysis;
        this.checkNewlyEnabled = checkNewlyEnabled;
        this.epsilon = epsilon;
        this.numSamples = numSamples;
    }

    /**
     * Builds an initial {@link State} instance from a
     * {@link DeterministicEnablingState}.
     *
     * <p>The state includes a {@code PetriStateFeature} and
     * {@code StochasticStateFeature}.
     *
     * @param s a deterministic enabling state
     * @return a state instance
     */
    @Override
    public State build(DeterministicEnablingState s) {

        // adds the petri state feature
        State state = InitialPetriStateBuilder.computeInitialState(petriNet,
                s.getMarking(), checkNewlyEnabled);
        Set<Transition> enabledTransitions = state.getFeature(
                PetriStateFeature.class).getNewlyEnabled();

        // builds the stochastic state feature
        StochasticStateFeature ssf = new StochasticStateFeature(epsilon,
                numSamples);
        StateDensityFunction f = new StateDensityFunction();
        ssf.setStateDensity(f);

        // builds the initial state density function through Cartesian products
        Map<Variable, BigDecimal> enablingTimes = s.getEnablingTimes();
        for (Transition t : enabledTransitions) {
            Variable v = new Variable(t.getName());

            PartitionedFunction density = t.getFeature(StochasticTransitionFeature.class)
                    .density();

            if (enablingTimes.containsKey(v))
                ssf.addVariableReduced(v, density, enablingTimes.get(v));
            else if (density instanceof EXP)
                ssf.addVariable(v, density);
            else
                throw new IllegalArgumentException("The GEN transition " + v
                        + " does not have a deterministic enabling time");
        }

        // updates rates of all EXPs with a RateExpressionFeature
        for (Transition t : enabledTransitions) {
            StochasticTransitionFeature tf = t.getFeature(StochasticTransitionFeature.class);
            if (tf.isEXP() && !tf.clockRate().equals(MarkingExpr.ONE)) {
                BigDecimal scalingRate = new BigDecimal(tf.clockRate().evaluate(s.getMarking()));
                BigDecimal rate = ((EXP)tf.density()).getLambda();
                ssf.setEXPRate(new Variable(t.getName()), rate.multiply(scalingRate));
            }
        }

        // adds the regeneration object
        state.addFeature(new Regeneration<DeterministicEnablingState>(s));

        // flags update
        boolean hasEnabledImm = false;
        for (Entry<Variable, BigDecimal> e : f.getDeterministicValues()) {
            if (!e.getKey().equals(Variable.AGE)
                    && e.getValue().compareTo(BigDecimal.ZERO) == 0) {
                hasEnabledImm = true;
                break;
            }
        }

        ssf.setVanishing(hasEnabledImm);
        ssf.setAbsorbing(enabledTransitions.isEmpty());
        state.addFeature(ssf);

        // adds the age variable and the transient stochastic feature
        if (transientAnalysis) {
            ssf.addAgeVariable(Variable.AGE);

            TransientStochasticStateFeature tssf = new TransientStochasticStateFeature();
            tssf.setReachingProbability(BigDecimal.ONE);

            StateDensityFunction enteringTime = new StateDensityFunction();
            enteringTime
                    .addDeterministicVariable(Variable.AGE, BigDecimal.ZERO);
            tssf.setEnteringTimeDensity(enteringTime);

            state.addFeature(tssf);
        }

        return state;
    }
}
