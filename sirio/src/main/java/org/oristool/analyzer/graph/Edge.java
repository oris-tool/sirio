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

package org.oristool.analyzer.graph;

/**
 * Edge of a {@code SuccessionGraph}.
 */
final class Edge {
    private final Node predecessor;
    private final Node successor;

    public Edge(Node predecessor, Node successor) {
        this.predecessor = predecessor;
        this.successor = successor;
    }

    public Node predecessor() {
        return predecessor;
    }

    public Node successor() {
        return successor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof Edge))
            return false;

        Edge o = (Edge) obj;

        return this.predecessor.equals(o.predecessor)
                && this.successor.equals(o.successor);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.predecessor.hashCode();
        result = 31 * result + this.successor.hashCode();
        return result;
    }
}
