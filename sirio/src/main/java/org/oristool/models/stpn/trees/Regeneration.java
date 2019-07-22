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

package org.oristool.models.stpn.trees;

import org.oristool.analyzer.state.StateFeature;

/**
 * A state feature including a regeneration.
 *
 * @param <V> type of the regeneration
 */
public class Regeneration<V> implements StateFeature {

    private V value;

    public Regeneration(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof Regeneration))
            return false;

        @SuppressWarnings("rawtypes")
        Regeneration other = (Regeneration) obj;

        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + this.value.hashCode();

        return result;
    }

    @Override
    public String toString() {

        return "Regeneration: " + value.toString() + "\n";
    }
}
