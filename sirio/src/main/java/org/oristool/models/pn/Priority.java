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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.oristool.petrinet.Transition;
import org.oristool.petrinet.TransitionFeature;

/**
 * Transition feature encoding the priority used to resolve races between
 * immediate transitions.
 */
public final class Priority implements TransitionFeature {

    private final int priority;

    public Priority(int priority) {
        this.priority = priority;
    }

    public int value() {
        return priority;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof Priority))
            return false;

        Priority other = (Priority) obj;

        return this.priority == other.priority;
    }

    @Override
    public int hashCode() {
        return priority;
    }

    @Override
    public String toString() {
        return "Priority: " + priority;
    }

    /**
     * Finds the subset of transitions with maximum priority.
     *
     * @param transitions a group of transitions
     * @return {@code true} if the transition is enabled by the marking
     */
    public static Set<Transition> maxPriority(Collection<Transition> transitions) {

        Set<Transition> maxPriority = new HashSet<>();

        int requiredPriority = Integer.MIN_VALUE;
        for (Transition t : transitions) {
            Priority p = t.getFeature(Priority.class);
            int priority = p == null ? Integer.MIN_VALUE : p.value();

            if (priority > requiredPriority) {
                maxPriority.clear();
                requiredPriority = priority;
            }

            if (priority == requiredPriority) {
                maxPriority.add(t);
            }
        }

        return maxPriority;
    }
}
