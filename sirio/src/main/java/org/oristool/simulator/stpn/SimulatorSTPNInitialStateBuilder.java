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

package org.oristool.simulator.stpn;

import org.oristool.analyzer.state.State;
import org.oristool.models.pn.InitialPetriStateBuilder;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.SimulatorInitialStateBuilder;
import org.oristool.simulator.TimedSimulatorStateFeature;

/**
 * Builder for the initial state for the simulation of an STPN.
 */
public final class SimulatorSTPNInitialStateBuilder implements SimulatorInitialStateBuilder {

    @Override
    public State build(PetriNet petriNet, Marking initialMarking) {

        State state = InitialPetriStateBuilder.computeInitialState(petriNet, initialMarking, false);

        TimedSimulatorStateFeature timedFeature = new TimedSimulatorStateFeature();
        for (Transition t : state.getFeature(PetriStateFeature.class).getNewlyEnabled())
            timedFeature.setTimeToFire(t, t.getFeature(SamplerFeature.class).getSample());

        state.addFeature(timedFeature);

        return state;

    }
}
