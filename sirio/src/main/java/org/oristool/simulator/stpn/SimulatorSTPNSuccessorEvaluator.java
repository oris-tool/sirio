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

import java.math.BigDecimal;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.state.State;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.PetriSuccessionEvaluator;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.SimulatorSuccessorEvaluator;
import org.oristool.simulator.TimedSimulatorStateFeature;

/**
 * Succession state evaluator for the simulation of an STPN.
 */
public final class SimulatorSTPNSuccessorEvaluator implements SimulatorSuccessorEvaluator {

    @Override
    public Succession computeSuccessor(PetriNet petriNet, State state, Transition fired) {

        Succession succession = new PetriSuccessionEvaluator().computeSuccession(petriNet, state,
                fired);

        TimedSimulatorStateFeature oldTimedFeature = state
                .getFeature(TimedSimulatorStateFeature.class);
        TimedSimulatorStateFeature newTimedFeature = new TimedSimulatorStateFeature();

        Marking prevMarking = succession.getParent()
                .getFeature(PetriStateFeature.class).getMarking();
        BigDecimal firedRate = new BigDecimal(
                fired.getFeature(StochasticTransitionFeature.class)
                .clockRate().evaluate(prevMarking));
        BigDecimal elapsed = oldTimedFeature.getTimeToFire(fired).multiply(firedRate);

        for (Transition t : succession.getChild().getFeature(PetriStateFeature.class)
                .getPersistent()) {
            BigDecimal reduction = new BigDecimal(
                    t.getFeature(StochasticTransitionFeature.class)
                    .clockRate().evaluate(prevMarking)).multiply(elapsed);

            newTimedFeature.setTimeToFire(t,
                    oldTimedFeature.getTimeToFire(t).subtract(reduction));
        }

        for (Transition t : succession.getChild().getFeature(PetriStateFeature.class)
                .getNewlyEnabled())
            newTimedFeature.setTimeToFire(t, t.getFeature(SamplerFeature.class).getSample());

        succession.getChild().addFeature(newTimedFeature);
        return succession;
    }
}
