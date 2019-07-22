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

package org.oristool.models.tpn;

import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Variable;
import org.oristool.models.pn.InitialPetriStateBuilder;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * State builder for time Petri nets.
 */
public final class InitialTimedStateBuilder {

    private final boolean transientAnalysis;
    private final boolean checkNewlyEnabled;

    /**
     * Creates a state builder for time Petri nets.
     *
     * <p>When {@code checkNewlyEnabled} is true, the {@code PetriStateFeature} will
     * distinguish states with the same marking but different sets of newly enabled
     * transitions.
     *
     * @param transientAnalysis whether the state should include
     *        {@code Variable.AGE}
     * @param checkNewlyEnabled whether to compare the sets of newly enabled
     *        transitions of states
     */
    public InitialTimedStateBuilder(boolean transientAnalysis,
            boolean checkNewlyEnabled) {
        this.transientAnalysis = transientAnalysis;
        this.checkNewlyEnabled = checkNewlyEnabled;
    }

    /**
     * Builds an initial {@link State} instance from a marking.
     *
     * <p>The state includes a {@code PetriStateFeature} and a
     * {@code TimedStateFeature}.
     *
     * @param pn Petri net associated with the marking
     * @param initialMarking initial marking
     * @return a state instance with {@code PetriStateFeature} and
     *         {@code TimedStateFeature}
     */
    public State computeInitialState(PetriNet pn, Marking initialMarking) {

        State state = InitialPetriStateBuilder.computeInitialState(pn,
                initialMarking, checkNewlyEnabled);

        Set<Transition> enabledTransitions = state.getFeature(
                PetriStateFeature.class).getNewlyEnabled();
        Set<Variable> enabledVariables = Transition
                .newVariableSetInstance(enabledTransitions);
        if (transientAnalysis)
            enabledVariables.add(Variable.AGE);

        DBMZone domain = new DBMZone(enabledVariables);
        for (Transition t : enabledTransitions) {
            TimedTransitionFeature ttf = t
                    .getFeature(TimedTransitionFeature.class);
            domain.setCoefficient(t.newVariableInstance(), Variable.TSTAR,
                    ttf.getLFT());
            domain.setCoefficient(Variable.TSTAR, t.newVariableInstance(), ttf
                    .getEFT().negate());
        }

        if (transientAnalysis) {
            domain.setCoefficient(Variable.AGE, Variable.TSTAR,
                    OmegaBigDecimal.ZERO);
            domain.setCoefficient(Variable.TSTAR, Variable.AGE,
                    OmegaBigDecimal.ZERO);
        }

        TimedStateFeature tsf = new TimedStateFeature();
        tsf.setDomain(domain);
        state.addFeature(tsf);

        return state;
    }

}
