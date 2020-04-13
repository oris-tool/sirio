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

package org.oristool.analyzer;

import org.oristool.analyzer.state.State;
import org.oristool.util.Featurizable;

/**
 * Succession between two states after the firing of an event.
 */
public final class Succession extends Featurizable<SuccessionFeature> {

    private State parent;
    private Event event;
    private State child;

    /**
     * Creates a succession from parent to child.
     *
     * @param parent parent state
     * @param event event triggering the succession
     * @param child child state
     */
    public Succession(State parent, Event event, State child) {
        this.parent = parent;
        this.event = event;
        this.child = child;
    }

    public State getChild() {
        return child;
    }

    public State getParent() {
        return parent;
    }

    public Event getEvent() {
        return event;
    }

    public void setParent(State parent) {
        this.parent = parent;
    }

    public void setChild(State child) {
        this.child = child;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("-- Parent --\n");
        b.append(parent);
        b.append("-- Event --\n");
        b.append(event + "\n");
        b.append("-- Child --\n");
        b.append(child);
        b.append(super.toString());
        return b.toString();
    }
}
