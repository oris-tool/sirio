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

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.policy.FIFOPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.models.pn.InitialPetriStateBuilder;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

class GSPNGraphGenerator {

    // genera il successionGraph
    public static SuccessionGraph generateGraph(PetriNet petriNet,
            Marking initialMarking, MarkingCondition stopCondition,
            AnalysisMonitor monitor) {

        GSPNReachabilityComponentsFactory factory = new GSPNReachabilityComponentsFactory(
                null, null, false, new FIFOPolicy(), stopCondition, monitor);

        Analyzer<PetriNet, Transition> analyzer = new Analyzer<PetriNet, Transition>(
                factory, petriNet,
                InitialPetriStateBuilder.computeInitialState(petriNet,
                        initialMarking));

        SuccessionGraph graph = analyzer.analyze();
        return graph;

    }

    public static Map<SuccessionGraph, Double> generateReducedGraphs(
            GSPNGraphAnalyzer analyzerG, Map<Node, Double> rootList) {
        SuccessionGraph s_graph = analyzerG.getGraph();
        Map<Node, Map<Node, Set<List<Succession>>>> index = analyzerG
                .getIndex();
        Map<SuccessionGraph, Double> graphMap = new HashMap<>();

        class Couple {
            public ReducedTransition transition;
            public Node nodeChild;
            public State stateParent;
            public State stateChild;

            Couple(ReducedTransition a, Node b, State a1, State b1) {
                transition = a;
                nodeChild = b;
                stateParent = a1;
                stateChild = b1;

            }

            @Override
            public boolean equals(Object other) {

                Couple othercouple = (Couple) other;

                return transition.equals(othercouple.transition)
                        && nodeChild.equals(othercouple.nodeChild)
                        && stateParent.equals(othercouple.stateParent)
                        && stateChild.equals(othercouple.stateChild);
            }
        }

        Map<Node, Set<Couple>> couples = new HashMap<Node, Set<Couple>>();
        for (Node n : index.keySet()) {
            Set<Couple> set = new HashSet<Couple>();

            if (analyzerG.isCyclesPresent()) {

                for (Node v : index.get(n).keySet()) {

                    if (analyzerG.getVanishing().contains(v)) {
                        Map<Node, Double> v_map = analyzerG.getCycleMap().get(
                                v);

                        for (List<Succession> linList : index.get(n).get(v)) {

                            Succession vanishSucc = linList.get(0);
                            double transRate = ((Transition) vanishSucc
                                    .getEvent())
                                    .getFeature(RateExpressionFeature.class)
                                    .getRate(
                                            analyzerG.getPetriNet(),
                                            vanishSucc
                                                    .getParent()
                                                    .getFeature(
                                                            PetriStateFeature.class)
                                                    .getMarking());
                            for (Node mapNode : v_map.keySet()) {
                                if (n != mapNode
                                        && v_map.get(mapNode).doubleValue() != 0.0) {
                                    State parent = s_graph.getState(n);
                                    State child = s_graph.getState(mapNode);

                                    ReducedTransition trans = new ReducedTransition(
                                            "ReducedTransition from "
                                                    + parent.toString()
                                                    + " to " + child.toString(),
                                            transRate
                                                    * v_map.get(mapNode)
                                                            .doubleValue());

                                    set.add(new Couple(trans, mapNode, parent,
                                            child));
                                }

                            }

                        }
                    } else {
                        if (n != v) {

                            State parent = s_graph.getState(n);
                            State child = s_graph.getState(v);

                            ReducedTransition trans = new ReducedTransition(
                                    "from " + parent.toString() + " to "
                                            + child.toString());

                            for (List<Succession> list : index.get(n).get(v)) {
                                trans.calculateRateFromList(list, analyzerG);
                            }

                            set.add(new Couple(trans, v, parent, child));
                        }
                    }
                }

            } else {
                for (Node v : index.get(n).keySet()) {
                    // to eliminate self-loops
                    if (n != v) {
                        State parent = s_graph.getState(n);
                        State child = s_graph.getState(v);

                        ReducedTransition trans = new ReducedTransition(
                                "ReducedTransition from " + parent.toString()
                                        + " to " + child.toString());

                        for (List<Succession> list : index.get(n).get(v)) {
                            trans.calculateRateFromList(list, analyzerG);
                        }

                        // to check possible errors
                        if (trans.getRate() > 0.0) {
                            set.add(new Couple(trans, v, parent, child));

                        } else
                            throw new RuntimeException(
                                    "ReducedTransition from "
                                            + parent.toString() + " to "
                                            + child.toString()
                                            + " hasn't a positive rate");

                    }

                }
            }
            couples.put(n, set);
        }

        for (Node root : rootList.keySet()) {
            SuccessionGraph newGraph = new SuccessionGraph();
            Node cursor;
            Set<Node> known = new HashSet<Node>();
            Queue<Node> queue = new LinkedList<Node>();
            queue.add(root);
            known.add(root);
            newGraph.addSuccession(new Succession(null, null, analyzerG
                    .getGraph().getState(root)));
            while (!queue.isEmpty()) {
                cursor = queue.remove();
                if (couples.containsKey(cursor)) {
                    for (Couple c : couples.get(cursor)) {
                        Succession newSuccession = new Succession(
                                c.stateParent, c.transition, c.stateChild);
                        newGraph.addSuccession(newSuccession);
                        if (!known.contains(c.nodeChild)) {
                            known.add(c.nodeChild);
                            queue.add(c.nodeChild);
                        }
                    }
                }
            }
            graphMap.put(newGraph, rootList.get(root));
        }

        return graphMap;

    }

    public static Map<Node, Double> calculateRootAbsorption(
            GSPNGraphAnalyzer analyzerG, Node root) {

        Map<Node, Double> rootAndProbabilities = new HashMap<Node, Double>();
        if (analyzerG.isRootVanishing()) {

            double[] array = analyzerG.calculateAbsorptionVector(root);
            for (int i = 0; i < array.length; i++) {
                if (array[i] > 0.0) {
                    rootAndProbabilities.put(
                            analyzerG.getTangiblesList().get(i), array[i]);
                }
            }

        } else
            rootAndProbabilities.put(root, 1.0);

        return rootAndProbabilities;
    }

}
