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

package org.oristool.analyzer.graph;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Node of a {@code SuccessionGraph}.
 */
public final class Node {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private int id = 0;

    public Node() {
        this.id = counter.addAndGet(1);
    }

    /**
     * Returns the unique ID of this node.
     * 
     * @return node id
     */
    public int getId() {
        return id;
    }
}
