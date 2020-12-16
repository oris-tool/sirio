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

package org.oristool.petrinet;

import org.oristool.util.Featurizable;

/**
 * Place of a Petri net.
 */
public final class Place extends Featurizable<PlaceFeature> {

    private final String name;

    /**
     * Builds a place from its name.
     *
     * @param placeName name of the place
     */
    Place(String placeName) {
        this.name = placeName;
    }

    /**
     * Returns the name of this place.
     *
     * @return name of the place
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
