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

package org.oristool.analyzer.policy;

import org.oristool.analyzer.Succession;

/**
 * Enumeration policy to select the next state to expand.
 *
 * <p>States are added to the policy as successions, together with their parent.
 */
public interface EnumerationPolicy {

    /**
     * Adds the succession child as a new state to be explored.
     *
     * @param succession succession to be added
     */
    void add(Succession succession);

    /**
     * Extracts a succession: its child node is the next state to be explored.
     *
     * @return succession with next state
     */
    Succession remove();

    /**
     * Checks whether the set of states to explore is empty.
     *
     * @return true if no more states should be explored.
     */
    boolean isEmpty();
}