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

package org.oristool.models.pn;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.petrinet.Transition;
import org.oristool.petrinet.TransitionFeature;

/**
 * A transition feature forcing other transitions to resample a time-to-fire.
 */
public final class ResetSet implements TransitionFeature {

    final Set<Transition> resetSet;

    public ResetSet(Set<Transition> transitions) {
        resetSet = new LinkedHashSet<>(transitions);
    }

    public ResetSet(Transition... transitions) {
        resetSet = new LinkedHashSet<>(Arrays.asList(transitions));
    }

    public Set<Transition> getResetSet() {
        return resetSet;
    }
}
