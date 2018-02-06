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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

class GSPNGraphAnalyzer {

    private PetriNet petrinet;
    private SuccessionGraph graph;
    private Set<Node> tangibles;
    private Set<Node> vanishing;
    private boolean cyclesPresent;
    private Map<Node, Set<Succession>> outgoingSuccessions;
    private Map<Node, Double> nodeProbabDenominator;

    // used to optimize calculation of absorption for cycles
    // vanishing node - list of vanishing reachable from node
    private Map<Node, List<Node>> recordedCycles;
    // list of vanishing matrix (i,j) where j (tang) is probability of
    // absorption from i (van)
    private Map<List<Node>, double[][]> recordedMatrixes;

    // index contains the complete successions list (even self-loops)
    // if there's no cycles in the graph

    // if cycles are present for each node it simply contains the immediate
    // successions,
    // corresponding expansion of connections to vanishing states is reached
    // trough the cycleMap map
    private Map<Node, Map<Node, Set<List<Succession>>>> index;
    private boolean rootVanishing;
    private Map<Node, Map<Node, Double>> cycleMap;
    private List<State> tangiblesStateList;
    private List<Node> tangiblesList;

    public Map<Node, Map<Node, Double>> getCycleMap() {
        return Collections.unmodifiableMap(cycleMap);
    }

    public List<State> getTangiblesStateList() {
        return Collections.unmodifiableList(tangiblesStateList);
    }

    public List<Node> getTangiblesList() {
        return Collections.unmodifiableList(tangiblesList);
    }

    public PetriNet getPetriNet() {
        return petrinet;
    }

    public SuccessionGraph getGraph() {
        return graph;
    }

    public Map<Node, Map<Node, Set<List<Succession>>>> getIndex() {
        return Collections.unmodifiableMap(index);
    }

    public Set<Node> getTangibles() {
        return Collections.unmodifiableSet(tangibles);
    }

    public boolean isRootVanishing() {
        return rootVanishing;
    }

    public Set<Node> getVanishing() {
        return Collections.unmodifiableSet(vanishing);
    }

    public Map<Node, Double> getNodeProbabDenominator() {
        return Collections.unmodifiableMap(nodeProbabDenominator);
    }

    public boolean isCyclesPresent() {
        return cyclesPresent;
    }

    public GSPNGraphAnalyzer(SuccessionGraph graph, PetriNet petrinet) {
        this.petrinet = petrinet;
        this.index = new HashMap<Node, Map<Node, Set<List<Succession>>>>();
        this.nodeProbabDenominator = new HashMap<Node, Double>();
        this.tangiblesStateList = new LinkedList<State>();
        this.tangiblesList = new LinkedList<Node>();
        updateAnalyzer(graph);
        for (Node t : tangibles) {
            this.tangiblesStateList.add(graph.getState(t));
            this.tangiblesList.add(t);

        }
        for (Node v : vanishing) {
            nodeProbabDenominator.put(v, nodeProbability(v));
        }
        if (vanishing.contains(graph.getRoot())) {
            this.rootVanishing = true;
        } else
            this.rootVanishing = false;

        checkForVanishingCycles();

        this.recordedCycles = new HashMap<Node, List<Node>>();
        this.recordedMatrixes = new HashMap<List<Node>, double[][]>();

        graphOp(cyclesPresent);

    }

    // aggiorna le liste vanishing tangible secondo il graph attuale
    public void updateAnalyzer(SuccessionGraph graph) {
        this.graph = graph;
        Set<Node> currentTangible = new HashSet<Node>();
        Set<Node> currentVanishing = new HashSet<Node>();
        Map<Node, Set<Succession>> currentMap = new HashMap<Node, Set<Succession>>();

        // uno stato terminale senza possibili transizioni ? tangible di default
        for (Node n : graph.getNodes()) {
            boolean immediate = false;
            Set<Succession> newSet = graph.getOutgoingSuccessions(n);
            for (Succession s : newSet) {

                // ReducedTransitions are Rate (exponential) Transitions by
                // default
                if (!(s.getEvent() instanceof ReducedTransition)
                        && ((Transition) s.getEvent())
                                .hasFeature(WeightExpressionFeature.class)) {
                    immediate = true;
                    break;
                } else
                    break;
            }

            if (immediate == false)
                currentTangible.add(n);
            else
                currentVanishing.add(n);
            currentMap.put(n, newSet);
        }

        this.tangibles = currentTangible;
        this.vanishing = currentVanishing;
        this.outgoingSuccessions = currentMap;
    }

    private void checkForVanishingCycles() {
        this.cyclesPresent = false;
        List<Node> list = new LinkedList<Node>();
        list.addAll(vanishing);

        int[] colors = new int[list.size()];
        Node last;
        for (int i = 0; i < list.size(); i++) {
            last = list.get(i);
            if (colors[i] == 0) {
                if (visit(last, list, colors)) {
                    this.cyclesPresent = true;
                    return;
                }
            }
        }

    }

    private boolean visit(Node start, List<Node> list, int[] colors) {
        colors[list.indexOf(start)] = 1;
        for (Succession s : graph.getOutgoingSuccessions(start)) {
            Node next = graph.getNode(s.getChild());
            if (list.contains(next)) {
                if (colors[list.indexOf(next)] == 1) {
                    return true;
                } else {
                    if (colors[list.indexOf(next)] == 0) {
                        if (visit(next, list, colors))
                            return true;
                    }
                }
            }
        }
        colors[list.indexOf(start)] = 3;
        return false;
    }

    @SuppressWarnings("unused")
    private Set<Succession> getReachableSuccessions(Node from) {
        Node cursor;
        Set<Node> known = new HashSet<Node>();
        HashSet<Succession> successions = new HashSet<Succession>();
        Queue<Node> queue = new LinkedList<Node>();
        Set<Succession> currentSuccessions;
        queue.add(from);
        known.add(from);
        while (!queue.isEmpty()) {
            cursor = queue.remove();
            // modificato rispetto alla versione di StateClassGraphAnalyzer
            // per non dover richiamare getOutgoingSuccessions
            currentSuccessions = outgoingSuccessions.get(cursor);
            successions.addAll(currentSuccessions);
            for (Node n : graph.getSuccessors(cursor))
                if (!known.contains(n)) {
                    known.add(n);
                    queue.add(n);
                }
        }
        return successions;
    }

    private void graphOp(boolean hasCycles) {

        if (cyclesPresent == true) {
            cycleMap = new HashMap<>();
            for (Node v : vanishing) {
                Map<Node, Double> absorpMap = new HashMap<Node, Double>();
                double[] absorpArray = calculateAbsorptionVector(v);
                for (int i = 0; i < absorpArray.length; i++) {
                    absorpMap.put(this.tangiblesList.get(i), absorpArray[i]);
                }
                cycleMap.put(v, absorpMap);

            }
        }

        for (Node n : tangibles) {
            Map<Node, Set<List<Succession>>> followers = new HashMap<>();
            index.put(n, followers);

            if (!hasCycles) {
                recursiveGraphVisit(n, new LinkedList<Succession>());
            } else {
                for (Succession s : outgoingSuccessions.get(n)) {
                    Node next = graph.getNode(s.getChild());
                    if (!index.get(n).containsKey(next)) {
                        index.get(n).put(next, new HashSet<List<Succession>>());
                    }
                    List<Succession> succList = new LinkedList<Succession>();
                    succList.add(s);
                    index.get(n).get(next).add(succList);

                }

            }
        }
    }

    // start ? il primo valore vanishing incontrato se root ? van pu? anche
    // essere direttamente il root
    // package-private per semplicit?, nel caso root sia vanishing calcola
    // assorbimento
    // - aggiorna valori di recordedCycles per ogni vanishing che butta nel
    // cicle da fuori
    double[] calculateAbsorptionVector(Node start) {

        double[][] endMatrix;
        List<Node> visitabiliEVanish;

        if (recordedCycles != null && recordedCycles.containsKey(start)) {

            endMatrix = recordedMatrixes.get(recordedCycles.get(start));
            visitabiliEVanish = recordedCycles.get(start);

        } else {

            List<Node> visitabili = new LinkedList<Node>();
            Node cursor;
            Queue<Node> queue = new LinkedList<Node>();
            queue.add(start);
            visitabili.add(start);
            visitabiliEVanish = new LinkedList<Node>();
            visitabiliEVanish.add(start);
            while (!queue.isEmpty()) {
                cursor = queue.remove();

                for (Succession s : outgoingSuccessions.get(cursor)) {
                    Node next = graph.getNode(s.getChild());
                    if (!visitabili.contains(next)) {
                        visitabili.add(next);
                        if (vanishing.contains(next)) {
                            visitabiliEVanish.add(next);
                            recordedCycles.put(next, visitabiliEVanish);
                            queue.add(next);
                        }
                    }

                }

            }

            // check for Time Locks
            if (visitabiliEVanish.size() == visitabili.size()) {
                StringBuilder stringb = new StringBuilder();
                for (Node n : visitabiliEVanish) {
                    stringb.append(graph.getState(n)
                            .getFeature(PetriStateFeature.class).getMarking()
                            .toString()
                            + "\n");
                }

                throw new IllegalStateException(
                        "Time Lock has been found, constituting markings are: \n"
                                + stringb.toString());
            }

            List<Node> tanList = new LinkedList<Node>();
            for (Node v : visitabili) {
                if (!visitabiliEVanish.contains(v))
                    tanList.add(v);
            }

            double[][] matrixQ = new double[visitabiliEVanish.size()][visitabiliEVanish
                    .size()];
            double[][] matrixR = new double[visitabiliEVanish.size()][tanList
                    .size()];

            for (Node v : visitabiliEVanish) {

                for (Succession s : outgoingSuccessions.get(v)) {
                    double value = ((Transition) s.getEvent()).getFeature(
                            WeightExpressionFeature.class).getWeight(
                            petrinet,
                            s.getParent().getFeature(PetriStateFeature.class)
                                    .getMarking())
                            / nodeProbabDenominator.get(v);
                    if (visitabiliEVanish.contains(graph.getNode(s.getChild())))
                        matrixQ[visitabiliEVanish.indexOf(v)][visitabiliEVanish
                                .indexOf(graph.getNode(s.getChild()))] += value;
                    else
                        matrixR[visitabiliEVanish.indexOf(v)][tanList
                                .indexOf(graph.getNode(s.getChild()))] += value;
                }
            }

            double[][] n_inverted = new double[matrixQ.length][matrixQ.length];
            for (int row = 0; row < n_inverted.length; row++) {
                for (int column = 0; column < n_inverted.length; column++) {
                    n_inverted[row][column] = -matrixQ[row][column];
                    if (row == column)
                        n_inverted[row][column] += 1.0;
                }
            }

            Matrix a = new Basic2DMatrix(n_inverted);

            MatrixInverter inverter = a
                    .withInverter(LinearAlgebra.GAUSS_JORDAN);
            // The 'b' matrix will be dense
            Matrix N = inverter.inverse();

            Matrix B = N.multiply(new Basic2DMatrix(matrixR));

            endMatrix = new double[visitabiliEVanish.size()][tangiblesList
                    .size()];
            for (int b_row = 0; b_row < visitabiliEVanish.size(); b_row++) {
                for (int b_column = 0; b_column < tanList.size(); b_column++) {
                    endMatrix[b_row][tangiblesList.indexOf(tanList
                            .get(b_column))] = B.get(b_row, b_column);
                }
            }

            recordedCycles.put(start, visitabiliEVanish);
            recordedMatrixes.put(visitabiliEVanish, endMatrix);

        }
        double[] returnVector = new double[tangiblesList.size()];
        for (int i = 0; i < tangiblesList.size(); i++) {
            returnVector[i] = endMatrix[visitabiliEVanish.indexOf(start)][i];
        }

        return returnVector;

    }

    private void recursiveGraphVisit(Node from, LinkedList<Succession> list) {

        for (Succession s : outgoingSuccessions.get(from)) {

            Node next = graph.getNode(s.getChild());
            @SuppressWarnings("unchecked")
            LinkedList<Succession> nextList = (LinkedList<Succession>) list
                    .clone();
            nextList.add(s);

            if (tangibles.contains(next)) {

                if (!index.get(graph.getNode(nextList.getFirst().getParent()))
                        .containsKey(next)) {
                    index.get(graph.getNode(nextList.getFirst().getParent()))
                            .put(next, new HashSet<List<Succession>>());
                }
                index.get(graph.getNode(nextList.getFirst().getParent()))
                        .get(next).add(nextList);

            } else
                recursiveGraphVisit(next, nextList);

        }

    }

    private double nodeProbability(Node n) {
        double counter = 0.0;
        for (Succession s : outgoingSuccessions.get(n)) {
            counter += ((Transition) s.getEvent()).getFeature(
                    WeightExpressionFeature.class).getWeight(
                    petrinet,
                    graph.getState(n).getFeature(PetriStateFeature.class)
                            .getMarking());
        }

        return counter;
    }
}