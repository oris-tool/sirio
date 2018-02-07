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
/**
 *
 */

package org.oristool.models.stpn;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Edge;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.PrintStreamLogger;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.MonitorStopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.StateDensityFunction;
import org.oristool.models.Engine;
import org.oristool.models.ValidationMessageCollector;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * A full analysis, including transient state classes of any marking, the sets
 * of reachable markings, always regenerative markings, never regenerative
 * markings, both regenerative and not regenerative markings.
 */
public class TransientAnalysis implements Engine {
    private Set<Marking> reachableMarkings;
    private Set<Marking> alwaysRegenerativeMarkings;
    private Set<Marking> neverRegenerativeMarkings;
    private Set<Marking> regenerativeAndNotRegenerativeMarkings;

    public Set<Marking> getReachableMarkings() {
        return Collections.unmodifiableSet(reachableMarkings);
    }

    public Set<Marking> getAlwaysRegenerativeMarkings() {
        return Collections.unmodifiableSet(alwaysRegenerativeMarkings);
    }

    public Set<Marking> getNeverRegenerativeMarkings() {
        return Collections.unmodifiableSet(neverRegenerativeMarkings);
    }

    public Set<Marking> getRegenerativeAndNotRegenerativeMarkings() {
        return Collections
                .unmodifiableSet(regenerativeAndNotRegenerativeMarkings);
    }

    private Marking initialMarking;
    private PetriNet petriNet;
    private EnumerationPolicy truncationPolicy;

    public Marking getInitialMarking() {
        return new Marking(initialMarking);
    }

    public PetriNet getPetriNet() {
        return petriNet;
    }

    public EnumerationPolicy getTruncationPolicy() {
        return truncationPolicy;
    }

    private Map<Marking, Set<State>> stateClasses;

    public Map<Marking, Set<State>> getStateClasses() {
        return stateClasses;
    }


    private TransientAnalysis() {
    }

    @Override
    public boolean canAnalyze(PetriNet petriNet,
            ValidationMessageCollector c) {

        boolean canAnalyze = true;
        for (Transition t : petriNet.getTransitions()) {
            if (!t.hasFeature(StochasticTransitionFeature.class)) {
                canAnalyze = false;
                c.addError("Transition '" + t + "' is not stochastic");
            }
        }

        return canAnalyze;
    }

    /**
     * Performs a full analysis of the net from an initial marking with respect to
     * the process of being in any reachable marking.
     *
     * @param petriNet stochastic time Petri net to be analyzed
     * @param initialMarking initial marking of the net
     * @param truncationPolicy encapsulates analysis options and pruning behavior
     * @return Result of the analysis
     */
    public static TransientAnalysis compute(PetriNet petriNet,
            Marking initialMarking, TruncationPolicy truncationPolicy) {

        return TransientAnalysis.compute(petriNet, initialMarking,
                truncationPolicy, MarkingCondition.NONE, new PrintStreamLogger(
                        System.out), null, true);
    }

    /**
     * Performs a full analysis of the net from an initial marking with respect to
     * the process of being in any reachable marking.
     *
     * @param petriNet stochastic time Petri net to be analyzed
     * @param initialMarking initial marking of the net
     * @param truncationPolicy encapsulates analysis options and pruning behavior
     * @param l analysis logger
     * @return Result of the analysis
     */
    public static TransientAnalysis compute(PetriNet petriNet,
            Marking initialMarking, TruncationPolicy truncationPolicy,
            AnalysisLogger l) {

        return TransientAnalysis.compute(petriNet, initialMarking,
                truncationPolicy, MarkingCondition.NONE, l, null, true);
    }

    /**
     * Performs a straight analysis of the net from an initial marking with
     * respect to the process of being in any reachable marking, stopping the
     * transient tree construction on nodes satisfying stopCondition.
     *
     * @param petriNet
     *            Stochastic Petri Net to be analyzed
     * @param initialMarking
     *            initial marking of the net
     * @param truncationPolicy
     *            encapsulates analysis options and pruning behavior
     * @param stopCondition
     *            the analysis is stopped on nodes satisfying this condition
     *            complete transient analysis)
     * @param l analysis logger
     * @param monitor monitor to interrupt the analysis
     * @param verbose true to enable verbose logging
     * @return Result of the analysis
     */
    public static TransientAnalysis compute(PetriNet petriNet,
            Marking initialMarking, TruncationPolicy truncationPolicy,
            MarkingCondition stopCondition, AnalysisLogger l,
            AnalysisMonitor monitor, boolean verbose) {

        TransientAnalysis a = new TransientAnalysis();

        if (l != null) {
            l.log(">> Standard analysis starting from " + initialMarking
                    + " (pruning threshold " + truncationPolicy.getEpsilon()
                    + ", tauAgeLimit " + truncationPolicy.getTauAgeLimit());
            if (stopCondition != MarkingCondition.NONE)
                l.log(", stopCondition");
            l.log(")\n");
        }

        long startTime = System.currentTimeMillis();

        a.petriNet = petriNet;
        a.initialMarking = initialMarking;
        a.truncationPolicy = truncationPolicy;

        a.stateClasses = new HashMap<Marking, Set<State>>();

        Set<Marking> sometimesRegenerativeMarkings = new LinkedHashSet<Marking>();
        Set<Marking> sometimesNotRegenerativeMarkings = new LinkedHashSet<Marking>();

        sometimesRegenerativeMarkings.add(initialMarking);

        // Performs the analysis starting from the initial marking
        StochasticComponentsFactory f = new StochasticComponentsFactory(true,
                null, null, false, truncationPolicy,
                truncationPolicy.getTauAgeLimit(), stopCondition, null, 0,
                monitor);

        Analyzer<PetriNet, Transition> analyzer = new Analyzer<PetriNet, Transition>(
                f, petriNet,
                new NewlyEnablingStateBuilder(petriNet, true)
                        .build(initialMarking));

        SuccessionGraph graph = analyzer.analyze();

        if (l != null)
            l.log(">> " + graph.getNodes().size() + " state classes found\n");

        // Visits the state class tree in a depth first fashion
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

        // Tree counts
        int treeClasses = 0;
        int treeZones = 0;
        int treeTerms = 0;
        int treeDepth = 0;

        Deque<Node> stack = new LinkedList<Node>();
        stack.push(graph.getRoot());
        String offset = "";
        while (!stack.isEmpty()) {

            Node n = stack.pop();
            if (n != null) {
                State s = graph.getState(n);
                PetriStateFeature petriFeature = s
                        .getFeature(PetriStateFeature.class);
                StochasticStateFeature stochasticFeature = s
                        .getFeature(StochasticStateFeature.class);
                StateDensityFunction densityFunction = stochasticFeature
                        .getStateDensity();
                TransientStochasticStateFeature transientFeature = s
                        .getFeature(TransientStochasticStateFeature.class);

                // Update tree counts
                treeClasses += 1;
                treeZones += densityFunction.getPartitionedGen().getFunctions()
                        .size();
                for (GEN g : densityFunction.getPartitionedGen().getFunctions())
                    treeTerms += g.getDensity().getExmonomials().size();
                treeDepth = Math.max(treeDepth, offset.length() / 2);

                // Logs this marking as seen in a regenerative or not
                // regenerative class
                if (s.hasFeature(Regeneration.class))
                    sometimesRegenerativeMarkings
                            .add(petriFeature.getMarking());
                else
                    sometimesNotRegenerativeMarkings.add(petriFeature
                            .getMarking());

                // Adds this state class to the set corresponding to its marking
                if (!a.stateClasses.containsKey(petriFeature.getMarking()))
                    a.stateClasses.put(petriFeature.getMarking(),
                            new LinkedHashSet<State>());

                a.stateClasses.get(petriFeature.getMarking()).add(s);

                if (l != null && verbose) {
                    l.log(offset);
                    if (s.hasFeature(Regeneration.class))
                        l.log("{" + n.id() + "}");
                    else
                        l.log("(" + n.id() + ")");
                    l.log(" "
                            + formatProbability(transientFeature
                                    .getReachingProbability()));
                    l.log(" "
                            + formatProbability(transientFeature
                                    .computeVisitedProbability(
                                            OmegaBigDecimal.ZERO,
                                            truncationPolicy.getTauAgeLimit(),
                                            stochasticFeature)));
                    l.log(" (" + petriFeature.getMarking() + ")");
                    l.log(" ["
                            + transientFeature
                                    .getEnteringTimeLowerBound(stochasticFeature)
                            + ","
                            + transientFeature
                                    .getEnteringTimeUpperBound(stochasticFeature)
                            + "].."
                            + transientFeature
                                    .getTimeUpperBound(stochasticFeature) + " ");

                    Set<Variable> exps = stochasticFeature.getEXPVariables();
                    for (Node m : graph.getSuccessors(n)) {
                        Succession succ = graph.getSuccessions(new Edge(n, m))
                                .iterator().next();
                        Transition t = (Transition) succ.getEvent();
                        Variable tau = new Variable(t.getName());
                        l.log(" ");
                        if (petriFeature.getNewlyEnabled().contains(t))
                            l.log("~");
                        else if (!stochasticFeature.getEXPVariables().contains(
                                tau))
                            ;
                        l.log("*");

                        l.log(t.getName());
                        l.log("["
                                + (exps.contains(tau) ? "0" : densityFunction
                                        .getMinBound(Variable.TSTAR, tau)
                                        .negate())
                                + ","
                                + (exps.contains(tau) ? "0" : densityFunction
                                        .getMinBound(tau, Variable.TSTAR))
                                + "]");
                        l.log("->");

                        if (succ.getChild().hasFeature(Regeneration.class))
                            l.log("{" + m.id() + "}");
                        else
                            l.log("(" + m.id() + ")");
                    }

                    Set<Transition> notFiredEnabledTransitions = petriNet
                            .getEnabledTransitions(petriFeature.getMarking());
                    for (Node m : graph.getSuccessors(n))
                        for (Succession succ : graph.getSuccessions(new Edge(n,
                                m)))
                            if (!notFiredEnabledTransitions.contains(succ
                                    .getEvent()))
                                throw new IllegalStateException(
                                        "A not enabled transition fired in the graph");
                            else
                                notFiredEnabledTransitions.remove(succ
                                        .getEvent());

                    for (Transition t : notFiredEnabledTransitions) {
                        l.log(" ");
                        if (petriFeature.getNewlyEnabled().contains(t))
                            l.log("~");
                        else if (!stochasticFeature.getEXPVariables().contains(
                                new Variable(t.getName())))
                            l.log("*");

                        l.log(t.getName());
                        OmegaBigDecimal eft = stochasticFeature
                                .getEXPVariables().contains(
                                        new Variable(t.getName())) ? OmegaBigDecimal.ZERO
                                : densityFunction.getMinBound(Variable.TSTAR,
                                        new Variable(t.getName())).negate();

                        OmegaBigDecimal lft = stochasticFeature
                                .getEXPVariables().contains(
                                        new Variable(t.getName()))
                                ? OmegaBigDecimal.POSITIVE_INFINITY
                                : densityFunction.getMinBound(
                                        new Variable(t.getName()),
                                        Variable.TSTAR);

                        l.log("[" + eft + "," + lft + "]");
                    }

                    l.log("\n");
                    l.log(transientFeature.toString().replaceAll("^|(\\n)",
                            "$1" + offset + " | "));
                    l.log("\n");
                    if (s.hasFeature(Regeneration.class)) {
                        l.log(s.getFeature(Regeneration.class).toString()
                                .replaceAll("^|(\\n)", "$1" + offset + " | "));
                        l.log("\n");
                    }
                    l.log(stochasticFeature.toString().replaceAll("^|(\\n)",
                            "$1" + offset + " | "));
                    l.log("\n");
                }

                stack.push(null);
                for (Node m : graph.getSuccessors(n))
                    stack.push(m);

                offset = "  " + offset;
            } else
                offset = offset.substring(2);
        }

        if (l != null) {
            l.log(">> Analysis took "
                    + (System.currentTimeMillis() - startTime) / 1000 + "s\n");
            l.log(String.format(">> Tree: %d classes, %d zones, %d terms\n",
                    treeClasses, treeZones, treeTerms));

        }

        a.alwaysRegenerativeMarkings = new LinkedHashSet<Marking>(
                sometimesRegenerativeMarkings);
        a.alwaysRegenerativeMarkings
                .removeAll(sometimesNotRegenerativeMarkings);

        a.neverRegenerativeMarkings = new LinkedHashSet<Marking>(
                sometimesNotRegenerativeMarkings);
        a.neverRegenerativeMarkings.removeAll(sometimesRegenerativeMarkings);

        a.regenerativeAndNotRegenerativeMarkings = new LinkedHashSet<Marking>(
                sometimesRegenerativeMarkings);
        a.regenerativeAndNotRegenerativeMarkings
                .retainAll(sometimesNotRegenerativeMarkings);

        a.reachableMarkings = new LinkedHashSet<Marking>(
                sometimesRegenerativeMarkings);
        a.reachableMarkings.addAll(sometimesNotRegenerativeMarkings);

        if (l != null) {
            l.log("Always regenerative markings: "
                    + a.alwaysRegenerativeMarkings + "\n");
            l.log("Never regenerative markings: " + a.neverRegenerativeMarkings
                    + "\n");
            l.log("Markings both regenerative and not regenerative: "
                    + a.regenerativeAndNotRegenerativeMarkings + "\n");
        }

        if (monitor != null)
            monitor.notifyMessage("Analysis completed");

        return a;
    }

    private static String formatProbability(BigDecimal prob) {
        return new DecimalFormat("###.##########", new DecimalFormatSymbols(
                new Locale("en", "US"))).format(prob);
    }

    /**
     * Computes transient being probabilities in the interval [0, timeLimit]
     * with the given time step and for any marking satisfying the given
     * MarkingCondition.
     *
     * @param markingCondition
     *            selects markings under analysis
     * @param timeLimit
     *            time limit of the solution
     * @param step
     *            time step for evaluation points
     * @param l analysis logger
     * @return a representation of the solution
     */
    public TransientSolution<Marking, Marking> solveDiscretizedBeingProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, AnalysisLogger l) {

        return solveDiscretized(timeLimit, step, markingCondition,
                MarkingCondition.NONE, false, l, null);
    }

    public TransientSolution<Marking, Marking> solveDiscretizedBeingProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition) {

        return solveDiscretized(timeLimit, step, markingCondition,
                MarkingCondition.NONE, false,
                new PrintStreamLogger(System.out), null);
    }

    public TransientSolution<Marking, Marking> solveDiscretizedBeingProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, AnalysisLogger l,
            AnalysisMonitor m) {

        return solveDiscretized(timeLimit, step, markingCondition,
                MarkingCondition.NONE, false, l, m);
    }

    public TransientSolution<Marking, Marking> solveDiscretizedBeingProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, MarkingCondition stopCondition,
            AnalysisLogger l, AnalysisMonitor m) {

        return solveDiscretized(timeLimit, step, markingCondition,
                stopCondition, false, l, m);
    }

    /**
     * Computes transient visited probabilities in the interval [0, timeLimit]
     * with the given time step and for any marking satisfying the given
     * MarkingCondition.
     *
     * @param markingCondition
     *            selects markings under analysis
     * @param timeLimit
     *            time limit of the solution
     * @param step
     *            time step for evaluation points
     * @param l analysis logger
     * @return a representation of the solution
     */
    public TransientSolution<Marking, Marking> solveDiscretizedVisitedProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, AnalysisLogger l) {

        return solveDiscretized(timeLimit, step, markingCondition,
                MarkingCondition.NONE, true, l, null);
    }

    public TransientSolution<Marking, Marking> solveDiscretizedVisitedProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition) {

        return solveDiscretized(timeLimit, step, markingCondition,
                MarkingCondition.NONE, true, new PrintStreamLogger(System.out),
                null);
    }

    public TransientSolution<Marking, Marking> solveDiscretizedVisitedProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, AnalysisLogger l,
            AnalysisMonitor m) {

        return solveDiscretized(timeLimit, step, markingCondition,
                MarkingCondition.NONE, true, l, m);
    }

    public TransientSolution<Marking, Marking> solveDiscretizedVisitedProbabilities(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, MarkingCondition stopCondition,
            AnalysisLogger l, AnalysisMonitor m) {

        return solveDiscretized(timeLimit, step, markingCondition,
                stopCondition, true, l, m);
    }

    /**
     * Computes transient (being or visited) probabilities in the interval [0,
     * timeLimit] with the given time step and for any marking satisfying the
     * given MarkingCondition.
     *
     * @param markingCondition
     *            selects markings under analysis
     * @param timeLimit
     *            time limit of the solution
     * @param step
     *            time step for evaluation points
     * @param visitedProbabilies
     *            selects visited probabilities instead of being ones
     * @return A representation of the solution
     * @throws Exception
     */
    private TransientSolution<Marking, Marking> solveDiscretized(
            BigDecimal timeLimit, BigDecimal step,
            MarkingCondition markingCondition, MarkingCondition stopCondition,
            boolean visitedProbabilies, AnalysisLogger l,
            AnalysisMonitor monitor) {

        // FIXME Ha senso qui convertire a double per mantenere
        // un output compatibile con l'analisi rigenerativa?

        if (l != null) {
            l.log(">> Solving in [0, " + timeLimit + "] with step " + step
                    + " ");
            if (markingCondition == MarkingCondition.ANY)
                l.log("for any reachable marking\n");
            else
                l.log("for any reachable marking satisfying the specified marking condition\n");
        }

        long startTime = System.currentTimeMillis();

        // The initial marking is the only row
        List<Marking> rowMarkings = new ArrayList<Marking>();
        rowMarkings.add(this.getInitialMarking());

        // Adds a column for any marking satisfying the marking condition
        List<Marking> columnMarkings = new ArrayList<Marking>();
        for (Marking m : this.getReachableMarkings())
            if (markingCondition.evaluate(m))
                columnMarkings.add(m);

        if (l != null) {
            l.log("Row markings: " + rowMarkings + "\n");
            l.log("Column markings: " + columnMarkings + "\n");
        }

        // Builds a representation of the transient solution
        TransientSolution<Marking, Marking> p = new TransientSolution<Marking, Marking>(
                timeLimit, step, rowMarkings, columnMarkings);

        // Computes the solution for each time instant and each marking
        OmegaBigDecimal timeValue = OmegaBigDecimal.ZERO;
        OmegaBigDecimal timeStep = new OmegaBigDecimal(step);
        for (int t = 0; t < p.samplesNumber; ++t) {

            if (l != null)
                l.log(timeValue.toString());

            if (monitor != null)
                monitor.notifyMessage("Computing probabilities at time t="
                        + timeValue);

            for (int j = 0; j < columnMarkings.size(); ++j) {
                if (stateClasses.get(columnMarkings.get(j)) != null)
                    // sums over probabilities of being (having visited) at time
                    // t in any class with marking j
                    for (State s : stateClasses.get(columnMarkings.get(j))) {
                        TransientStochasticStateFeature transientFeature = s
                                .getFeature(TransientStochasticStateFeature.class);
                        StochasticStateFeature stochasticFeature = s
                                .getFeature(StochasticStateFeature.class);
                        p.solution[t][0][j] += (!visitedProbabilies
                                && !stopCondition.evaluate(columnMarkings
                                        .get(j)) ? transientFeature
                                .computeTransientClassProbability(timeValue,
                                        stochasticFeature).doubleValue()
                                : transientFeature.computeVisitedProbability(
                                        OmegaBigDecimal.ZERO, timeValue,
                                        stochasticFeature).doubleValue());

                        if (monitor != null && monitor.interruptRequested()) {
                            monitor.notifyMessage("Aborted");
                            return null;
                        }
                    }

                if (l != null)
                    l.log(" " + p.solution[t][0][j]);
            }

            if (l != null)
                l.log("\n");
            timeValue = timeValue.add(timeStep);
        }

        if (l != null)
            l.log(">> Discretization took "
                    + (System.currentTimeMillis() - startTime) / 1000 + "s\n");

        if (monitor != null)
            monitor.notifyMessage("Computation completed");

        return p;
    }
}
