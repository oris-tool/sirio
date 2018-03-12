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

package org.oristool.models.gspn.reachability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.models.gspn.chains.AbsorptionProbs;
import org.oristool.models.gspn.chains.DTMC;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

/**
 * Analysis of a {@link StateGraph} that computes the underlying CTMC by
 * eliminating vanishing states.
 *
 * <p>Vanishing states are detected by inspecting the {@link SPNState} feature
 * of states in the graph and checking whether
 * {@code exitRate() == Double.POSITIVE_INFINITY}.
 *
 * <p>Transition probabilities are read from the graph by extracting
 * {@link FiringProbability} features from its successions between states.
 *
 * <p>The underlying CTMC is returned as a {@link DTMC} where each state is
 * associated with a marking and an exit rate (encoded by the same
 * {@link SPNState} instance used in the input graph).
 */
class TangibleReduction {

    private final SuccessionGraph graph;
    private DTMC<SPNState> result;

    /**
     * Builds an engine for the tangible reduction of an input graph. States of the
     * graph must include an {@link SPNState} feature: a state with exit rate equal
     * to {@code Double.POSITIVE_INFINITY} is considered vanishing.
     *
     * @param graph the input graph with {@link SPNState} features
     */
    public TangibleReduction(SuccessionGraph graph) {
        this.graph = graph;
    }

    /**
     * Computes the DTMC embedded at tangible states. States include a marking (used
     * to compare states) and the exit rate.
     *
     * <p>If invoked more than once, the cached result is returned.
     *
     * @return the {@code DTMC} embedded at tangible states
     */
    public DTMC<SPNState> compute() {
        if (result != null)
            return result;

        result = DTMC.create();
        SPNState root = feature(graph.getState(graph.getRoot()));
        Set<Succession> visited = new HashSet<>(graph.getSuccessions().size());

        for (Succession succ : graph.getSuccessions()) {
            if (visited.contains(succ))
                continue;

            SPNState i = feature(succ.getParent());
            SPNState j = feature(succ.getChild());

            if (!isVanishing(i) && !isVanishing(j)) {
                addProb(i, j, prob(succ), result.probsGraph());
                visited.add(succ);

            } else if (!isVanishing(i) && isVanishing(j) || i == root) {
                Node startNode = graph.getNode(succ.getParent());
                List<Succession> tangibleToVanishing = new ArrayList<>();
                AbsorptionProbs<SPNState> absorption = AbsorptionProbs.compute(
                        buildVanishingDTMC(startNode, tangibleToVanishing, visited));
                validate(absorption.bscc());

                for (Succession succToVanishing : tangibleToVanishing) {
                    SPNState tangible  = feature(succToVanishing.getParent());
                    SPNState vanishing = feature(succToVanishing.getChild());
                    int vanishingIndex = absorption.transientIndex(vanishing);
                    double succProb = prob(succToVanishing);

                    for (int t = 0; t < absorption.bscc().size(); t++) {
                        double absProb = succProb * absorption.probs(vanishingIndex, t);
                        if (absProb > 0.0) {
                            SPNState nextTangible = absorption.bscc().get(t).iterator().next();
                            addProb(tangible, nextTangible, absProb, result.probsGraph());
                        }
                    }

                    visited.add(succToVanishing);
                }

                if (i == root && isVanishing(root)) {
                    int rootIndex = absorption.transientIndex(root);
                    for (int t = 0; t < absorption.bscc().size(); t++) {
                        double absProb = absorption.probs(rootIndex, t);
                        if (absProb > 0.0) {
                            SPNState nextTangible = absorption.bscc().get(t).iterator().next();
                            result.initialStates().add(nextTangible);
                            result.initialProbs().add(absProb);
                            result.probsGraph().addNode(nextTangible);  // for absorbing roots
                        }
                    }

                    visited.addAll(graph.getOutgoingSuccessions(startNode));
                }
            }
        }

        assert visited.size() == graph.getSuccessions().size();

        if (!isVanishing(root)) {
            assert result.initialProbs().isEmpty() && result.initialProbs().isEmpty();
            result.initialStates().add(root);
            result.initialProbs().add(1.0);
            result.probsGraph().addNode(root);  // for absorbing root
        }

        return result;
    }

    private static boolean isVanishing(SPNState stateFeature) {
        return stateFeature.exitRate() == Double.POSITIVE_INFINITY;
    }

    private boolean isVanishing(Node n) {
        return isVanishing(graph.getState(n).getFeature(SPNState.class));
    }

    private static SPNState feature(State state) {
        return state.getFeature(SPNState.class);
    }

    private SPNState feature(Node n) {
        return feature(graph.getState(n));
    }

    private static double prob(Succession succ) {
        return succ.getFeature(FiringProbability.class).value();
    }

    private static void addProb(SPNState i, SPNState j, double prob,
            MutableValueGraph<SPNState, Double> probsGraph) {
        double currentProb = probsGraph.edgeValueOrDefault(i, j, 0.0);
        probsGraph.putEdgeValue(i, j, currentProb + prob);
    }

    private static void validate(List<Set<SPNState>> bscc) {
        for (Set<SPNState> b : bscc) {
            if (b.size() != 1 || isVanishing(b.iterator().next()))
                throw new IllegalStateException("The input graph contains a timelock");
        }
    }

    /**
     * Computes the undirected reachability set of a given node, stopping only on
     * tangible ones. If the initial node is tangible, only its successors are
     * explored; if it is vanishing, also its predecessors are explored.
     *
     * <p>Successions from tangible states to vanishing ones (including a vanishing
     * root) are added to the input list {@code tangibleToVanishig}.
     *
     * <p>All successions explored in this vanishing subgraph are added to the input
     * set {@code visited}.
     *
     * @param i initial state (tangible or vanishing)
     * @param tangibleToVanishig list of successions from tangible states to
     *        vanishing ones (including vanishing root)
     * @param visited set of graph successions visited by the analysis
     * @returns graph of vanishing transitions
     */
    private MutableValueGraph<SPNState, Double> buildVanishingDTMC(final Node root,
            final List<Succession> tangibleToVanishing, final Set<Succession> visited) {

        MutableValueGraph<SPNState, Double> dtmc = ValueGraphBuilder
                .directed().allowsSelfLoops(true).build();

        Set<Node> opened = new HashSet<>();
        Deque<NeighborIterator> stack = new ArrayDeque<>();

        opened.add(root);
        stack.push(new NeighborIterator(root, graph, isVanishing(root)));

        while (!stack.isEmpty()) {
            if (stack.peek().hasNext()) {
                boolean outgoingEdge = stack.peek().isNextSucc();
                Node next = stack.peek().next();
                Node i = outgoingEdge ? stack.peek().node() : next;
                Node j = outgoingEdge ? next : stack.peek().node();

                if (outgoingEdge) {  // add edges only when visited outward
                    Set<Succession> succIJ = graph.getSuccessions(i, j);
                    if (!isVanishing(i) && isVanishing(j)) {
                        // record succession from tangible node to vanishing one
                        tangibleToVanishing.addAll(succIJ);
                    } else {
                        // add succession from vanishing to vanishing/tangible into the graph
                        for (Succession succ : succIJ) {
                            addProb(feature(i), feature(j), prob(succ), dtmc);
                        }
                    }
                    visited.addAll(succIJ);
                }

                if (!opened.contains(next)) {
                    opened.add(next);
                    if (isVanishing(next)) {
                        // visit all neighbors of vanishing nodes (except root)
                        stack.push(new NeighborIterator(next, graph, true));
                    } else if (outgoingEdge) {
                        // outward tangible nodes are absorbing: add self loop and stop there
                        dtmc.putEdgeValue(feature(next), feature(next), 1.0);
                    }   // inward tangible nodes are not added to the graph
                }

            } else {
                stack.pop();
            }
        }

        return dtmc;
    }

    /**
     * Neighbor iterator: iterates first over predecessors, then over successors.
     */
    private static class NeighborIterator implements Iterator<Node> {
        private final Node node;
        private final Iterator<Node> pre;
        private final Iterator<Node> succ;
        private boolean isNextSucc = false;

        NeighborIterator(Node n, SuccessionGraph graph, boolean visitRootPredecessors) {
            this.node = n;
            this.pre = visitRootPredecessors
                    ? graph.getPredecessors(n).iterator() : Collections.emptyIterator();
            this.succ = graph.getSuccessors(n).iterator();
        }

        public Node node() {
            return node;
        }

        public boolean isNextSucc() {
            if (!isNextSucc && !pre.hasNext())
                isNextSucc = true;
            return isNextSucc;
        }

        @Override public boolean hasNext() {
            return isNextSucc() ? succ.hasNext() : pre.hasNext();
        }

        @Override public Node next() {
            return isNextSucc() ? succ.next() : pre.next();
        }
    }
}
