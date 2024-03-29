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
import java.util.Iterator;
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
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * A simple GSPN including corner cases of IMM priorities and weights.
 */
class PriorityWeightsImmTest {

    PetriNet pn;
    Marking marking;
    List<Marking> expectedMarkings;
    List<Transition> expectedFirings;
    Set<Marking> tangible;

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
        pn.addPlace("zero");
        Place one = pn.addPlace("one");
        Place two = pn.addPlace("two");

        marking = new Marking();
        marking.setTokens(one, 1);
        marking.setTokens(two, 2);

        // higher priority wins over higher weight, but
        // if weight is zero the transition is not enabled
        addImm("imm1", "p0", "p1", "1.0",  5);
        addImm("imm2", "p0", "p2", "0.0",  9);
        addImm("imm3", "p0", "p3", "9.0",  1);

        // priority has no effect on distributed transitions
        addImm("imm4", "p1", "p4", "1.0", 1);
        addExp("exp1", "p1", "p5", "9.0", 9);

        // weight can disable distributed transitions
        addExp("exp2", "p4", "p6", "one", -1);
        addExp("exp3", "p4", "p7", "zero", -1);


        // should go from p0 =(imm1)> p1 =(imm4)> p4 =(exp2)> p6
        Marking m1 = new Marking(marking);
        m1.setTokens(pn.getPlace("p0"), 1);
        Marking m2 = new Marking(marking);
        m2.setTokens(pn.getPlace("p1"), 1);
        Marking m3 = new Marking(marking);
        m3.setTokens(pn.getPlace("p4"), 1);
        Marking m4 = new Marking(marking);
        m4.setTokens(pn.getPlace("p6"), 1);

        expectedMarkings = List.of(m1, m2, m3, m4);
        expectedFirings = List.of(
                pn.getTransition("imm1"),
                pn.getTransition("imm4"),
                pn.getTransition("exp2"));
        tangible = Set.of(expectedMarkings.get(2), expectedMarkings.get(3));

        marking.addTokens(pn.addPlace("p0"), 1);
    }

    @Test
    void testTimedExcludeZero() {

        TimedAnalysis analysis = TimedAnalysis.builder().excludeZeroProb(true).build();
        SuccessionGraph graph = analysis.compute(pn, marking);

        assertEquals(4, graph.getNodes().size());
        assertEquals(3, graph.getSuccessions().size());

        Node n0 = graph.getRoot();
        assertEquals(expectedMarkings.get(0),
                graph.getState(n0).getFeature(PetriStateFeature.class).getMarking());

        Succession t0 = graph.getOutgoingSuccessions(n0).iterator().next();
        assertEquals(expectedFirings.get(0), t0.getEvent());

        Node n1 = graph.getNode(t0.getChild());
        assertEquals(expectedMarkings.get(1),
                graph.getState(n1).getFeature(PetriStateFeature.class).getMarking());

        Succession t1 = graph.getOutgoingSuccessions(n1).iterator().next();
        assertEquals(expectedFirings.get(1), t1.getEvent());

        Node n2 = graph.getNode(t1.getChild());
        assertEquals(expectedMarkings.get(2),
                graph.getState(n2).getFeature(PetriStateFeature.class).getMarking());

        Succession t2 = graph.getOutgoingSuccessions(n2).iterator().next();
        assertEquals(expectedFirings.get(2), t2.getEvent());

        Node n3 = graph.getNode(t2.getChild());
        assertEquals(expectedMarkings.get(3),
                graph.getState(n3).getFeature(PetriStateFeature.class).getMarking());
    }

    @Test
    void testTimed() {
        // exp1 can also fire instead of imm4 (second firing)
        TimedAnalysis analysis = TimedAnalysis.builder().build();
        SuccessionGraph graph = analysis.compute(pn, marking);

        assertEquals(5, graph.getNodes().size());
        assertEquals(4, graph.getSuccessions().size());

        Node n0 = graph.getRoot();
        assertEquals(expectedMarkings.get(0),
                graph.getState(n0).getFeature(PetriStateFeature.class).getMarking());

        Succession t0 = graph.getOutgoingSuccessions(n0).iterator().next();
        assertEquals(expectedFirings.get(0), t0.getEvent());

        Node n1 = graph.getNode(t0.getChild());
        assertEquals(expectedMarkings.get(1),
                graph.getState(n1).getFeature(PetriStateFeature.class).getMarking());

        Iterator<Succession> it = graph.getOutgoingSuccessions(n1).iterator();
        Succession t1 = it.next();
        if (expectedFirings.get(1).equals(pn.getTransition("exp1")))
            t1 = it.next();
        else
            assertEquals(pn.getTransition("exp1"), it.next().getEvent());

        assertEquals(expectedFirings.get(1), t1.getEvent());

        Node n2 = graph.getNode(t1.getChild());
        assertEquals(expectedMarkings.get(2),
                graph.getState(n2).getFeature(PetriStateFeature.class).getMarking());

        Succession t2 = graph.getOutgoingSuccessions(n2).iterator().next();
        assertEquals(expectedFirings.get(2), t2.getEvent());

        Node n3 = graph.getNode(t2.getChild());
        assertEquals(expectedMarkings.get(3),
                graph.getState(n3).getFeature(PetriStateFeature.class).getMarking());
    }

    @Test
    void testGSPN() {

        GSPNReachability reachability = GSPNReachability.builder().build();
        DTMC<SPNState> graph = reachability.compute(pn, marking);
        assertEquals(2, graph.probsGraph().nodes().size());
        assertEquals(1, graph.probsGraph().edges().size());
        assertEquals(tangible, graph.probsGraph().nodes().stream()
                .map(n -> n.state()).collect(Collectors.toSet()));

        GSPNSteadyState steadyAnalysis = GSPNSteadyState.builder().build();
        Map<Marking, Double> steadyProbs = steadyAnalysis.compute(pn, marking);
        assertEquals(Set.of(expectedMarkings.get(3)), steadyProbs.keySet());
        assertEquals(1.0, steadyProbs.get(expectedMarkings.get(3)), 1e-9);

        GSPNTransient transientAnalysis = GSPNTransient.builder()
                .timePoints(0.0, 9.0, 1.0)
                .error(1e-9).build();

        Pair<Map<Marking, Integer>, double[][]> transientProbs =
                transientAnalysis.compute(pn, marking);
        assertEquals(tangible, transientProbs.first().keySet());

        int j2 = transientProbs.first().get(expectedMarkings.get(2));
        int j3 = transientProbs.first().get(expectedMarkings.get(3));
        for (int t = 0; t < transientProbs.second().length; t++) {
            assertEquals(Math.exp(-1.0 * t), transientProbs.second()[t][j2], 1e-9);
            assertEquals(1 - Math.exp(-1.0 * t), transientProbs.second()[t][j3], 1e-9);
        }
    }

    @Test
    void testSTPN() {

        /* TODO absorbing state space not supported yet
        RegSteadyState steadyAnalysis = RegSteadyState.builder().build();
        SteadyStateSolution<Marking> steadyProbs = steadyAnalysis.compute(pn, marking);
        assertEquals(new HashSet<>(expectedMarkings), steadyProbs.getSteadyState().keySet()); */

        TreeTransient treeAnalysis = TreeTransient.builder()
                .timeStep(new BigDecimal("0.1"))
                .greedyPolicy(BigDecimal.ONE, new BigDecimal("0.1")).build();
        TransientSolution<Marking, Marking> solution = treeAnalysis.compute(pn, marking);
        assertEquals(new HashSet<>(expectedMarkings), new HashSet<>(solution.getColumnStates()));

        RegTransient regAnalysis = RegTransient.builder()
                .timeStep(new BigDecimal("0.1"))
                .greedyPolicy(BigDecimal.ONE, new BigDecimal("0.1")).build();
        TransientSolution<DeterministicEnablingState, Marking> regSolution =
                regAnalysis.compute(pn, marking);
        assertEquals(new HashSet<>(expectedMarkings),
                new HashSet<>(regSolution.getColumnStates()));
    }
}
