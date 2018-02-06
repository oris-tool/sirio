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

package org.oristool.models.pn;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.oristool.analyzer.state.State;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * State builder for Petri nets.
 */
public final class InitialPetriStateBuilder {

    public static State computeInitialState(PetriNet petriNet,
            Marking initialMarking) {
        return computeInitialState(petriNet, initialMarking, false);
    }

    /**
     * Builds an initial {@link State} instance from a marking.
     *
     * <p>The state includes only a {@code PetriStateFeature}.
     *
     * <p>When {@code checkNewlyEnabled} is true, the {@code PetriStateFeature} will
     * distinguish states with the same marking but different sets of newly enabled
     * transitions.
     *
     * @param pn Petri net associated with the marking
     * @param initialMarking initial marking
     * @param checkNewlyEnabled whether to compare the set of newly enabled
     *        transitions of two states
     * @return a state instance with a {@code PetriStateFeature}
     */
    public static State computeInitialState(PetriNet pn,
            Marking initialMarking, boolean checkNewlyEnabled) {

        PetriStateFeature psf = new PetriStateFeature();

        psf.setMarking(initialMarking);
        psf.setEnabled(pn.getEnabledTransitions(initialMarking));
        psf.setNewlyEnabled(new LinkedHashSet<Transition>(psf.getEnabled()));
        psf.setPersistent(Collections.emptySet());
        psf.setDisabled(Collections.emptySet());

        if (checkNewlyEnabled)
            psf.setCheckNewlyEnabled(checkNewlyEnabled);

        State s = new State();
        s.addFeature(psf);

        return s;
    }

}
