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

package org.oristool.models.pn;

import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Precondition;
import org.oristool.petrinet.Transition;

/**
 * A simple strategy remove tokens from each input place.
 */
public final class PetriTokensRemover implements MarkingUpdater {

    /**
     * Removes tokens from each input place according to the multiplicity of the
     * precondition.
     */
    @Override
    public void update(Marking m, PetriNet petriNet, Transition t) {
        for (Precondition pc : petriNet.getPreconditions(t))
            m.removeTokens(pc.getPlace(), pc.getMultiplicity());

        if (t.hasFeature(PlaceFlusher.class))
            t.getFeature(PlaceFlusher.class).updateMarking(m);
    }
}
