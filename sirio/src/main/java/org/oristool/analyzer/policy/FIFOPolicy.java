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

package org.oristool.analyzer.policy;

import java.util.LinkedList;
import java.util.Queue;

import org.oristool.analyzer.Succession;

/**
 * FIFO enumeration policy.
 */
public final class FIFOPolicy implements EnumerationPolicy {
    private Queue<Succession> queue = new LinkedList<>();

    @Override
    public void add(Succession succession) {
        queue.add(succession);
    }

    @Override
    public Succession remove() {
        return queue.remove();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
