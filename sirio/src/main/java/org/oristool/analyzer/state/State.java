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

package org.oristool.analyzer.state;

import org.oristool.util.Featurizable;

/**
 * A state collecting data as features.
 */
public class State extends Featurizable<StateFeature> {

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof State)) {
            return false;
        }

        State o = (State) obj;

        // Note that .values() view in .getFeatures() uses Object's equals!
        return this.features.equals(o.features);
    }

    @Override
    public int hashCode() {
        // Note that .getFeatures() unmodifiable view uses Object's hashCode!
        return this.features.hashCode();
    }
}
