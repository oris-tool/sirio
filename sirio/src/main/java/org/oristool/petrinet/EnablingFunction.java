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

package org.oristool.petrinet;

/**
 * The enabling function of a transition.
 *
 * <p>The transition will be enabled only if:
 * <ul>
 * <li>input places contain at least the number of tokens required by their
 * precondition arcs;
 * <li>inhibitor places contain fewer tokens than those required to activate
 * their inhibitor arcs;
 * <li>the enabling function evaluates to true.
 * </ul>
 *
 * <p>This class implements the interface {@link TransitionFeature} to be added
 * dynamically as feature to any {@link Transition}.
 */
public final class EnablingFunction implements TransitionFeature {

    private final MarkingCondition markingCondition;

    public EnablingFunction(MarkingCondition markingCondition) {

        this.markingCondition = markingCondition;
    }

    public EnablingFunction(String condition) {

        this.markingCondition = MarkingCondition.fromString(condition);
    }

    /**
     * Creates a copy the input instance.
     *
     * @param other input instance
     */
    public EnablingFunction(EnablingFunction other) {

        this.markingCondition = other.markingCondition;
    }

    /**
     * Returns the marking condition.
     *
     * @return the marking condition
     */
    public MarkingCondition getMarkingCondition() {
        return markingCondition;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof EnablingFunction))
            return false;

        EnablingFunction o = (EnablingFunction) obj;
        return markingCondition.equals(o.markingCondition);
    }

    @Override
    public int hashCode() {
        return markingCondition.hashCode();
    }

    @Override
    public String toString() {
        return markingCondition.toString();
    }
}
