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

package org.oristool.models.gspn.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.gspn.reachability.GSPNReachability;
import org.oristool.models.gspn.reachability.SPNState;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * Simple GSPN with two BSCCs, IMM cycles, EXP cycles, vanishing initial state.
 */
public class SimpleCycleTest {

    private PetriNet pn;
    private Marking marking;
    private Marking root1Marking;
    private Marking root2Marking;
    private Marking child1Marking;
    private double root1Prob = 0.3125;
    private double root2Prob = 0.6875;
    private double root1Rate = 1.0;
    private double root2Rate = 1.0;
    private double child1Rate = 2.0;
    private SPNState root1 = new SPNState(root1Marking, root1Rate);
    private SPNState root2 = new SPNState(root2Marking, root2Rate);
    private SPNState child1 = new SPNState(child1Marking, child1Rate);

    @BeforeEach
    void prepareExpectedStates() {
        root1Marking = new Marking();
        root1Marking.addTokens(pn.getPlace("six"), 6);
        root1Marking.addTokens(pn.getPlace("three"), 3);
        root1Marking.addTokens(pn.getPlace("root1"), 1);

        root2Marking = new Marking();
        root2Marking.addTokens(pn.getPlace("six"), 6);
        root2Marking.addTokens(pn.getPlace("three"), 3);
        root2Marking.addTokens(pn.getPlace("root2"), 1);

        child1Marking = new Marking();
        child1Marking.addTokens(pn.getPlace("six"), 6);
        child1Marking.addTokens(pn.getPlace("three"), 3);
        child1Marking.addTokens(pn.getPlace("child1"), 1);

        root1 = new SPNState(root1Marking, root1Rate);
        root2 = new SPNState(root2Marking, root2Rate);
        child1 = new SPNState(child1Marking, child1Rate);

    }

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();
        marking = new Marking();

        Place root = pn.addPlace("root");
        marking.addTokens(root, 1);

        Transition loop = pn.addTransition("loop");
        loop.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
        pn.addPrecondition(root, loop);
        pn.addPostcondition(loop, root);

        Place nowhere = pn.addPlace("nowhere");
        Transition notTaken = pn.addTransition("notTaken");
        notTaken.addFeature(StochasticTransitionFeature.newExponentialInstance("20"));
        pn.addPrecondition(root, notTaken);
        pn.addPostcondition(notTaken, nowhere);

        Place three = pn.addPlace("three");
        Place six = pn.addPlace("six");
        marking.addTokens(three, 3);
        marking.addTokens(six, 6);

        Transition rand1 = pn.addTransition("rand1");
        Transition rand2 = pn.addTransition("rand2");
        rand1.addFeature(StochasticTransitionFeature.newDeterministicInstance(
                BigDecimal.ZERO, MarkingExpr.from("three - .5", pn)));
        rand2.addFeature(StochasticTransitionFeature.newDeterministicInstance(
                BigDecimal.ZERO, MarkingExpr.from("six - .5", pn)));

        Place root1 = pn.addPlace("root1");
        Place root2 = pn.addPlace("root2");
        pn.addPrecondition(root, rand1);
        pn.addPostcondition(rand1, root1);
        pn.addPrecondition(root, rand2);
        pn.addPostcondition(rand2, root2);

        Place child1 = pn.addPlace("child1");
        Transition rc1 = pn.addTransition("rc1");
        rc1.addFeature(StochasticTransitionFeature.newExponentialInstance(
                new BigDecimal("0.5"),
                MarkingExpr.from("three - 1", pn)));
        pn.addPrecondition(root1, rc1);
        pn.addPostcondition(rc1, child1);
        Transition cr1 = pn.addTransition("cr1");
        cr1.addFeature(StochasticTransitionFeature.newExponentialInstance("2"));
        pn.addPrecondition(child1, cr1);
        pn.addPostcondition(cr1, root1);

        Place child2 = pn.addPlace("child2");
        Transition rc2 = pn.addTransition("rc2");
        rc2.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
        pn.addPrecondition(root2, rc2);
        pn.addPostcondition(rc2, child2);
        Transition cr2 = pn.addTransition("cr2");  // exp would timelock
        cr2.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
        pn.addPrecondition(child2, cr2);
        pn.addPostcondition(cr2, root2);

        Place vanish = pn.addPlace("vanish");
        Transition cv2 = pn.addTransition("cv2");
        cv2.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
        pn.addPrecondition(child2, cv2);
        pn.addPostcondition(cv2, vanish);
        Transition vc2 = pn.addTransition("vc2");
        vc2.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
        pn.addPrecondition(vanish, vc2);
        pn.addPostcondition(vc2, child2);
    }

    @Test
    public void reachability() {

        final DTMC<SPNState> result = GSPNReachability.builder().build().compute(pn, marking);

        assertEquals(2, result.initialStates().size());
        assertTrue(result.initialStates().contains(root1));
        assertTrue(result.initialStates().contains(root2));
        int root1Index = result.initialStates().indexOf(root1);
        int root2Index = result.initialStates().indexOf(root2);

        assertEquals(root1Rate, result.initialStates().get(root1Index).exitRate(), 1e-15);
        assertEquals(root2Rate, result.initialStates().get(root2Index).exitRate(), 1e-15);

        assertEquals(root1Prob, result.initialProbs().get(root1Index), 1e-15);
        assertEquals(root2Prob, result.initialProbs().get(root2Index), 1e-15);

        Set<SPNState> nodes = Set.of(root1, root2, child1);
        assertEquals(nodes, result.probsGraph().nodes());

        assertEquals(3, result.probsGraph().edges().size());
        assertTrue(result.probsGraph().hasEdgeConnecting(root1, child1));
        assertTrue(result.probsGraph().hasEdgeConnecting(child1, root1));
        assertTrue(result.probsGraph().hasEdgeConnecting(root2, root2));
    }

    @Test
    public void reachabilityAbsorbing() {

        MarkingCondition mc = MarkingCondition.fromString("root1 > 0");

        final DTMC<SPNState> result = GSPNReachability.builder()
                .stopOn(mc)
                .build().compute(pn, marking);

        assertEquals(2, result.initialStates().size());
        assertTrue(result.initialStates().contains(root1));
        assertTrue(result.initialStates().contains(root2));
        int root1Index = result.initialStates().indexOf(root1);
        int root2Index = result.initialStates().indexOf(root2);

        assertEquals(0.0, result.initialStates().get(root1Index).exitRate(), 1e-15);
        assertEquals(root2Rate, result.initialStates().get(root2Index).exitRate(), 1e-15);

        assertEquals(root1Prob, result.initialProbs().get(root1Index), 1e-15);
        assertEquals(root2Prob, result.initialProbs().get(root2Index), 1e-15);

        Set<SPNState> nodes = Set.of(root1, root2);
        assertEquals(nodes, result.probsGraph().nodes());

        assertEquals(1, result.probsGraph().edges().size());
        assertTrue(result.probsGraph().hasEdgeConnecting(root2, root2));
    }

    @Test
    public void stationary() {
        Map<Marking, Double> dist = GSPNSteadyState.builder().build().compute(pn, marking);

        Map<Marking, Double> expected = Map.of(
                root1Marking,
                root1Prob * (0.5 / root1Rate) / (0.5 / root1Rate + 0.5 / child1Rate),
                root2Marking,
                root2Prob,
                child1Marking,
                root1Prob * (0.5 / child1Rate) / (0.5 / root1Rate + 0.5 / child1Rate));

        assertEquals(expected.size(), dist.size());
        for (Marking m : dist.keySet())
            assertEquals(expected.get(m), dist.get(m), 1e-9);
    }

    @Test
    public void transientProbs() {

        double step = 0.1;
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                .timePoints(0.0, 10.0, step)
                .error(1e-9)
                .build().compute(pn, marking);

        Map<Marking, Integer> statePos = result.first();
        double[][] probs = result.second();

        // TransientSolution<Marking, Marking> solution =
        //        TransientSolution.fromArray(result.second(), step, result.first(), marking);
        // new TransientSolutionViewer(solution);

        assertEquals(Set.of(root1Marking, child1Marking, root2Marking), statePos.keySet());

        for (int t = 0; t < probs.length; t++) {
            double time = t * step;
            assertEquals(0.6875, probs[t][statePos.get(root2Marking)], 1e-9);
            double probRoot1 = kolmogorovProb1(time, 1.0, 0.0, 1.0, 2.0);
            assertEquals(0.3125 * probRoot1, probs[t][statePos.get(root1Marking)], 1e-9);
            assertEquals(0.3125 * (1 - probRoot1), probs[t][statePos.get(child1Marking)], 1e-9);
        }
    }

    private static double kolmogorovProb1(double time, double p1, double p2,
            double lambda12, double lambda21) {
        double lambdaSum = lambda12 + lambda21;
        double transientProb11 = (lambda21 + lambda12 * Math.exp(-lambdaSum * time)) / lambdaSum;
        double transientProb22 = (lambda12 + lambda21 * Math.exp(-lambdaSum * time)) / lambdaSum;
        return p1 * transientProb11 + p2 * (1 - transientProb22);
    }
}
