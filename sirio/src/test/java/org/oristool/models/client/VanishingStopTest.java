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

package org.oristool.models.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.gspn.reachability.GSPNReachability;
import org.oristool.models.gspn.reachability.SPNState;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.tpn.TimedAnalysis;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * Test of all engines with an absorbing condition stopping on a vanishing state.
 */
public class VanishingStopTest {
    PetriNet pn;
    Marking marking;
    List<Marking> expectedMarkings;
    List<Transition> expectedFirings;
    Set<Marking> tangible;
    MarkingCondition stop;

    private void addImm(String name, String from, String to, String weight, int prio) {
        Transition t = pn.addTransition(name);
        pn.addPrecondition(pn.addPlace(from), t);
        pn.addPostcondition(t, pn.addPlace(to));
        t.addFeature(StochasticTransitionFeature.newDeterministicInstance(
                new BigDecimal("0.0"), MarkingExpr.from(weight, pn)));
        t.addFeature(new Priority(prio));
    }

    private void addExp(String name, String from, String to, String weight, int prio) {
        Transition t = pn.addTransition(name);
        pn.addPrecondition(pn.addPlace(from), t);
        pn.addPostcondition(t, pn.addPlace(to));
        t.addFeature(StochasticTransitionFeature.newExponentialInstance(
                BigDecimal.ONE, MarkingExpr.ONE, MarkingExpr.from(weight, pn)));
        t.addFeature(new Priority(prio));
    }

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        // stopping state is vanishing
        addImm("imm1", "p0", "p1", "1.0",  1);
        addImm("imm2", "p1", "p2", "1.0",  1);
        addExp("exp1", "p2", "p3", "1.0",  1);

        // should go from p0 =(imm1)> p1 (absorbing)
        marking = new Marking();
        Marking m0 = new Marking(marking);
        m0.setTokens(pn.getPlace("p0"), 1);
        Marking m1 = new Marking(marking);
        m1.setTokens(pn.getPlace("p1"), 1);

        expectedMarkings = List.of(m0, m1);
        expectedFirings = List.of(pn.getTransition("imm1"));
        tangible = Set.of(m1);

        marking.addTokens(pn.addPlace("p0"), 1);
        stop = MarkingCondition.fromString("p1 > 0");
    }

    @Test
    void testTimedExcludeZero() {

        TimedAnalysis analysis = TimedAnalysis.builder()
                .stopOn(stop)
                .excludeZeroProb(true).build();
        SuccessionGraph graph = analysis.compute(pn, marking);

        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getSuccessions().size());

        Node n0 = graph.getRoot();
        assertEquals(expectedMarkings.get(0),
                graph.getState(n0).getFeature(PetriStateFeature.class).getMarking());

        Succession t0 = graph.getOutgoingSuccessions(n0).iterator().next();
        assertEquals(expectedFirings.get(0), t0.getEvent());

        Node n1 = graph.getNode(t0.getChild());
        assertEquals(expectedMarkings.get(1),
                graph.getState(n1).getFeature(PetriStateFeature.class).getMarking());
    }

    @Test
    void testTimed() {
        TimedAnalysis analysis = TimedAnalysis.builder()
                .stopOn(stop)
                .excludeZeroProb(false).build();
        SuccessionGraph graph = analysis.compute(pn, marking);

        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getSuccessions().size());

        Node n0 = graph.getRoot();
        assertEquals(expectedMarkings.get(0),
                graph.getState(n0).getFeature(PetriStateFeature.class).getMarking());

        Succession t0 = graph.getOutgoingSuccessions(n0).iterator().next();
        assertEquals(expectedFirings.get(0), t0.getEvent());

        Node n1 = graph.getNode(t0.getChild());
        assertEquals(expectedMarkings.get(1),
                graph.getState(n1).getFeature(PetriStateFeature.class).getMarking());
    }

    @Test
    void testGSPN() {

        GSPNReachability reachability = GSPNReachability.builder().stopOn(stop).build();
        DTMC<SPNState> graph = reachability.compute(pn, marking);
        assertEquals(1, graph.probsGraph().nodes().size());
        assertEquals(0, graph.probsGraph().edges().size());
        assertEquals(tangible, graph.probsGraph().nodes().stream()
                .map(n -> n.state()).collect(Collectors.toSet()));

        GSPNSteadyState steadyAnalysis = GSPNSteadyState.builder().stopOn(stop).build();
        Map<Marking, Double> steadyProbs = steadyAnalysis.compute(pn, marking);
        assertEquals(Set.of(expectedMarkings.get(1)), steadyProbs.keySet());
        assertEquals(1.0, steadyProbs.get(expectedMarkings.get(1)), 1e-9);

        GSPNTransient transientAnalysis = GSPNTransient.builder()
                .stopOn(stop).timePoints(0.0, 9.0, 1.0).build();
        Pair<Map<Marking, Integer>, double[][]> transientProbs =
                transientAnalysis.compute(pn, marking);
        assertEquals(tangible, transientProbs.first().keySet());

        int j2 = transientProbs.first().get(expectedMarkings.get(1));
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
                .stopOn(stop)
                .timeStep(new BigDecimal("0.1"))
                .greedyPolicy(BigDecimal.ONE, new BigDecimal("0.1")).build();
        TransientSolution<Marking, Marking> solution = transientAnalysis.compute(pn, marking);
        assertEquals(new HashSet<>(expectedMarkings), new HashSet<>(solution.getColumnStates()));

        RegTransient regAnalysis = RegTransient.builder()
                .stopOn(stop)
                .timeStep(new BigDecimal("0.1"))
                .greedyPolicy(BigDecimal.ONE, new BigDecimal("0.1")).build();
        TransientSolution<DeterministicEnablingState, Marking> regSolution =
                regAnalysis.compute(pn, marking);
        assertEquals(new HashSet<>(expectedMarkings),
                new HashSet<>(regSolution.getColumnStates()));
    }
}
