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

package org.oristool.models;

import org.oristool.petrinet.PetriNet;

/**
 * Common interface of analysis engines.
 */
public interface Engine {

    /**
     * Checks if the analysis can be applied to the given Petri net.
     *
     * <p>Problems are collected in a {@link validationMessageCollector}.
     *
     * @param pn Petri net
     * @param c collector for error messages
     * @return true if the analysis can be applied to the given Petri net
     */
    boolean canAnalyze(PetriNet pn, ValidationMessageCollector c);
}
