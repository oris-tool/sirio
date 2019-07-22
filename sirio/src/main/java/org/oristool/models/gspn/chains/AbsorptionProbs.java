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

package org.oristool.models.gspn.chains;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.oristool.util.Pair;

import com.google.common.graph.ValueGraph;

/**
 * Computation of absorption probabilities into BSCCs from all states.
 *
 * @param <S> type of states/nodes in the input DTMC
 */
public final class AbsorptionProbs<S> {
    private final List<S> transientNodes;
    private final List<Set<S>> bscc;
    private final Map<S, Integer> transientIndex;
    private final Map<S, Integer> bsccOf;
    private final DMatrixRMaj probs;  // transientNodes.size() x bscc.size()

    private AbsorptionProbs(List<S> transientNodes, List<Set<S>> bscc,
            Map<S, Integer> transientIndex, Map<S, Integer> bsccOf, DMatrixRMaj probs) {
        this.transientNodes = transientNodes;
        this.bscc = bscc;
        this.transientIndex = transientIndex;
        this.bsccOf = bsccOf;
        this.probs = probs;
    }

    /**
     * Returns the list of bottom strongly connected components.
     *
     * @return list of BSCCs
     */
    public List<Set<S>> bscc() {
        return bscc;
    }

    /**
     * Returns the absorption probability from a transient node to a BSCC.
     *
     * @param transientIndex index of the transient state
     * @param bscc index of an absorbing BSCC
     * @return the absorption probability
     */
    public double probs(int transientIndex, int bscc) {
        return probs.get(transientIndex, bscc);
    }

    /**
     * Returns the list of transient nodes.
     *
     * @return the list of transient nodes
     */
    public List<S> transientNodes() {
        return transientNodes;
    }

    /**
     * Returns the position of a transient node in the list.
     *
     * @param transientNode a transient node
     * @return position of the node
     */
    public int transientIndex(S transientNode) {
        return transientIndex.get(transientNode);
    }

    /**
     * Returns the position of the BSCC of a BSCC node.
     *
     * @param bsccNode a BSCC node
     * @return position of the node BSCC
     */
    public int bsccIndexFor(S bsccNode) {
        return bsccOf.get(bsccNode);
    }

    /**
     * Checks whether a node is in the transient set.
     *
     * @param node a node
     * @return true if the node is in the transient set
     */
    public boolean isTransient(S node) {
        return transientIndex.containsKey(node);
    }

    /**
     * Checks whether a node is in some BSCC.
     *
     * @param node a node
     * @return true if the node is in some BSCC
     */
    public boolean isBottom(S node) {
        return bsccOf.containsKey(node);
    }

    /**
     * Computes the BSCC components of an input DTMC and the absorption
     * probabilities from each transient state.
     *
     * @param <S> type of DTMC states
     * @param dtmcProbs input DTMC
     * @return BSCCs and absorption probabilities from each transient state
     */
    public static <S> AbsorptionProbs<S> compute(ValueGraph<S, Double> dtmcProbs) {
        Pair<List<S>, List<Set<S>>> components = components(dtmcProbs);
        List<S> transientNodes = components.first();
        List<Set<S>> bscc =  components.second();

        Map<S, Integer> transientIndex = new HashMap<>();
        for (int i = 0; i < transientNodes.size(); i++) {
            S node = transientNodes.get(i);
            transientIndex.put(node, i);
        }

        Map<S, Integer> bsccOf = new HashMap<>();
        for (int i = 0; i < bscc.size(); i++) {
            for (S node : bscc.get(i)) {
                bsccOf.put(node, i);
            }
        }

        // compute absorbing probabilities from each transient state
        // as (Identity - TransientToTransient) * x = TransientToBSCC
        int n = transientNodes.size();
        int m = bscc.size();

        DMatrixRMaj a = CommonOps_DDRM.identity(n);
        DMatrixRMaj b = new DMatrixRMaj(n, m);
        DMatrixRMaj x = new DMatrixRMaj(n, m);

        for (int i = 0; i < transientNodes.size(); i++) {
            S node = transientNodes.get(i);
            for (S succ : dtmcProbs.successors(node)) {
                double prob = dtmcProbs.edgeValueOrDefault(node, succ, 0.0);
                if (bsccOf.containsKey(succ)) {
                    b.add(i, bsccOf.get(succ), prob);
                } else {
                    a.add(i, transientIndex.get(succ), -prob);
                }
            }
        }

        LinearSolver<DMatrixRMaj, DMatrixRMaj> solver =
                LinearSolverFactory_DDRM.lu(n);
        if (!solver.setA(a))
            throw new IllegalArgumentException("Singular matrix");
        solver.solve(b, x);

        assert validAbsorptionProbs(x);
        return new AbsorptionProbs<>(transientNodes, bscc, transientIndex, bsccOf, x);
    }

    private static boolean validAbsorptionProbs(DMatrixRMaj x) {
        DMatrixRMaj sumByRows = CommonOps_DDRM.sumRows(x, null);
        for (int i = 0; i < sumByRows.numRows; i++)
            if (Math.abs(sumByRows.get(i, 0) - 1.0) > 1e-9)
                return false;
        return true;
    }

    /**
     * Computes the reverse post-visit DFS order of the reverse graph.

     * @param dtmcProbs input graph
     * @return nodes in reverse post-visit order for the reverse graph
     */
    private static <S> Deque<S> reversePostOrder(ValueGraph<S, Double> dtmcProbs) {

        Deque<S> reversePostOrder = new ArrayDeque<>(dtmcProbs.nodes().size());

        // iterative DFS on reverse graph
        Deque<S> stack = new ArrayDeque<>();
        Deque<Iterator<S>> adj = new ArrayDeque<>();
        Set<S> newNodes = new HashSet<>(dtmcProbs.nodes());

        while (!newNodes.isEmpty()) {
            S root = newNodes.iterator().next();
            stack.push(root);
            adj.push(dtmcProbs.predecessors(root).iterator());
            newNodes.remove(root);

            while (!stack.isEmpty()) {
                if (adj.peek().hasNext()) {
                    S next = adj.peek().next();
                    if (newNodes.contains(next)) {
                        newNodes.remove(next);
                        stack.push(next);
                        adj.push(dtmcProbs.predecessors(next).iterator());
                    }
                } else {
                    reversePostOrder.push(stack.pop());
                    adj.pop();
                }
            }
        }

        assert stack.isEmpty() && adj.isEmpty() && newNodes.isEmpty();
        assert reversePostOrder.size() == dtmcProbs.nodes().size();
        return reversePostOrder;
    }

    /**
     * Computes the set of transient states and bottom strongly connected
     * components (BSCCs).
     *
     * @return the list of transient states and the list of BSCCs
     */
    private static <S> Pair<List<S>, List<Set<S>>> components(ValueGraph<S, Double> dtmcProbs) {

        Deque<S> reversePostOrder = reversePostOrder(dtmcProbs);

        // iterative DFS with roots in post-visit reverse order
        Deque<S> stack = new ArrayDeque<>();
        Deque<Iterator<S>> adj = new ArrayDeque<>();
        Set<S> newNodes = new HashSet<>(dtmcProbs.nodes());

        // output BSCC and transient nodes
        List<S> transientNodes = new ArrayList<>();
        List<Set<S>> bscc = new ArrayList<>();

        while (!reversePostOrder.isEmpty()) {
            // use all nodes as roots, skip visited ones
            S root = reversePostOrder.pop();
            if (!newNodes.remove(root))
                continue;
            stack.push(root);
            adj.push(dtmcProbs.successors(root).iterator());

            // open new SCC
            Collection<S> nextSCC = new HashSet<>();
            nextSCC.add(root);
            while (!stack.isEmpty()) {
                if (adj.peek().hasNext()) {
                    S next = adj.peek().next();
                    if (newNodes.contains(next)) {
                        newNodes.remove(next);
                        stack.push(next);
                        adj.push(dtmcProbs.successors(next).iterator());
                        nextSCC.add(next);
                    } else if (nextSCC != transientNodes
                            && !nextSCC.contains(next)) {
                        // SCC is not bottom: add nodes to transient set
                        transientNodes.addAll(nextSCC);
                        nextSCC = transientNodes;
                    }

                } else {
                    stack.pop();
                    adj.pop();
                }
            }

            // close SCC
            if (nextSCC != transientNodes) {
                bscc.add((Set<S>)nextSCC);
            }
        }

        return Pair.of(transientNodes, bscc);
    }
}
