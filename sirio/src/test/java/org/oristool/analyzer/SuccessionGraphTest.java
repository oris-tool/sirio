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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.models.stpn.trees.StochasticStateFeature;

/**
 * Simple tests of succession graph model.
 */
class SuccessionGraphTest {

    @Test
    public void edgeCreation() {
        SuccessionGraph g = new SuccessionGraph();

        // adds s1 as root
        State s1 = new State();
        s1.addFeature(new StochasticStateFeature(null, 0));
        g.addSuccession(new Succession(null, null, s1));

        // adds s1 -> s2
        State s2 = new State();
        s2.addFeature(new StochasticStateFeature(null, 0));
        Succession succession = new Succession(s1, () -> "Test", s2);
        g.addSuccession(succession);

        assertTrue(g.getSuccessors(g.getNode(s1)).contains(g.getNode(s2)));
        assertTrue(g.getPredecessors(g.getNode(s2)).contains(g.getNode(s1)));
    }
}
