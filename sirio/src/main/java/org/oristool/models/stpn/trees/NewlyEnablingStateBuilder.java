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
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.StateDensityFunction;
import org.oristool.models.gspn.RateExpressionFeature;
import org.oristool.models.pn.InitialPetriStateBuilder;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * State builder for stochastic time Petri nets, using standard regenerations.
 */
public class NewlyEnablingStateBuilder implements StateBuilder<Marking> {

    private PetriNet petriNet;
    private boolean transientAnalysis;
    boolean checkNewlyEnabled;
    private BigDecimal epsilon;
    private int numSamples;

    public NewlyEnablingStateBuilder(PetriNet petriNet,
            boolean transientAnalysis) {
        this(petriNet, transientAnalysis, false, BigDecimal.ZERO, 0);
    }

    /**
     * Configures a state builder for STPNs.
     *
     * @param petriNet Petri net
     * @param transientAnalysis whether the state should include
     *        {@code Variable.AGE}
     * @param checkNewlyEnabled whether to compare enabled sets
     * @param epsilon allowed error when comparing states
     * @param numSamples number of samples used when comparing states
     */
    public NewlyEnablingStateBuilder(PetriNet petriNet,
            boolean transientAnalysis, boolean checkNewlyEnabled,
            BigDecimal epsilon, int numSamples) {

        this.petriNet = petriNet;
        this.transientAnalysis = transientAnalysis;
        this.checkNewlyEnabled = checkNewlyEnabled;
        this.epsilon = epsilon;
        this.numSamples = numSamples;
    }

    @Override
    public State build(Marking marking) {

        // adds the petri state feature
        State state = InitialPetriStateBuilder.computeInitialState(petriNet,
                marking, checkNewlyEnabled);
        Set<Transition> enabledTransitions = state.getFeature(
                PetriStateFeature.class).getNewlyEnabled();

        // builds the stochastic state feature
        StochasticStateFeature ssf = new StochasticStateFeature(epsilon,
                numSamples);
        StateDensityFunction f = new StateDensityFunction();
        ssf.setStateDensity(f);

        // builds the initial state density function through cartesian products
        // and adds deterministic functions manually
        for (Transition t : enabledTransitions) {
            ssf.addVariable(new Variable(t.getName()),
                    t.getFeature(StochasticTransitionFeature.class)
                            .getFiringTimeDensity());
        }

        // updates rates of all EXPs with a RateExpressionFeature
        for (Transition t : enabledTransitions) {
            if (t.hasFeature(RateExpressionFeature.class)) {
                ssf.setEXPRate(new Variable(t.getName()),
                        new BigDecimal(t
                                .getFeature(RateExpressionFeature.class)
                                .getRate(petriNet, marking)));
            }
        }

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
