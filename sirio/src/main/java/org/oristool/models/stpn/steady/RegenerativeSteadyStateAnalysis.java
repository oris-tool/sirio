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

package org.oristool.models.stpn.steady;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;
import org.oristool.analyzer.stop.MonitorStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.models.stpn.trees.RegenerativeComponentsFactory;
import org.oristool.models.stpn.trees.StochasticStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

class RegenerativeSteadyStateAnalysis<R> {

    private Set<Marking> reachableMarkings;
    private Map<R, Map<Marking, BigDecimal>> sojournMap;
    private SteadyStateSolution<Marking> steadyState;
    private EmbeddedDTMC<R> embeddedDTMC;

    public EmbeddedDTMC<R> geteDTMC() {
        return embeddedDTMC;
    }

    public Map<R, Map<Marking, BigDecimal>> getSojournMap() {
        return sojournMap;
    }

    public SteadyStateSolution<Marking> getSteadyState() {
        return steadyState;
    }

    public Set<Marking> getReachableMarkings() {
        return Collections.unmodifiableSet(reachableMarkings);
    }

    private R initialRegeneration;
    private PetriNet petriNet;
    private EnumerationPolicy enumerationPolicy;
    private StopCriterion absorbingCondition;

    public R getInitialRegeneration() {
        return initialRegeneration;
    }

    public Set<R> getRegenerations() {
        return regenerations;
    }

    public PetriNet getPetriNet() {
        return petriNet;
    }

    public EnumerationPolicy getEnumerationPolicy() {
        return enumerationPolicy;
    }

    public StopCriterion getAbsorbingCondition() {
        return absorbingCondition;
    }

    private Map<R, Map<Marking, Set<State>>> localClasses;
    private Map<R, Map<R, Set<State>>> regenerationClasses;

    public Map<R, Map<Marking, Set<State>>> getLocalClasses() {
        return localClasses;
    }

    public Map<R, Map<R, Set<State>>> getRegenerationClasses() {
        return regenerationClasses;
    }

    private Set<R> regenerations;

    private RegenerativeSteadyStateAnalysis() {
    }

    /**
     * Computes marking steady-state probabilities from sojourn times in each
     * regeneration epoch and steady-state probabilities of initial regenerations.
     *
     * @param a result with reachable markings and sojourn map
     * @return steady-state distribution
     */
    private static <R> Map<Marking, BigDecimal> calculateSteadyState(
            RegenerativeSteadyStateAnalysis<R> a) {

        a.embeddedDTMC = EmbeddedDTMC.compute(a.getRegenerations(), a.getRegenerationClasses());
        Map<R, BigDecimal> eDTMCSteadyState = a.embeddedDTMC.getSteadyState();

        Map<Marking, BigDecimal> steadyState = new HashMap<>();
        BigDecimal normalizationFactor = BigDecimal.ZERO;
        for (Marking m : a.reachableMarkings) {
            BigDecimal aij = BigDecimal.ZERO;
            for (R reg : a.getRegenerations()) {
                if (a.sojournMap.get(reg).containsKey(m)) {
                    BigDecimal prob = eDTMCSteadyState.get(reg).multiply(
                            a.sojournMap.get(reg).get(m));
                    aij = aij.add(prob);
                    normalizationFactor = normalizationFactor.add(prob);
                }
            }
            steadyState.put(m, aij);
        }

        for (Marking m : a.reachableMarkings) {
            steadyState.replace(m,
                    steadyState.get(m).divide(normalizationFactor, MathContext.DECIMAL128));
        }

        return steadyState;
    }

    /**
     * Computes steady-state probabilities for each state.
     *
     * @param <R> regeneration type
     * @param petriNet Petri net
     * @param initialRegeneration initial regeneration ofthe analysis
     * @param stateBuilder builder to be used
     * @param postProcessor postprocessor to apply to each state
     * @param enumerationPolicy policy for state selection
     * @param absorbingCondition absorbing condition
     * @param l logger
     * @param monitor monitor
     * @param verbose whether to output additional information
     * @return result of the analysis
     */
    public static <R> RegenerativeSteadyStateAnalysis<R> compute(PetriNet petriNet,
            R initialRegeneration, StateBuilder<R> stateBuilder, SuccessionProcessor postProcessor,
            EnumerationPolicy enumerationPolicy, StopCriterion absorbingCondition,
            AnalysisLogger l, AnalysisMonitor monitor, boolean verbose) {

        Locale.setDefault(Locale.US);
        RegenerativeSteadyStateAnalysis<R> a = new RegenerativeSteadyStateAnalysis<R>();

        if (l != null) {
            l.log(">> Regenerative analysis starting from " + initialRegeneration);
            if (absorbingCondition != MarkingCondition.NONE)
                l.log(", absorbingCondition");
            l.log(")\n");
        }

        a.petriNet = petriNet;
        a.initialRegeneration = initialRegeneration;
        a.enumerationPolicy = enumerationPolicy;
        a.absorbingCondition = absorbingCondition;

        a.localClasses = new HashMap<>();
        a.regenerationClasses = new HashMap<>();
        a.reachableMarkings = new LinkedHashSet<>();
        a.sojournMap = new HashMap<>();

        // Adds the initialRegeneration to the list of regenerations
        Set<R> reachedRegenerations = new LinkedHashSet<>();
        reachedRegenerations.add(initialRegeneration);

        Queue<R> initialRegenerations = new LinkedList<>();
        initialRegenerations.add(initialRegeneration);

        // Performs the analysis from the initial marking and then from any
        // regenerative marking ever visited
        while (!initialRegenerations.isEmpty()) {
            // Removes the next initial marking from the queue
            R currentInitialRegeneration = initialRegenerations.remove();

            // Performs the analysis starting from the current initial marking
            if (l != null)
                l.log(">> Analyzing from initial regeneration (" + currentInitialRegeneration
                        + ")\n");

            RegenerativeComponentsFactory f = new RegenerativeComponentsFactory(false, null, null,
                    true, postProcessor, enumerationPolicy, OmegaBigDecimal.POSITIVE_INFINITY,
                    absorbingCondition, null, 0, monitor);

            Analyzer<PetriNet, Transition> analyzer = new Analyzer<PetriNet, Transition>(f,
                    petriNet, stateBuilder.build(currentInitialRegeneration));

            SuccessionGraph graph = analyzer.analyze();

            a.sojournMap.put(currentInitialRegeneration, new HashMap<>());

            if (l != null)
                l.log(">> " + graph.getNodes().size() + " state classes found\n");

            if (monitor != null) {
                if (f.getGlobalStopCriterion() instanceof MonitorStopCriterion
                        && ((MonitorStopCriterion) f.getGlobalStopCriterion())
                                .interruptedExecution()) {
                    monitor.notifyMessage("Interrupted after the enumeration of "
                            + graph.getNodes().size() + " state classes");
                    return null;
                } else
                    monitor.notifyMessage("Analyzing the transient tree ("
                            + graph.getNodes().size() + " state classes)");
            }

            // Analyzes the state class tree in a depth first fashion
            Deque<Node> stack = new LinkedList<>();
            stack.push(graph.getRoot());
            String offset = "";
            while (!stack.isEmpty()) {

                Node n = stack.pop();
                if (n != null) {
                    State s = graph.getState(n);
                    PetriStateFeature petriFeature = s.getFeature(PetriStateFeature.class);
                    StochasticStateFeature stochasticFeature = s
                            .getFeature(StochasticStateFeature.class);

                    if (s.hasFeature(Regeneration.class)) {

                        @SuppressWarnings("unchecked")
                        // regenerations should be of type R (!)
                        R regeneration = (R) s.getFeature(Regeneration.class).getValue();

                        if (!reachedRegenerations.contains(regeneration)) {
                            reachedRegenerations.add(regeneration);
                            initialRegenerations.add(regeneration);
                        }
                    }

                    a.reachableMarkings.add(petriFeature.getMarking());

                    if (s.hasFeature(Regeneration.class)
                            && graph.getSuccessors(n).size() == 0
                            && !n.equals(graph.getRoot())) {

                        @SuppressWarnings("unchecked")
                        // regenerations should be of type R (!)
                        R regeneration = (R) s.getFeature(Regeneration.class).getValue();

                        // Any regenerative leaf class other than the root is
                        // added to the entry numStatesof
                        // regenerationClasses corresponding to the current
                        // initial and class marking
                        if (!a.regenerationClasses.containsKey(currentInitialRegeneration))
                            a.regenerationClasses.put(currentInitialRegeneration,
                                    new HashMap<>());

                        if (!a.regenerationClasses.get(currentInitialRegeneration).containsKey(
                                regeneration))
                            a.regenerationClasses.get(currentInitialRegeneration).put(regeneration,
                                    new LinkedHashSet<>());

                        a.regenerationClasses.get(currentInitialRegeneration).get(regeneration)
                                .add(s);

                    } else {
                        // If this state class is not a regenerative leaf (or is
                        // the root node), it is added to
                        // the entry of localClasses corresponding to the
                        // current initial and class marking
                        if (!a.localClasses.containsKey(currentInitialRegeneration))
                            a.localClasses.put(currentInitialRegeneration,
                                    new HashMap<Marking, Set<State>>());

                        if (!a.localClasses.get(currentInitialRegeneration).containsKey(
                                petriFeature.getMarking()))
                            a.localClasses.get(currentInitialRegeneration).put(
                                    petriFeature.getMarking(), new LinkedHashSet<State>());

                        a.localClasses.get(currentInitialRegeneration)
                                .get(petriFeature.getMarking()).add(s);

                        if (!a.sojournMap.get(currentInitialRegeneration).containsKey(
                                petriFeature.getMarking())) {
                            a.sojournMap.get(currentInitialRegeneration).put(
                                    petriFeature.getMarking(), BigDecimal.ZERO);
                        }

                        Set<Succession> successions = graph.getOutgoingSuccessions(n);
                        BigDecimal sojournTime = BigDecimal.ZERO;
                        for (Succession succession : successions) {
                            StochasticStateFeature tmpStochasticFeature =
                                    new StochasticStateFeature(stochasticFeature);

                            Variable variable = new Variable(succession.getEvent().getName());
                            tmpStochasticFeature.conditionToMinimum(variable);

                            BigDecimal mean = tmpStochasticFeature.computeMeanValue(variable);
                            BigDecimal mass = succession.getChild()
                                    .getFeature(ReachingProbabilityFeature.class).getValue();
                            sojournTime = sojournTime.add(mean.multiply(mass));
                        }

                        a.sojournMap.get(currentInitialRegeneration).replace(
                                petriFeature.getMarking(),
                                a.sojournMap.get(currentInitialRegeneration)
                                        .get(petriFeature.getMarking()).add(sojournTime));
                    }

                    stack.push(null);
                    for (Node m : graph.getSuccessors(n))
                        stack.push(m);

                    offset = "  " + offset;

                } else {
                    if (!stack.isEmpty())
                        offset = offset.substring(2);
                }
            }

        }

        a.regenerations = reachedRegenerations;
        a.steadyState = new SteadyStateSolution<Marking>(calculateSteadyState(a));

        if (monitor != null)
            monitor.notifyMessage("Analysis completed");

        return a;
    }
}
