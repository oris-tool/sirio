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

package org.oristool.models.gspn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.linear.LinearSystemSolver;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.dense.BasicVector;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.state.State;
import org.oristool.util.Pair;

class TransientAndSteadyMatrixes {

    // list contains only the nodes(tangible) of reducedGraph OR of the BSCC
    // graph must contain only ReducedTransitions
    // there aren't self-loops and there can be multiple successions between 2
    // different states
    public static double[][] createInfinitesimalGeneratorForSteady(List<State> list,
            SuccessionGraph graph, Set<State> set) {
        int size = list.size();
        double[][] matrix = new double[size][size];

        for (State n : list) {
            int row_index = list.indexOf(n);

            for (Succession s : graph.getOutgoingSuccessions(graph.getNode(n))) {
                if (set.contains((s.getChild()))) {
                    double value = ((ReducedTransition) s.getEvent()).getRate();

                    int column_index = list.indexOf(s.getChild());

                    matrix[row_index][column_index] += value;
                    matrix[row_index][row_index] -= value;
                }
            }

        }

        return matrix;
    }

    // list contains ALL tangible nodes
    public static double[][] createInfinitesimalGeneratorForTransient(List<State> list,
            Map<SuccessionGraph, Double> graphMap) {
        int size = list.size();
        double[][] matrix = new double[size][size];

        Set<State> discovered = new HashSet<State>();

        for (SuccessionGraph graph : graphMap.keySet()) {
            for (State n : graph.getStates()) {
                if (!discovered.contains(n)) {
                    discovered.add(n);
                    int row_index = list.indexOf(n);
                    for (Succession s : graph.getOutgoingSuccessions(graph.getNode(n))) {

                        double value = ((ReducedTransition) s.getEvent()).getRate();

                        int column_index = list.indexOf(s.getChild());
                        matrix[row_index][column_index] += value;
                        matrix[row_index][row_index] -= value;
                    }
                }
            }
        }

        return matrix;
    }

    // for Steady State input inf.Generator
    public static double[][] createMatrixProbabilityDistribution(double[][] matrix) {
        int size = matrix.length;
        double[][] newMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int r = 0; r < size; r++) {
                if (i == r)
                    newMatrix[i][r] = 0.0;
                else
                    newMatrix[i][r] = matrix[i][r] / (-matrix[i][i]);
            }
        }

        return newMatrix;
    }

    // matrixP contiene solo
    public static double[] steadyStateProbability(double[][] matrixP, double[][] matrixQ) {
        int size = matrixP.length;
        double[] returnVector = new double[size];

        double[][] matrixPtransposed = new double[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                matrixPtransposed[row][column] = matrixP[column][row];
            }
        }

        double[][] systemMatrix = new double[size][size];
        // Ax=b == P_Tras u_Tras dove P matrice di probabilita'
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                systemMatrix[row][column] = matrixPtransposed[row][column];
                if (row == column)
                    systemMatrix[row][column] -= 1.0;
            }
        }

        Matrix a = new Basic2DMatrix(systemMatrix);
        Matrix aWithNorm = a.copyOfRows(size + 1);
        aWithNorm.setRow(size, 1.0);

        double[] vector = new double[size + 1];
        vector[size] = 1.0;
        Vector b = new BasicVector(vector);

        LinearSystemSolver solver = aWithNorm.withSolver(LinearAlgebra.LEAST_SQUARES);

        Vector x = solver.solve(b);

        double denominator = 0.0;
        double[] solution = new double[size];
        for (int i = 0; i < size; i++) {
            double value = x.get(i);
            solution[i] = value;
            denominator += value / (-matrixQ[i][i]);
        }

        for (int i = 0; i < size; i++) {
            returnVector[i] = (solution[i] / (-matrixQ[i][i])) / denominator;
        }

        return returnVector;

    }

    // for transient input inf.Generator
    public static double[][] createMatrixUniformized(double[][] matrix, double lambda) {
        int size = matrix.length;
        double[][] uniformized = new double[size][size];

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                uniformized[row][column] = matrix[row][column] / lambda;
                if (row == column)
                    uniformized[row][column] += 1.0;

            }
        }

        return uniformized;
    }

    public static double findLambda(double[][] matrix) {
        double lambda = 0.0;
        for (int i = 0; i < matrix.length; i++) {
            if (lambda < -matrix[i][i])
                lambda = -matrix[i][i];
        }
        if (lambda == 0.0)
            throw new RuntimeException(
                    "Lambda equals 0.0 (all diagonal values in the infinitesimalGenerator are 0)");
        lambda *= 1.03;
        return lambda;
    }

    public static LinkedList<double[]> calculateEmbeddedProbabilities(double[][] matrixUnif,
            double[] rootList, int rightTruncPoint, AnalysisLogger log) {
        return calculateEmbeddedProbabilities(null, matrixUnif, rootList, rightTruncPoint, log);
    }

    // rootList contiene probabilita' un tangible sia la radice
    public static LinkedList<double[]> calculateEmbeddedProbabilities(
            LinkedList<double[]> oldValues, double[][] matrixUnif, double[] rootList,
            int rightTruncPoint, AnalysisLogger log) {
        LinkedList<double[]> orderedValues = oldValues;
        if (orderedValues == null)
            orderedValues = new LinkedList<>();

        int statesSize = matrixUnif.length;
        int oldRIndex = orderedValues.size() - 1;

        if (oldRIndex == -1) {
            // First passage
            oldRIndex = 0;
            orderedValues.add(rootList);// R=0
        }
        if (oldRIndex + 1 <= rightTruncPoint && log != null) {
            log.log("Evaluating new embedded uniformized DTMC transient solution for R in ["
                    + (oldRIndex + 1) + "," + rightTruncPoint + "]\n");
        }
        // [1,R]
        for (int i = oldRIndex + 1; i <= rightTruncPoint; i++) {
            double[] array = new double[statesSize];
            double[] oldArray = orderedValues.getLast();
            for (int j = 0; j < statesSize; j++) {
                double counter = 0.0;
                for (int k = 0; k < statesSize; k++) {
                    counter += oldArray[k] * matrixUnif[k][j];
                }
                array[j] = counter;
            }
            orderedValues.add(array);
        }

        return orderedValues;
    }

    public static Pair<Integer, Integer> calculateLeftAndRight(double lambda, double err,
            AnalysisLogger logger) {
        int L = 0;
        int R = 0;
        boolean F = true;
        double cm = (1.0 / (Math.sqrt(2 * Math.PI * Math.floor(lambda))))
                * Math.exp(Math.floor(lambda) - lambda - 1.0 / 12 * Math.floor(lambda));

        int lowK = 0;
        int k = 0;

        if (0.0 < lambda && lambda < 25.0) {
            L = 0;
            if (Math.exp(-lambda) < Double.MIN_VALUE) {
                F = false;
            }
        } else if (lambda >= 25.0) {
            double b = (1 + (1.0 / lambda)) * Math.exp(1.0 / (8 * lambda));

            double lowStore;
            lowK = 3;
            do {
                lowK++;
                lowStore = b * Math.exp(-((double) lowK * lowK) / 2)
                        / (lowK * Math.sqrt(2 * Math.PI));

            } while (lowStore >= err / 2);

            L = (int) Math.floor(Math.floor(lambda) - (lowK * Math.sqrt(lambda)) - 3.0 / 2);

        }

        if (lambda == 0.0) {
            R = 0;
            F = false;
        } else if (lambda < 0.0) {
            throw new RuntimeException("lambda is negative");
        } else {
            double l = lambda;

            if (lambda < 400.0) {
                l = 400;
            }
            double a = (1 + (1.0 / l)) * Math.exp(1.0 / 16) * Math.sqrt(2);

            double store;
            k = 3;
            do {
                k++;
                double d = 1.0 / (1 - Math.exp(-(2.0 / 9) * (k * Math.sqrt(2 * lambda) + 3.0 / 2)));
                store = a * d * Math.exp(-((double) k * k) / 2)
                        / (k * Math.sqrt(2 * Math.PI));

            } while (store >= err / 2);
            R = (int) (Math.ceil(Math.floor(lambda) + (k * Math.sqrt(2 * l)) + 3.0 / 2.0));

        }

        double controlFlag = Double.MAX_VALUE / (Math.pow(10, 10) * (R - L));
        double kFlagL = lowK + 3.0 / 2 * Math.sqrt(lambda);
        double kFlagH = k * Math.sqrt(lambda) + 3.0 / 2 * Math.sqrt(lambda);

        if (lambda >= 25) {
            if (kFlagL > 0 && kFlagL <= Math.sqrt(lambda) / 2) {
                if (cm * Math
                        .exp(-Math.pow(kFlagL, 2) / 2 - Math.pow(kFlagL, 3) / 3 * Math.sqrt(lambda))
                        * controlFlag < Double.MIN_VALUE)
                    F = false;
            } else if (kFlagL <= (Math.sqrt(Math.floor(lambda) + 1) / Math.floor(lambda))) {
                double flagBound1 = cm * Math.pow(1 - (kFlagL / Math.sqrt(Math.floor(lambda) + 1)),
                        kFlagL * Math.sqrt(Math.floor(lambda) + 1));
                double flagBound2 = Math.exp(-lambda);
                if (Math.max(flagBound1, flagBound2) * controlFlag < Double.MIN_VALUE)
                    F = false;
            }
        }

        if (lambda >= 400) {
            if (cm * Math.exp(-Math.pow(kFlagH + 1, 2) / 2) * controlFlag < Double.MIN_VALUE)
                F = false;
        }

        if (logger != null && F == false) {
            logger.log(
                    "WARNING UNDERFLOW: UNDERFLOW for allowed error\n");
        }

        if (logger != null && R > 600) {
            logger.log(
                    "WARNING: possible underflow due to right bound > 600");
        }

        return Pair.of(L, R);
    }


    public static double[] transientStateProbability(LinkedList<double[]> precalculatedValues,
            LinkedList<Double> factConstants, Pair<Integer, Integer> truncated) {

        int L = truncated.first();
        int R = truncated.second();
        int size = precalculatedValues.getFirst().length;
        double[] result = new double[size];

        // suppongo result inizializzato a zeri
        for (int cursor = L; cursor <= R; cursor++) {
            for (int i = 0; i < size; i++) {
                // precalcV start from 0 so it's good not to cursor-1
                result[i] += precalculatedValues.get(cursor)[i]
                        * factConstants.get(cursor).doubleValue();

            }

        }

        return result;
    }

    public static LinkedList<Double> calculateConstants(Pair<Integer, Integer> truncated,
            double time, double lambda) {

        LinkedList<Double> result = new LinkedList<Double>();
        int R = truncated.second();
        double constant1 = lambda * time;
        double constant2 = Math.exp(-1 * constant1);

        double value = constant2;
        if (constant1 == 0.0) {
            value = 1.0;
        }

        result.add(value);
        for (int i = 1; i <= R; i++) {
            value *= constant1 / i;
            result.add(value);

        }

        return result;
    }

    public static Map<SuccessionGraph, Map<Set<State>, Double>> obtainBSCC(
            Map<SuccessionGraph, Double> graphMap) {

        Map<SuccessionGraph, Map<Set<State>, Double>> bscc = new HashMap<>();

        for (SuccessionGraph graph : graphMap.keySet()) {

            Map<Set<State>, Set<Succession>> SCCmap = applyKosarajuAlgorithm(graph);

            // link fra ogni state e il set in cui ? contenuto
            Map<State, Set<State>> supportMap = new HashMap<State, Set<State>>();

            // nuovi state rappresentanti SCCs per il nuovo grafo e collegamenti
            // fra essi
            Map<Set<State>, State> newStatesForGraph = new HashMap<Set<State>, State>();
            Map<State, Set<State>> stateToSet = new HashMap<State, Set<State>>();
            Map<State, Set<Succession>> successionsSet = new HashMap<State, Set<Succession>>();

            for (Set<State> set : SCCmap.keySet()) {
                for (State node : set) {
                    supportMap.put(node, set);
                }

                class StateSimple extends State {
                    @Override
                    public boolean equals(Object obj) {

                        if (obj == this)
                            return true;
                        else
                            return false;
                    }

                }

                State state = new StateSimple();
                newStatesForGraph.put(set, state);
                stateToSet.put(state, set);
            }

            for (Set<State> set : SCCmap.keySet()) {
                State state = newStatesForGraph.get(set);
                Set<Succession> newSet = new HashSet<Succession>();
                for (Succession s : SCCmap.get(set)) {
                    State child = newStatesForGraph.get(supportMap.get(s.getChild()));
                    double rate = ((ReducedTransition) s.getEvent()).getRate();
                    ReducedTransition transition = new ReducedTransition(
                            state.toString() + child.toString(), rate);
                    Succession newSuccession = new Succession(state, transition, child);
                    newSet.add(newSuccession);
                }
                successionsSet.put(state, newSet);
            }

            // build graph
            SuccessionGraph SCCgraph = new SuccessionGraph();
            State cursor;
            Set<State> known = new HashSet<State>();
            Queue<State> queue = new LinkedList<State>();
            Set<State> rootSet = supportMap.get(graph.getState(graph.getRoot()));
            State root = newStatesForGraph.get(rootSet);
            SCCgraph.addSuccession(new Succession(null, null, root));
            queue.add(root);
            known.add(root);
            while (!queue.isEmpty()) {
                cursor = queue.remove();
                for (Succession s : successionsSet.get(cursor)) {
                    SCCgraph.addSuccession(s);
                    if (!known.contains(s.getChild())) {
                        known.add(s.getChild());
                        queue.add(s.getChild());
                    }
                }

            }

            Map<State, Double> stateDenominator = new HashMap<State, Double>();
            for (State state : SCCgraph.getStates()) {
                stateDenominator.put(state, stateProbability(state, SCCgraph));
            }

            Map<State, Double> probOfBCCS = new HashMap<State, Double>();
            calculateBCCSProbabilityRecursive(stateDenominator, SCCgraph, root,
                    new LinkedList<Succession>(), probOfBCCS);

            Map<Set<State>, Double> BSCCandProbab = new HashMap<Set<State>, Double>();
            for (State state : probOfBCCS.keySet()) {
                BSCCandProbab.put(stateToSet.get(state), probOfBCCS.get(state));
            }

            bscc.put(graph, BSCCandProbab);
        }

        return bscc;
    }

    // sum of all rates of outgoing successions
    private static double stateProbability(State n, SuccessionGraph graph) {
        double counter = 0.0;
        for (Succession s : graph.getOutgoingSuccessions(graph.getNode(n))) {
            counter += ((ReducedTransition) s.getEvent()).getRate();
        }
        return counter;
    }

    private static void calculateBCCSProbabilityRecursive(Map<State, Double> stateDenominator,
            SuccessionGraph graph, State startingState, LinkedList<Succession> successionList,
            Map<State, Double> probOfBCCS) {

        // there's no self-loops given applyKosarajuAlg formulation
        if (!graph.getOutgoingSuccessions(graph.getNode(startingState)).isEmpty()) {
            for (Succession s : graph.getOutgoingSuccessions(graph.getNode(startingState))) {

                @SuppressWarnings("unchecked")
                LinkedList<Succession> nextList = (LinkedList<Succession>) successionList.clone();
                nextList.add(s);
                calculateBCCSProbabilityRecursive(stateDenominator, graph, s.getChild(), nextList,
                        probOfBCCS);

            }
            // if there's no outgoing successions the state
            // represents a Bottom SCC of which we calculate rate
        } else {
            double value = 1.0;
            for (Succession s : successionList) {
                value *= ((ReducedTransition) s.getEvent()).getRate()
                        / stateDenominator.get(s.getParent());
            }
            if (probOfBCCS.keySet().contains(startingState)) {
                double oldV = probOfBCCS.get(startingState).doubleValue();
                probOfBCCS.put(startingState, value + oldV);
            } else
                probOfBCCS.put(startingState, value);
        }

    }

    private static Map<Set<State>, Set<Succession>> applyKosarajuAlgorithm(SuccessionGraph graph) {

        Map<Set<State>, Set<Succession>> stronglyConnectedComponentsRecord = new HashMap<>();
        LinkedList<State> stateList = new LinkedList<State>();
        stateList.addAll(graph.getStates());
        Set<State> discovered = new HashSet<State>();

        int size = stateList.size();
        Stack<State> stack = new Stack<State>();
        State randomNotInStack;

        while (stack.size() < size) {
            randomNotInStack = stateList.remove();
            depthFirstSearchForward(graph, stack, discovered, randomNotInStack, stateList);
        }

        Set<State> recordSet = new HashSet<State>();
        while (!stack.isEmpty()) {
            State popped = stack.pop();
            Set<State> connectedComponent = new HashSet<State>();
            depthFirstSearchBackward(stack, graph, connectedComponent, recordSet, popped);
            Set<Succession> outgoingSCCSuccessions = new HashSet<Succession>();
            for (State n : connectedComponent) {
                for (Succession s : graph.getOutgoingSuccessions(graph.getNode(n))) {
                    if (!connectedComponent.contains(s.getChild())) {
                        outgoingSCCSuccessions.add(s);
                    }
                }
            }

            stronglyConnectedComponentsRecord.put(connectedComponent, outgoingSCCSuccessions);

        }

        return stronglyConnectedComponentsRecord;
    }

    private static void depthFirstSearchForward(SuccessionGraph graph, Stack<State> stack,
            Set<State> discovered, State startingState, LinkedList<State> stateList) {

        discovered.add(startingState);
        for (Succession s : graph.getOutgoingSuccessions(graph.getNode(startingState))) {
            if (!discovered.contains(s.getChild())) {
                depthFirstSearchForward(graph, stack, discovered, s.getChild(), stateList);
            }
        }
        stack.push(startingState);
        stateList.remove(startingState);

    }

    private static void depthFirstSearchBackward(Stack<State> stack, SuccessionGraph graph,
            Set<State> connectedSet, Set<State> markedStates, State startingState) {

        connectedSet.add(startingState);
        stack.remove(startingState);
        markedStates.add(startingState);
        for (Succession s : graph.getIncomingSuccessions(graph.getNode(startingState))) {
            if (!markedStates.contains(s.getParent())) {
                depthFirstSearchBackward(stack, graph, connectedSet, markedStates, s.getParent());
            }
        }

    }

}
