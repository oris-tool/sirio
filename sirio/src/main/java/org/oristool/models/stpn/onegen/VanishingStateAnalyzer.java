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

package org.oristool.models.stpn.onegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.models.gspn.WeightExpressionFeature;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Transition;

// refactored from GSPNGraphAnalyzer.java
class VanishingStateAnalyzer {
    private SuccessionGraph graph;
    private Set<State> vanishingStates;
    private Map<State, Integer> tangibleStates;
    private Map<State, Double> stateProbDenominator;

    public VanishingStateAnalyzer(SuccessionGraph graph) {
        this.graph = graph;

        this.tangibleStates = new LinkedHashMap<>();
        this.vanishingStates = new HashSet<>();

        int tangibleNodesIndex = 0;
        for (State state : graph.getStates()) {

            // a state without outgoing successions is tangible
            boolean immediate = false;

            Set<Succession> newSet = graph.getOutgoingSuccessions(graph.getNode(state));
            if (!newSet.isEmpty()) {
                Succession s = newSet.iterator().next();

                // ReducedTransitions are exponential
                immediate = isImmediate(s);
            }

            if (immediate == false) {
                this.tangibleStates.put(state, tangibleNodesIndex);
                tangibleNodesIndex++;
            } else {
                this.vanishingStates.add(state);
                if (state.hasFeature(Regeneration.class)) {
                    throw new IllegalStateException(
                            "Vanishing state \n" + state + "\n is regenerative");
                }
            }
        }

        this.stateProbDenominator = new HashMap<>();
        for (State state : vanishingStates) {
            stateProbDenominator.put(state, Double.valueOf(nodeProbability(state)));
        }
    }

    private boolean isImmediate(Succession s) {
        boolean immediate = (s.getEvent() instanceof Transition)
                && ((Transition) s.getEvent()).hasFeature(WeightExpressionFeature.class)
                && ((Transition) s.getEvent()).hasFeature(StochasticTransitionFeature.class)
                && ((Transition) s.getEvent()).getFeature(StochasticTransitionFeature.class)
                        .isIMM();
        return immediate;
    }

    public Set<State> getTangibleStates() {
        return tangibleStates.keySet();
    }

    public Set<State> getVanishingStates() {
        return vanishingStates;
    }

    public boolean isVanishing(State s) {
        return vanishingStates.contains(s);
    }

    public boolean isTangible(State s) {
        return tangibleStates.containsKey(s);
    }

    public Map<State, Double> calculateAbsorptionVector(State vanishingStartingState) {

        Map<State, Integer> vanishingReachableStates = new HashMap<>();
        Map<State, Integer> tangibleReachableStates = new HashMap<>();
        initReachableStateSets(vanishingStartingState, tangibleReachableStates,
                vanishingReachableStates);

        checkForTimeLocks(tangibleReachableStates, vanishingReachableStates);

        Matrix matrixB = computeMatrixB(tangibleReachableStates, vanishingReachableStates);

        final int vanishingStartingStateIndex = 0;
        Map<State, Double> absorptionVector = new HashMap<>();
        tangibleReachableStates.forEach((state, stateIndex) -> absorptionVector.put(state,
                matrixB.get(vanishingStartingStateIndex, stateIndex)));

        return absorptionVector;
    }

    private double nodeProbability(State state) {
        double counter = 0.0;
        for (Succession s : graph.getOutgoingSuccessions(graph.getNode(state))) {
            counter += ((Transition) s.getEvent()).getFeature(StochasticTransitionFeature.class)
                    .getWeight().doubleValue();
        }
        return counter;
    }

    private void checkForTimeLocks(Map<State, Integer> tangibleReachableStates,
            Map<State, Integer> vanishingReachableStates) {
        if (tangibleReachableStates.isEmpty()) {
            StringBuilder stringb = new StringBuilder();
            for (State s : vanishingReachableStates.keySet()) {
                stringb.append(
                        s.getFeature(PetriStateFeature.class).getMarking().toString() + "\n");
            }

            throw new IllegalStateException(
                    "Time Lock has been found, constituting markings are: \n" + stringb.toString());
        }
    }

    private void initReachableStateSets(State vanishingStartingState,
            Map<State, Integer> tangibleReachableStates,
            Map<State, Integer> vanishingReachableStates) {
        int tangibleIndex = 0;
        int vanishingIndex = 0;

        vanishingReachableStates.put(vanishingStartingState, vanishingIndex);
        vanishingIndex++;

        Queue<Node> vanishingFrontier = Utils.newQueue(graph.getNode(vanishingStartingState));

        while (!vanishingFrontier.isEmpty()) {
            Node current = vanishingFrontier.remove();

            for (Succession s : graph.getOutgoingSuccessions(current)) {
                State next = s.getChild();
                if (!vanishingReachableStates.containsKey(next)
                        && !tangibleReachableStates.containsKey(next)) {
                    if (vanishingStates.contains(next)) {
                        vanishingFrontier.add(graph.getNode(next));
                        vanishingReachableStates.put(next, vanishingIndex);
                        vanishingIndex++;
                    } else {
                        tangibleReachableStates.put(next, tangibleIndex);
                        tangibleIndex++;
                    }
                }
            }
        }
        assert (tangibleReachableStates.size() == tangibleIndex);
        assert (vanishingReachableStates.size() == vanishingIndex);
    }

    private Matrix computeMatrixB(Map<State, Integer> tangibleReachableStates,
            Map<State, Integer> vanishingReachableStates) {
        double[][] matrixN = new double[vanishingReachableStates.size()][vanishingReachableStates
                .size()];
        double[][] matrixR = new double[vanishingReachableStates.size()][tangibleReachableStates
                .size()];

        for (State source : vanishingReachableStates.keySet()) {
            int stateIndex = vanishingReachableStates.get(source);
            matrixN[stateIndex][stateIndex] = 1.0;

            for (Succession s : graph.getOutgoingSuccessions(graph.getNode(source))) {
                double destProb = ((Transition) s.getEvent())
                        .getFeature(StochasticTransitionFeature.class).getWeight().doubleValue()
                        / stateProbDenominator.get(source);
                State dest = s.getChild();

                if (vanishingReachableStates.containsKey(dest)) {
                    matrixN[stateIndex][vanishingReachableStates.get(dest)] -= destProb;
                } else {
                    matrixR[stateIndex][tangibleReachableStates.get(dest)] += destProb;
                }
            }
        }

        // B = inv(N) * R;
        return new Basic2DMatrix(matrixN).withInverter(LinearAlgebra.GAUSS_JORDAN).inverse()
                .multiply(new Basic2DMatrix(matrixR));
    }
}