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

package org.oristool.petrinet;

import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.analyzer.Event;
import org.oristool.math.expression.Variable;
import org.oristool.util.Featurizable;

/**
 * Transition of a Petri net.
 */
public final class Transition extends Featurizable<TransitionFeature> implements Event {

    private final String name;

    /**
     * Builds a transition from its name.
     *
     * @param transitionName name of the transition
     */
    Transition(String transitionName) {
        this.name = transitionName;
    }

    /**
     * Returns the name of this transition as the event name.
     *
     * @return name of the place
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Returns the variable associated with this transition.
     *
     * @return variable for this transition
     */
    public Variable newVariableInstance() {
        return new Variable(name);
    }

    /**
     * Variables associated with a set of transitions.
     *
     * @param transitions input transitions
     * @return associated variables
     */
    public static Set<Variable> newVariableSetInstance(
            Set<Transition> transitions) {

        Set<Variable> variables = new LinkedHashSet<Variable>(
                transitions.size());
        for (Transition t : transitions)
            variables.add(new Variable(t.name));

        return variables;
    }
}
