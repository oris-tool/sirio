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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.petrinet.Marking;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.TransitionFeature;

/**
 * Transition feature removing all tokens from a set of places.
 */
public final class PlaceFlusher implements TransitionFeature {

    private final Set<Place> places = new LinkedHashSet<>();

    public PlaceFlusher(Place... ps) {
        places.addAll(Arrays.asList(ps));
    }

    public boolean addPlace(Place p) {
        return places.add(p);
    }

    public boolean hasPlace(Place p) {
        return places.contains(p);
    }

    public boolean removePlace(Place p) {
        return places.remove(p);
    }

    public void updateMarking(Marking m) {
        for (Place p : places)
            m.setTokens(p, 0);
    }

    @Override
    public String toString() {
        return places.toString();
    }
}
