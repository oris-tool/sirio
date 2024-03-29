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

package org.oristool.analyzer;

import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Common interface used by the analyzer to enumerate the events enabled in a
 * state for a model.
 *
 * @param <M> type of the model (such as a {@link PetriNet}
 * @param <E> type of the event (such as the firing of a {@link Transition}
 */
public interface EnabledEventsBuilder<M, E extends Event> {

    public Set<E> getEnabledEvents(M model, State state);

}
