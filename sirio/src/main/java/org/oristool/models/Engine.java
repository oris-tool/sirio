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

package org.oristool.models;

import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

/**
 * Common interface of analysis engines.
 *
 * @param <M> model type (such as {@link PetriNet})
 * @param <S> initial state type (such as {@link Marking})
 * @param <R> result type (such as {@link SuccessionGraph})
 */
public interface Engine<M, S, R> {

    /**
     * Checks if the analysis can be applied to the given model.
     *
     * @param model input model (such as a Petri net)
     * @return true if the analysis can be applied to the given model
     */
    default boolean canAnalyze(M model) {
        return canAnalyze(model, new ValidationMessageCollector());
    }

    /**
     * Checks if the analysis can be applied to the given model.
     *
     * <p>Problems are collected in a {@link ValidationMessageCollector}.
     *
     * @param model input model (such as a Petri net)
     * @param collector collector of error messages
     * @return true if the analysis can be applied to the given model
     */
    boolean canAnalyze(M model, ValidationMessageCollector collector);

    /**
     * Runs the analysis engine on a given model and initial state.
     *
     * @param model input model (such as a Petri net)
     * @param initialState initial state for the analysis (such as a Marking)
     * @return the result of the analysis
     * @throws IllegalArgumentException if the analysis cannot be applied
     */
    R compute(M model, S initialState);
}
