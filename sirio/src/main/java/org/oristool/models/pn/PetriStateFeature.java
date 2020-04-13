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

package org.oristool.models.pn;

import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.analyzer.state.StateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.Transition;

/**
 * A feature collecting state information of a Petri net.
 *
 * <p>The feature includes a marking and the set of enabled, disabled,
 * persistent and newly-enabled transitions.
 */
public final class PetriStateFeature implements StateFeature {

    private Marking marking;
    private Set<Transition> disabled;
    private Set<Transition> persistent;
    private Set<Transition> newlyEnabled;
    private Set<Transition> enabled;
    private boolean checkNewlyEnabled;

    /**
     * Builds an empty state feature.
     */
    public PetriStateFeature() {

    }

    /**
     * Builds a deep copy of an input state feature.
     *
     * @param other another state feature
     */
    public PetriStateFeature(PetriStateFeature other) {

        this.marking = new Marking(other.marking);

        this.disabled = new LinkedHashSet<Transition>(other.disabled);
        this.persistent = new LinkedHashSet<Transition>(other.persistent);
        this.newlyEnabled = new LinkedHashSet<Transition>(other.newlyEnabled);
        this.enabled = new LinkedHashSet<Transition>(other.enabled);
    }

    public boolean checkNewlyEnabled() {
        return checkNewlyEnabled;
    }

    public void setCheckNewlyEnabled(
            boolean checkNewlyEnabled) {
        this.checkNewlyEnabled = checkNewlyEnabled;
    }

    public Marking getMarking() {
        return marking;
    }

    public void setMarking(Marking marking) {
        this.marking = marking;
    }

    public Set<Transition> getDisabled() {
        return disabled;
    }

    public void setDisabled(Set<Transition> disabled) {
        this.disabled = disabled;
    }

    public Set<Transition> getPersistent() {
        return persistent;
    }

    public void setPersistent(Set<Transition> persistent) {
        this.persistent = persistent;
    }

    public Set<Transition> getNewlyEnabled() {
        return newlyEnabled;
    }

    public void setNewlyEnabled(Set<Transition> newlyEnabled) {
        this.newlyEnabled = newlyEnabled;
    }

    public Set<Transition> getEnabled() {
        return enabled;
    }

    public void setEnabled(Set<Transition> enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof PetriStateFeature))
            return false;

        PetriStateFeature o = (PetriStateFeature) obj;

        if (!this.marking.equals(o.marking))
            return false;

        if (checkNewlyEnabled)
            return this.newlyEnabled.equals(o.newlyEnabled);
        else
            return true;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.marking.hashCode();

        if (checkNewlyEnabled)
            result = 31 * result + this.newlyEnabled.hashCode();

        return result;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();
        b.append("Marking: ");
        b.append(marking);
        b.append("\n");
        b.append("NewlyEnabled: ");
        b.append(newlyEnabled);
        b.append("\n");
        b.append("Persistent: ");
        b.append(persistent);
        b.append("\n");
        b.append("Disabled: ");
        b.append(disabled);
        b.append("\n");

        return b.toString();
    }
}
