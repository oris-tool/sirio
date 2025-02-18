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

package org.oristool.models.gspn.reachability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.LocalStop;
import org.oristool.analyzer.state.State;
import org.oristool.models.gspn.chains.AbsorptionProbs;
import org.oristool.models.gspn.chains.DTMC;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

/**
 * Analysis of a {@link SuccessionGraph} that computes the underlying CTMC by
 * eliminating vanishing states.
 *
 * <p>Vanishing states are detected by inspecting the {@link SPNState} feature
 * of states in the graph and checking whether
 * {@code exitRate() == Double.POSITIVE_INFINITY}.
 *
 * <p>Transition probabilities are read from the graph by extracting
 * {@link FiringProbability} features of {@link Succession} between states.
 *
 * <p>The underlying CTMC is returned as a {@link DTMC} where each state is
 * associated with a marking and an exit rate (the sojourn time rate, encoded by
 * the same {@link SPNState} instance used in the input graph). Self-loops can
 * be present in the DTMC.
 *
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
     * to compare states) and an exit rate (the rate of the sojourn time).
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
            // successions can be marked as visited by buildVanishingDTMC
            if (visited.contains(succ))
                continue;

            SPNState i = feature(succ.getParent());
            SPNState j = feature(succ.getChild());

            if (!isVanishing(i) && !isVanishing(j)) {
                addProb(i, j, prob(succ), result.probsGraph());
                visited.add(succ);

            } else if ((!isVanishing(i) && isVanishing(j)) ||
                       (i == root && isVanishing(root))) {

                List<Succession> tangibleToVanishingEdges = new ArrayList<>();
                AbsorptionProbs<SPNState> absorption = AbsorptionProbs.compute(
                        buildVanishingDTMC(succ, tangibleToVanishingEdges, visited));

                for (Set<SPNState> b : absorption.bscc()) {
                    if (b.size() != 1 || isVanishing(b.iterator().next())) {
                        throw new IllegalStateException("The input graph contains a timelock");
                    }
                }

                for (Succession succToVanishing : tangibleToVanishingEdges) {
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
                }

                if (i == root && isVanishing(root)) {
                    // we have just processed the vanishing DTMC containing the root
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
        // impose sojourn rate equal to zero for stop states
        SPNState f = state.getFeature(SPNState.class);
        if (state.hasFeature(LocalStop.class))
            return new SPNState(f.state(), 0.0);
        else
            return f;
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

    /**
     * Computes the undirected (i.e., forward and backward) reachability set of
     * a vanishing node, stopping on tangible nodes.
     *
     * <p>Successions from tangible states to vanishing ones (including a vanishing
     * root) are added to the input list {@code tangibleToVanishig}.
     *
     * <p>All successions explored in this vanishing subgraph are added to the input
     * set {@code visited}.
     *
     * @param initialSucc Initial transition to a vanishing node
     * @param tangibleToVanishingEdges list where successions from tangible to vanishing are added
     * @param visited set where visited successions are added
     * @returns DTMC of vanishing transitions
     */
    private MutableValueGraph<SPNState, Double> buildVanishingDTMC(final Succession initialSucc,
            final List<Succession> tangibleToVanishingEdges, final Set<Succession> visited) {

        MutableValueGraph<SPNState, Double> dtmc = ValueGraphBuilder
                .directed().allowsSelfLoops(true).build();

        Set<Node> found = new HashSet<>();
        Deque<NeighborIterator> stack = new ArrayDeque<>();

        Node a = graph.getNode(initialSucc.getParent());
        Node b = graph.getNode(initialSucc.getChild());
        if (isVanishing(a)) {
            // vanishing initial node
            found.add(a);
            stack.push(new NeighborIterator(a, graph));
        } else {
            // (a, b) was an edge from tangible to vanishing
            assert isVanishing(b);
            found.add(b);
            stack.push(new NeighborIterator(b, graph));
        }

        while (!stack.isEmpty()) {
            if (!stack.peek().hasNext()) {
                stack.pop();
            } else {
                boolean isForward = stack.peek().isNextSuccessor();
                Node i = stack.peek().node();  // current DFS node
                Node j = stack.peek().next();  // the neighbor

                if (isForward) {
                    // connect vanishing/tangible nodes on forward edge (i,j)
                    Set<Succession> succIJ = graph.getSuccessions(i, j);
                    for (Succession succ : succIJ) {
                        addProb(feature(i), feature(j), prob(succ), dtmc);
                        visited.add(succ);
                    }
                    // if j is tangible, it is a BSCC of the DTMC
                    if (!isVanishing(j)) {
                        dtmc.putEdgeValue(feature(j), feature(j), 1.0);
                    }

                } else {
                    // use backward edges (j,i) to find incoming tangible nodes
                    if (!isVanishing(j)) {
                        // (j,i) enters the vanishing zone
                        Set<Succession> succJI = graph.getSuccessions(j, i);
                        tangibleToVanishingEdges.addAll(succJI);
                        visited.addAll(succJI);
                    }
                }

                if (!found.contains(j) && isVanishing(j)) {
                    // if we find a new vanishing node (forward or backward)
                    // we start exploring it (forward and backward)
                    found.add(j);
                    stack.push(new NeighborIterator(j, graph));
                }
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
        private boolean finishedPredecessors = false;

        NeighborIterator(Node n, SuccessionGraph graph) {
            this.node = n;
            this.pre = graph.getPredecessors(n).iterator();
            this.succ = graph.getSuccessors(n).iterator();
        }

        public Node node() {
            return node;
        }

        public boolean isNextSuccessor() {
            if (!finishedPredecessors && !pre.hasNext()) {
                finishedPredecessors = true;
            }
            return finishedPredecessors;
        }

        @Override
        public boolean hasNext() {
            return isNextSuccessor() ? succ.hasNext() : pre.hasNext();
        }

        @Override
        public Node next() {
            return isNextSuccessor() ? succ.next() : pre.next();
        }
    }
}
