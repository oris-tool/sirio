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

package org.oristool.models.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.gspn.reachability.GSPNReachability;
import org.oristool.models.gspn.reachability.SPNState;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.tpn.TimedAnalysis;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.util.Pair;

/**
 * Test of all engines on a Petri net with no transitions.
 */
public class NoTransitionsTest {
    PetriNet pn;
    Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();
        Place p0 = pn.addPlace("p0");

        marking = new Marking();
        marking.setTokens(p0,  1);
    }

    @Test
    void testTimed() {
        TimedAnalysis analysis = TimedAnalysis.builder().build();
        SuccessionGraph graph = analysis.compute(pn, marking);

        assertEquals(1, graph.getNodes().size());
        assertEquals(0, graph.getSuccessions().size());

        Node n0 = graph.getRoot();
        assertEquals(marking,
                graph.getState(n0).getFeature(PetriStateFeature.class).getMarking());
    }

    @Test
    void testGSPN() {

        GSPNReachability reachability = GSPNReachability.builder().build();
        DTMC<SPNState> graph = reachability.compute(pn, marking);
        assertEquals(1, graph.probsGraph().nodes().size());
        assertEquals(0, graph.probsGraph().edges().size());
        assertEquals(marking, graph.probsGraph().nodes().iterator().next().state());

        GSPNSteadyState steadyAnalysis = GSPNSteadyState.builder().build();
        Map<Marking, Double> steadyProbs = steadyAnalysis.compute(pn, marking);
        assertEquals(Set.of(marking), steadyProbs.keySet());
        assertEquals(1.0, steadyProbs.get(marking), 1e-9);

        GSPNTransient transientAnalysis = GSPNTransient.builder()
                .timePoints(0.0, 9.0, 1.0).build();
        Pair<Map<Marking, Integer>, double[][]> transientProbs =
                transientAnalysis.compute(pn, marking);
        assertEquals(Set.of(marking), transientProbs.first().keySet());

        int j2 = transientProbs.first().get(marking);
        for (int t = 0; t < transientProbs.second().length; t++) {
            assertEquals(1.0, transientProbs.second()[t][j2], 1e-9);
        }
    }

    @Test
    void testSTPN() {

        /* TODO absorbing state space not supported yet
        RegSteadyState steadyAnalysis = RegSteadyState.builder().stopOn(stop).build();
        SteadyStateSolution<Marking> steadyProbs = steadyAnalysis.compute(pn, marking);
        assertEquals(new HashSet<>(expectedMarkings), steadyProbs.getSteadyState().keySet()); */

        TreeTransient transientAnalysis = TreeTransient.builder()
                .timeStep(new BigDecimal("0.1"))
                .greedyPolicy(BigDecimal.ONE, new BigDecimal("0.1")).build();
        TransientSolution<Marking, Marking> solution = transientAnalysis.compute(pn, marking);
        assertEquals(Set.of(marking), new HashSet<>(solution.getColumnStates()));

        RegTransient regAnalysis = RegTransient.builder()
                .timeStep(new BigDecimal("0.1"))
                .greedyPolicy(BigDecimal.ONE, new BigDecimal("0.1")).build();
        TransientSolution<DeterministicEnablingState, Marking> regSolution =
                regAnalysis.compute(pn, marking);
        assertEquals(Set.of(marking),
                new HashSet<>(regSolution.getColumnStates()));
    }
}
