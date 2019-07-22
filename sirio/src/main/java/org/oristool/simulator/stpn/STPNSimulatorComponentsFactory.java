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

package org.oristool.simulator.stpn;

import java.util.Set;

import org.oristool.analyzer.EnabledEventsBuilder;
import org.oristool.analyzer.state.State;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.SimulatorComponentsFactory;
import org.oristool.simulator.SimulatorInitialStateBuilder;
import org.oristool.simulator.SimulatorSuccessorEvaluator;

/**
 * Components using during the simulation of STPNs.
 */
public final class STPNSimulatorComponentsFactory
        implements SimulatorComponentsFactory<PetriNet, Transition> {

    private final SimulatorInitialStateBuilder initialStateBuilder =
            new SimulatorSTPNInitialStateBuilder();

    private final SimulatorSuccessorEvaluator successorEvaluator =
            new SimulatorSTPNSuccessorEvaluator();

    @Override
    public SimulatorInitialStateBuilder getInitialStateBuilder() {
        return initialStateBuilder;
    }

    @Override
    public SimulatorSuccessorEvaluator getSuccessorEvaluator() {
        return successorEvaluator;
    }

    @Override
    public EnabledEventsBuilder<PetriNet, Transition> getFirableTransitionSetBuilder() {

        return new EnabledEventsBuilder<PetriNet, Transition>() {
            @Override
            public Set<Transition> getEnabledEvents(PetriNet petriNet, State state) {
                return state.getFeature(PetriStateFeature.class).getEnabled();
            }
        };
    }
}
