/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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

package org.oristool.analyzer.graph;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionFeature;
import org.oristool.analyzer.state.State;

/**
 * A graph of {@code Succession} objects.
 *
 * <p>Multiple successions can be associated with the same ordered pair of
 * nodes.
 */
public final class SuccessionGraph {

    // nodes and edges representation: nodes are unique, edges are node pairs
    private Node root;
    private final Set<Node> nodes = new LinkedHashSet<Node>();

    // successors and predecessors sets for each node
    private final Map<Node, Set<Node>> successors = new LinkedHashMap<>();
    private final Map<Node, Set<Node>> predecessors = new LinkedHashMap<>();

    // state object associated with each node (and inverse relation)
    private final Map<Node, State> nodeState = new LinkedHashMap<>();
    private final Map<State, Node> stateNode = new LinkedHashMap<>();

    // successions objects associated with each edge
    private final Map<Edge, Set<Succession>> successions = new LinkedHashMap<>();

    /**
     * Adds a node and an associated {@code State} instance.
     *
     * <p>The node is added only if not present.
     *
     * @param n node
     * @param s state
     * @return true if the node was not already present
     */
    private boolean addNode(Node n, State s) {

        boolean newNode = nodes.add(n);

        if (newNode) {
            nodeState.put(n, s);
            stateNode.put(s, n);
            successors.put(n, new LinkedHashSet<>());
            predecessors.put(n, new LinkedHashSet<>());
        }

        return newNode;
    }

    /**
     * Adds an edge between existing nodes.
     *
     * <p>The edge is added only if not present.
     *
     * @param n1 source node
     * @param n2 destination node
     * @return true if the edge was not already present
     */
    private boolean addEdge(Node n1, Node n2) {

        boolean newEdge = successors.get(n1).add(n2);
        predecessors.get(n2).add(n1);

        if (newEdge)
            successions.put(new Edge(n1, n2), new LinkedHashSet<Succession>());

        return newEdge;
    }

    /**
     * Adds a succession to the graph.
     *
     * <p>The parent state of the succession must be already present in the graph.
     *
     * <p>If the succession has no parent state, the child state is used as root of
     * the graph.
     *
     * @param s input succession
     * @return true if the child node of the succession was not present in the graph
     */
    public boolean addSuccession(Succession s) {

        if (s.getParent() == null) {
            // sets the root node, discarding the succession object
            if (this.root != null)
                throw new IllegalStateException("Root already set");

            this.root = new Node();
            this.addNode(this.root, s.getChild());
            return true;
        }

        Node predecessorNode = stateNode.get(s.getParent());
        if (predecessorNode == null)
            throw new IllegalArgumentException(
                    "No node is associated with the predecessor state of succession:\n" + s);

        boolean newSuccessorNode = false;
        Node successorNode = stateNode.get(s.getChild());
        if (successorNode == null) {
            newSuccessorNode = true;
            successorNode = new Node();
            this.addNode(successorNode, s.getChild());
        }

        this.addEdge(predecessorNode, successorNode);
        this.successions.get(new Edge(predecessorNode, successorNode)).add(s);

        return newSuccessorNode;
    }

    public Node getRoot() {
        return root;
    }

    public Set<Node> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public Set<Node> getPredecessors(Node n) {
        return predecessors.get(n);
    }

    public Set<Node> getSuccessors(Node n) {
        return successors.get(n);
    }

    public State getState(Node n) {
        return nodeState.get(n);
    }

    public Node getNode(State s) {
        return stateNode.get(s);
    }

    public Set<State> getStates() {
        return stateNode.keySet();
    }

    /**
     * Returns a copy of all successions in the graph.
     *
     * @return a set of successions
     */
    public Set<Succession> getSuccessions() {
        Set<Succession> r = new LinkedHashSet<>();

        for (Set<Succession> s : successions.values())
            r.addAll(s);

        return r;
    }

    public Set<Succession> getSuccessions(Node n1, Node n2) {
        return successions.getOrDefault(new Edge(n1, n2), Collections.emptySet());
    }

    /**
     * Returns a copy of all incoming successions of a node.
     *
     * @param n input node
     * @return a set of successions
     */
    public Set<Succession> getIncomingSuccessions(Node n) {
        Set<Succession> ingoingSuccessions = new LinkedHashSet<>();

        for (Node predecessor : predecessors.get(n))
            ingoingSuccessions.addAll(successions.get(new Edge(predecessor, n)));

        return ingoingSuccessions;
    }

    /**
     * Returns a copy of all outgoing successions of a node.
     *
     * @param n input node
     * @return a set of successions
     */
    public Set<Succession> getOutgoingSuccessions(Node n) {
        Set<Succession> outgoingSuccessions = new LinkedHashSet<>();

        for (Node successor : successors.get(n))
            outgoingSuccessions.addAll(successions.get(new Edge(n, successor)));

        return outgoingSuccessions;
    }

    /**
     * Modifies this graph by applying a function to each state. This can also
     * modify the structure of the graph (confluences can be introduced or removed).
     *
     * @param stateChange function applied to each state
     * @return resulting graph
     */
    public SuccessionGraph modifyStates(UnaryOperator<State> stateChange) {

        final SuccessionGraph newGraph = new SuccessionGraph();
        final Node root = this.getRoot();
        final State rootState = stateChange.apply(this.getState(root));

        // add the initial succession of root node
        newGraph.addSuccession(new Succession(null, null, rootState));

        // visit the rest of the graph
        Deque<Node> stack = new ArrayDeque<>();
        Deque<Iterator<Node>> adj = new ArrayDeque<>();
        Set<Node> opened = new HashSet<>();

        opened.add(root);
        stack.push(root);
        adj.push(this.getSuccessors(root).iterator());

        while (!stack.isEmpty()) {
            if (adj.peek().hasNext()) {
                Node prev = stack.peek();
                Node next = adj.peek().next();

                // visit edge
                for (Succession succ : this.getSuccessions(prev, next)) {
                    newGraph.addSuccession(modifySuccession(succ, stateChange));
                }

                // open node
                if (!opened.contains(next)) {
                    opened.add(next);
                    stack.push(next);
                    adj.push(this.getSuccessors(next).iterator());
                }

            } else {
                stack.pop();
                adj.pop();
            }
        }

        assert opened.size() == this.getNodes().size();
        return newGraph;
    }

    private static Succession modifySuccession(Succession succ, UnaryOperator<State> stateChange) {

        Succession newSucc = new Succession(
                stateChange.apply(succ.getParent()),
                succ.getEvent(),
                stateChange.apply(succ.getChild()));

        for (SuccessionFeature f : succ.getFeatures())
            newSucc.addFeature(f);

        return newSucc;
    }
}
