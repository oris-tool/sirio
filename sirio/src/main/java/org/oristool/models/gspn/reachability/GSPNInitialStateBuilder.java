/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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

package org.oristool.models.gspn.reachability;

import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

/**
 * Initial state builder for GSPNs.
 *
 * <p>The initial state has the given marking and exit rate equal to
 * {@code Double.POSITIVE_INFINITY} (if IMM transitions are enabled) or to the
 * sum of exponential rates of enabled transitions (scaled by the execution rate
 * in the current marking).
 */
class GSPNInitialStateBuilder implements StateBuilder<Marking> {

    private final PetriNet petriNet;

    GSPNInitialStateBuilder(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    @Override
    public State build(Marking marking) {
        State state = new State();
        double exitRate = GSPNSuccessionEvaluator.exitRate(marking, petriNet);
        state.addFeature(new SPNState(marking, exitRate));
        return state;
    }
}
