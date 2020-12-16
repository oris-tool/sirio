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

package org.oristool.models.stpn.trans;

import java.math.BigDecimal;
import java.math.MathContext;
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
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.PrintStreamLogger;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.state.LocalStop;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.MonitorStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.StateDensityFunction;
import org.oristool.models.ValidationMessageCollector;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.models.stpn.trees.RegenerativeComponentsFactory;
import org.oristool.models.stpn.trees.StochasticStateFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.stpn.trees.TransientStochasticStateFeature;
import org.oristool.models.stpn.trees.TruncationPolicy;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Regenerative analysis of stochastic time Petri nets.
 */
class RegenerativeTransientAnalysis<R> {

    private double[][][] globalKernel;
    private double[][][] localKernel;

    private Set<Marking> reachableMarkings;

    public Set<Marking> getReachableMarkings() {
        return Collections.unmodifiableSet(reachableMarkings);
    }

    private R initialRegeneration;
    private PetriNet petriNet;
    private EnumerationPolicy policy;
    private StopCriterion absorbingCondition;

    public R getInitialRegeneration() {
        return initialRegeneration;
    }

    public PetriNet getPetriNet() {
        return petriNet;
    }

    public EnumerationPolicy getTruncationPolicy() {
        return policy;
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

    private OmegaBigDecimal globalConvergenceLimit;
    private OmegaBigDecimal localConvergenceLimit;
    private Set<R> regenerations;

    public OmegaBigDecimal getGlobalConvergenceLimit() {
        return globalConvergenceLimit;
    }

    public OmegaBigDecimal getLocalConvergenceLimit() {
        return localConvergenceLimit;
    }

    private RegenerativeTransientAnalysis() {
    }

    public boolean canAnalyze(PetriNet petriNet, ValidationMessageCollector c) {

        boolean canAnalyze = true;
        for (Transition t : petriNet.getTransitions()) {
            if (!t.hasFeature(StochasticTransitionFeature.class)) {
                canAnalyze = false;
                c.addError("Transition '" + t + "' is not stochastic");
            }
        }

        return canAnalyze;
    }

    public static <R> RegenerativeTransientAnalysis<R> compute(PetriNet petriNet,
            R initialRegeneration, StateBuilder<R> stateBuilder, SuccessionProcessor postProcessor,
            TruncationPolicy truncationPolicy, boolean truncationLeavesInGlobalKernel,
            boolean markTruncationLeavesAsRegenerative, AnalysisLogger l, AnalysisMonitor monitor) {

        return RegenerativeTransientAnalysis.compute(petriNet, initialRegeneration, stateBuilder,
                postProcessor, truncationPolicy, new AlwaysFalseStopCriterion(),
                truncationLeavesInGlobalKernel, markTruncationLeavesAsRegenerative, l, monitor);
    }

    public static <R> RegenerativeTransientAnalysis<R> compute(PetriNet petriNet,
            R initialRegeneration, StateBuilder<R> stateBuilder, SuccessionProcessor postProcessor,
            TruncationPolicy truncationPolicy, boolean truncationLeavesInGlobalKernel,
            boolean markTruncationLeavesAsRegenerative) {

        return RegenerativeTransientAnalysis.compute(petriNet, initialRegeneration,
                truncationPolicy.getTauAgeLimit().bigDecimalValue(), stateBuilder,
                postProcessor, truncationPolicy, new AlwaysFalseStopCriterion(),
                truncationLeavesInGlobalKernel, markTruncationLeavesAsRegenerative,
                new PrintStreamLogger(System.out), null, false);
    }

    public static <R> RegenerativeTransientAnalysis<R> compute(PetriNet petriNet,
            R initialRegeneration, StateBuilder<R> stateBuilder, SuccessionProcessor postProcessor,
            TruncationPolicy truncationPolicy, StopCriterion absorbingCondition,
            boolean truncationLeavesInGlobalKernel, boolean markTruncationLeavesAsRegenerative,
            AnalysisLogger l, AnalysisMonitor monitor) {

        return RegenerativeTransientAnalysis.compute(petriNet, initialRegeneration,
                truncationPolicy.getTauAgeLimit().bigDecimalValue(), stateBuilder,
                postProcessor, truncationPolicy, absorbingCondition, truncationLeavesInGlobalKernel,
                markTruncationLeavesAsRegenerative, l, monitor, true);
    }

    public static <R> RegenerativeTransientAnalysis<R> compute(PetriNet petriNet,
            R initialRegeneration, BigDecimal timeBound, StateBuilder<R> stateBuilder,
            SuccessionProcessor postProcessor,
            EnumerationPolicy policy, StopCriterion absorbingCondition,
            boolean truncationLeavesInGlobalKernel, boolean markTruncationLeavesAsRegenerative,
            AnalysisLogger l, AnalysisMonitor monitor, boolean verbose) {

        Locale.setDefault(Locale.US);
        RegenerativeTransientAnalysis<R> a = new RegenerativeTransientAnalysis<R>();

        if (l != null) {
            l.log(">> Regenerative analysis starting from " + initialRegeneration
                    + " (policy: " + policy);
            if (absorbingCondition != MarkingCondition.NONE)
                l.log(", absorbingCondition");
            l.log(")\n");
        }

        // global counts and statistics
        int totalClasses = 0;
        int totalZones = 0;
        int totalTerms = 0;
        int totalTrees = 0;
        DescriptiveStatistics treeClassesStats = new DescriptiveStatistics();
        DescriptiveStatistics treeZonesStats = new DescriptiveStatistics();
        DescriptiveStatistics treeTermsStats = new DescriptiveStatistics();
        DescriptiveStatistics treeDepthStats = new DescriptiveStatistics();
        DescriptiveStatistics treeTimeStats = new DescriptiveStatistics();

        long startTime = System.currentTimeMillis();

        a.petriNet = petriNet;
        a.initialRegeneration = initialRegeneration;
        a.policy = policy;
        a.absorbingCondition = absorbingCondition;

        a.localClasses = new HashMap<R, Map<Marking, Set<State>>>();
        a.regenerationClasses = new HashMap<R, Map<R, Set<State>>>();
        a.reachableMarkings = new LinkedHashSet<>();

        // Adds the initialRegeneration to the list of regenerations to start
        // the analysis from
        a.regenerations = new LinkedHashSet<R>();
        a.regenerations.add(initialRegeneration);

        Queue<R> initialRegenerations = new LinkedList<R>();
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
            long currentStartTime = System.currentTimeMillis();

            RegenerativeComponentsFactory f = new RegenerativeComponentsFactory(true, null, null,
                    true, postProcessor, policy, new OmegaBigDecimal(timeBound),
                    absorbingCondition, null, 0, monitor);

            Analyzer<PetriNet, Transition> analyzer = new Analyzer<PetriNet, Transition>(f,
                    petriNet, stateBuilder.build(currentInitialRegeneration));

            SuccessionGraph graph = analyzer.analyze();
            treeTimeStats.addValue(System.currentTimeMillis() - currentStartTime);

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
                    monitor.notifyMessage("Analyzing the transient tree (" + graph.getNodes().size()
                            + " state classes)");
            }

            // Tree counts
            int treeClasses = 0;
            int treeZones = 0;
            int treeTerms = 0;
            int treeDepth = 0;

            // Analyzes the state class tree in a depth first fashion
            Deque<Node> stack = new LinkedList<Node>();
            stack.push(graph.getRoot());
            String offset = "";
            while (!stack.isEmpty()) {

                Node n = stack.pop();
                if (n != null) {
                    State s = graph.getState(n);
                    final PetriStateFeature petriFeature = s.getFeature(PetriStateFeature.class);
                    final StochasticStateFeature stochasticFeature = s
                            .getFeature(StochasticStateFeature.class);
                    final StateDensityFunction densityFunction =
                            stochasticFeature.getStateDensity();
                    final TransientStochasticStateFeature transientFeature = s
                            .getFeature(TransientStochasticStateFeature.class);

                    // Update tree counts
                    treeClasses += 1;
                    treeZones += densityFunction.getPartitionedGen().getFunctions().size();
                    for (GEN g : densityFunction.getPartitionedGen().getFunctions())
                        treeTerms += g.getDensity().getExmonomials().size();
                    treeDepth = Math.max(treeDepth, offset.length() / 2);

                    // Adds this marking to the initial ones to start the
                    // analysis from if it is in a regenerative class
                    if (s.hasFeature(Regeneration.class)) {
                        // (markTruncationLeavesAsRegenerative &&
                        // graph.getSuccessors(n).size()==0)))
                        // TODO fix markTruncationLeavesAsRegenerative by
                        // computing the enablingTimes and
                        // checking that they are
                        // not in analyzedInitialRegenerations

                        @SuppressWarnings("unchecked")
                        // regenerations should be of type R (!)
                        R regeneration = (R) s.getFeature(Regeneration.class).getValue();

                        if (!a.regenerations.contains(regeneration)) {

                            if (regReachedAfterTimeBound(regeneration, initialRegeneration,
                                    new OmegaBigDecimal(timeBound))) {
                                if (l != null)
                                    l.log(">> Skipping regeneration after time bound: "
                                            + regeneration);
                            } else {
                                a.regenerations.add(regeneration);
                                initialRegenerations.add(regeneration);
                            }
                        }
                    }

                    a.reachableMarkings.add(petriFeature.getMarking());

                    String addedTo = "";
                    if (s.hasFeature(Regeneration.class) && graph.getSuccessors(n).size() == 0
                            && !n.equals(graph.getRoot())
                            || (truncationLeavesInGlobalKernel
                                    && graph.getSuccessors(n).size() == 0)) {

                        @SuppressWarnings("unchecked")
                        // regenerations should be of type R (!)
                        R regeneration = (R) s.getFeature(Regeneration.class).getValue();

                        // Any regenerative leaf class other than the root is
                        // added to the entry of
                        // regenerationClasses corresponding to the current
                        // initial and class marking
                        if (!a.regenerationClasses.containsKey(currentInitialRegeneration))
                            a.regenerationClasses.put(currentInitialRegeneration,
                                    new HashMap<R, Set<State>>());

                        if (!a.regenerationClasses.get(currentInitialRegeneration)
                                .containsKey(regeneration))
                            a.regenerationClasses.get(currentInitialRegeneration).put(regeneration,
                                    new LinkedHashSet<State>());

                        a.regenerationClasses.get(currentInitialRegeneration).get(regeneration)
                                .add(s);
                        if (l != null)
                            addedTo = " G(" + currentInitialRegeneration + ", " + regeneration
                                    + ")";

                    } else {
                        // If this state class is not a regenerative leaf (or is
                        // the root node), it is added to
                        // the entry of localClasses corresponding to the
                        // current initial and class marking
                        if (!a.localClasses.containsKey(currentInitialRegeneration))
                            a.localClasses.put(currentInitialRegeneration,
                                    new HashMap<Marking, Set<State>>());

                        if (!a.localClasses.get(currentInitialRegeneration)
                                .containsKey(petriFeature.getMarking()))
                            a.localClasses.get(currentInitialRegeneration)
                                    .put(petriFeature.getMarking(), new LinkedHashSet<State>());

                        a.localClasses.get(currentInitialRegeneration)
                                .get(petriFeature.getMarking()).add(s);
                        if (l != null)
                            addedTo = " L(" + currentInitialRegeneration + ", "
                                    + petriFeature.getMarking() + ")";
                    }

                    if (l != null && verbose) {
                        l.log(offset);
                        if (s.hasFeature(Regeneration.class))
                            l.log("{" + n.id() + "}");
                        else
                            l.log("(" + n.id() + ")");
                        l.log(" " + formatProbability(transientFeature.getReachingProbability()));
                        l.log(" " + formatProbability(
                                transientFeature.computeVisitedProbability(OmegaBigDecimal.ZERO,
                                        new OmegaBigDecimal(timeBound), stochasticFeature)));
                        l.log(" (" + petriFeature.getMarking().toString().trim() + ")");
                        l.log(" [" + transientFeature.getEnteringTimeLowerBound(stochasticFeature)
                                + ","
                                + transientFeature.getEnteringTimeUpperBound(stochasticFeature)
                                + "] ");

                        Set<Variable> exps = stochasticFeature.getEXPVariables();
                        for (Node m : graph.getSuccessors(n)) {
                            Succession succ = graph.getSuccessions(n, m).iterator()
                                    .next();
                            Transition t = (Transition) succ.getEvent();
                            Variable tau = new Variable(t.getName());
                            l.log(" ");
                            if (petriFeature.getNewlyEnabled().contains(t))
                                l.log("~");
                            else if (!stochasticFeature.getEXPVariables().contains(tau))
                                l.log("*");

                            l.log(t.getName());
                            l.log("["
                                    + (exps.contains(tau) ? "0"
                                            : densityFunction.getMinBound(Variable.TSTAR, tau)
                                                    .negate())
                                    + ","
                                    + (exps.contains(tau) ? "0"
                                            : densityFunction.getMinBound(tau, Variable.TSTAR))
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
                            for (Succession succ : graph.getSuccessions(n, m))
                                if (!notFiredEnabledTransitions.contains(succ.getEvent()))
                                    throw new IllegalStateException(
                                            "A not enabled transition fired in the graph");
                                else
                                    notFiredEnabledTransitions.remove(succ.getEvent());

                        for (Transition t : notFiredEnabledTransitions) {
                            final Variable tau = new Variable(t.getName());
                            l.log(" ");
                            if (petriFeature.getNewlyEnabled().contains(t))
                                l.log("~");
                            else if (!stochasticFeature.getEXPVariables()
                                    .contains(new Variable(t.getName())))
                                l.log("*");

                            l.log(t.getName());
                            l.log("["
                                    + (exps.contains(tau) ? "0"
                                            : densityFunction.getMinBound(Variable.TSTAR, tau)
                                                    .negate())
                                    + ","
                                    + (exps.contains(tau) ? "0"
                                            : densityFunction.getMinBound(tau, Variable.TSTAR))
                                    + "]");
                        }

                        l.log(addedTo);
                        l.log("\n");
                        l.log(transientFeature.toString().replaceAll("^|(\\n)",
                                "$1" + offset + " | ") + "\n");
                        l.log(stochasticFeature.toString().replaceAll("^|(\\n)",
                                "$1" + offset + " | ") + "\n");
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

            // Update total counts and stats
            totalClasses += treeClasses;
            totalZones += treeZones;
            totalTerms += treeTerms;
            totalTrees += 1;

            treeClassesStats.addValue(treeClasses);
            treeZonesStats.addValue(treeZones);
            treeTermsStats.addValue(treeTerms);
            treeDepthStats.addValue(treeDepth);
        }

        if (l != null) {
            l.log(">> Regenerative analysis took " + (System.currentTimeMillis() - startTime) / 1000
                    + "s\n");
            l.log(String.format(">> Total: %d classes, %d zones, %d terms, %d trees\n",
                    totalClasses, totalZones, totalTerms, totalTrees));
            l.log(String.format(">> Tree classes: %.0f min, %.0f max, %.2f mean, %.2f variance\n",
                    treeClassesStats.getMin(), treeClassesStats.getMax(),
                    treeClassesStats.getMean(), treeClassesStats.getVariance()));
            l.log(String.format(">> Tree zones: %.0f min, %.0f max, %.2f mean, %.2f variance\n",
                    treeZonesStats.getMin(), treeZonesStats.getMax(), treeZonesStats.getMean(),
                    treeZonesStats.getVariance()));
            l.log(String.format(">> Tree terms: %.0f min, %.0f max, %.2f mean, %.2f variance\n",
                    treeTermsStats.getMin(), treeTermsStats.getMax(), treeTermsStats.getMean(),
                    treeTermsStats.getVariance()));
            l.log(String.format(">> Tree depth: %.0f min, %.0f max, %.2f mean, %.2f variance\n",
                    treeDepthStats.getMin(), treeDepthStats.getMax(), treeDepthStats.getMean(),
                    treeDepthStats.getVariance()));
            l.log(String.format(">> Tree time (ms): %.0f min, %.0f max, %.2f mean, %.2f variance\n",
                    treeTimeStats.getMin(), treeTimeStats.getMax(), treeTimeStats.getMean(),
                    treeTimeStats.getVariance()));
        }


        // The maximum entering time upper bound among any regeneration class
        // is a convergence point for the global kernel
        OmegaBigDecimal maxEnteringTimeUpperBound = OmegaBigDecimal.ZERO;
        for (Map<R, Set<State>> map : a.regenerationClasses.values())
            for (Set<State> set : map.values())
                for (State s : set) {
                    OmegaBigDecimal enteringTimeUpperBound = s
                            .getFeature(TransientStochasticStateFeature.class)
                            .getEnteringTimeUpperBound(s.getFeature(StochasticStateFeature.class));
                    if (enteringTimeUpperBound.compareTo(maxEnteringTimeUpperBound) > 0)
                        maxEnteringTimeUpperBound = enteringTimeUpperBound;
                }

        a.globalConvergenceLimit = maxEnteringTimeUpperBound;
        if (l != null)
            l.log("Global kernel converges after t=" + a.globalConvergenceLimit + "\n");

        // After the maximum time upper bound among any local class, the
        // probability
        // of being in a local class is zero
        OmegaBigDecimal maxTimeUpperBound = OmegaBigDecimal.ZERO;
        for (Map<Marking, Set<State>> map : a.localClasses.values())
            for (Set<State> set : map.values())
                for (State s : set) {
                    OmegaBigDecimal timeUpperBound = s
                            .getFeature(TransientStochasticStateFeature.class)
                            .getTimeUpperBound(s.getFeature(StochasticStateFeature.class));
                    if (timeUpperBound.compareTo(maxTimeUpperBound) > 0)
                        maxTimeUpperBound = timeUpperBound;
                }

        a.localConvergenceLimit = maxTimeUpperBound;
        if (l != null)
            l.log("Local kernel converges after t=" + a.localConvergenceLimit + "\n");

        if (monitor != null)
            monitor.notifyMessage("Analysis completed");

        return a;
    }

    private static <R> boolean regReachedAfterTimeBound(R regeneration, R initialRegeneration,
            OmegaBigDecimal tauAgeLimit) {

        if (!(tauAgeLimit.bigDecimalValue() != null
                && regeneration instanceof DeterministicEnablingState
                && initialRegeneration instanceof DeterministicEnablingState))
            return false;

        BigDecimal timeBound = tauAgeLimit.bigDecimalValue();
        DeterministicEnablingState reached = (DeterministicEnablingState) regeneration;
        DeterministicEnablingState initial = (DeterministicEnablingState) initialRegeneration;

        for (Entry<Variable, BigDecimal> e : reached.getEnablingTimes().entrySet()) {
            BigDecimal elapsedTime = e.getValue();
            BigDecimal initialTime = initial.getEnablingTimes().get(e.getKey());
            if (initialTime != null)
                elapsedTime = elapsedTime.subtract(initialTime);

            if (elapsedTime.compareTo(timeBound) > 0)
                return true;
        }

        return false;
    }

    private static String formatProbability(BigDecimal prob) {
        return new DecimalFormat("###.##########", new DecimalFormatSymbols(new Locale("en", "US")))
                .format(prob);
    }

    public TransientSolution<R, Marking> solveDiscretizedMarkovRenewal(BigDecimal timeLimit,
            BigDecimal step, MarkingCondition markingCondition, boolean normalizeGlobalKernel) {

        return solveDiscretizedMarkovRenewal(timeLimit, step, markingCondition,
                normalizeGlobalKernel, new PrintStreamLogger(System.out), null);
    }

    public TransientSolution<R, Marking> solveDiscretizedMarkovRenewal(BigDecimal timeLimit,
            BigDecimal step, MarkingCondition markingCondition, boolean normalizeGlobalKernel,
            AnalysisLogger logger, AnalysisMonitor monitor) {

        if (logger != null) {
            logger.log(">> Solving in [0, " + timeLimit + "] with step " + step + " ");
            if (markingCondition == MarkingCondition.ANY)
                logger.log("for any reachable marking\n");
            else
                logger.log(
                        "for any reachable marking satisfying the specified marking condition\n");
        }

        long startTime = System.currentTimeMillis();

        // One row for each regeneration and absorbing marking // TODO FIX
        // ABSORBING
        List<R> regenerations = new ArrayList<R>(this.regenerations);

        // Adds a column for any marking satisfying the marking condition
        List<Marking> columnMarkings = new ArrayList<Marking>();
        for (Marking m : this.getReachableMarkings())
            if (markingCondition.evaluate(m))
                columnMarkings.add(m);

        if (logger != null) {
            logger.log("Regenerations: " + regenerations + "\n");
            logger.log("Column markings: " + columnMarkings + "\n");
        }

        int initialRegenerationIndex = regenerations.indexOf(this.getInitialRegeneration());

        // Builds a representation of the transient solution
        TransientSolution<R, Marking> p = new TransientSolution<R, Marking>(timeLimit, step,
                regenerations, columnMarkings, this.getInitialRegeneration());
        if (logger != null)
            logger.log(p.getSamplesNumber() + " solution samples\n");

        // Global kernel representation
        OmegaBigDecimal globalConvergenceLimit = this.getGlobalConvergenceLimit();
        int globalSamplesNumber;
        if (globalConvergenceLimit.compareTo(OmegaBigDecimal.POSITIVE_INFINITY) < 0)
            globalSamplesNumber = Math.min(p.getSamplesNumber(),
                    globalConvergenceLimit.divide(step, MathContext.DECIMAL128).intValue() + 2);
        else
            globalSamplesNumber = p.getSamplesNumber();

        if (logger != null)
            logger.log(globalSamplesNumber + " global kernel samples\n");
        globalKernel = new double[globalSamplesNumber][p.getRegenerations().size()][p
                .getRegenerations().size()];

        // If the solution is computed for a subset of markings,
        // local convergence might be faster
        OmegaBigDecimal markingConditionLocalConvergenceLimit = this.getLocalConvergenceLimit();
        if (markingCondition != MarkingCondition.ANY) {
            // After the maximum time upper bound among local classes satisfying
            // the condition,
            // the probability of being in such a class is zero
            OmegaBigDecimal maxTimeUpperBound = new OmegaBigDecimal(0);
            for (R i : regenerations)
                if (localClasses.containsKey(i))
                    for (Marking j : columnMarkings)
                        if (localClasses.get(i).containsKey(j))
                            for (State s : localClasses.get(i).get(j)) {
                                OmegaBigDecimal timeUpperBound = s
                                        .getFeature(TransientStochasticStateFeature.class)
                                        .getEnteringTimeUpperBound(
                                                s.getFeature(StochasticStateFeature.class));
                                if (timeUpperBound.compareTo(maxTimeUpperBound) > 0)
                                    maxTimeUpperBound = timeUpperBound;
                            }

            markingConditionLocalConvergenceLimit = maxTimeUpperBound;
            if (logger != null)
                logger.log("For markings satisfying the marking condition, "
                        + "local kernel converges after t="
                        + markingConditionLocalConvergenceLimit + "\n");
        }

        // Local kernel representation
        int localSamplesNumber;
        if (markingConditionLocalConvergenceLimit.compareTo(OmegaBigDecimal.POSITIVE_INFINITY) < 0)
            localSamplesNumber = Math.min(p.getSamplesNumber(),
                    markingConditionLocalConvergenceLimit.divide(step, MathContext.DECIMAL128)
                            .intValue() + 2);
        else
            localSamplesNumber = p.getSamplesNumber();

        localKernel = new double[localSamplesNumber][p.getRegenerations().size()][p
                .getColumnStates().size()];

        if (logger != null) {
            logger.log(localSamplesNumber + " local kernel samples\n");

            for (int i = 0; i < regenerations.size(); ++i) {
                for (int j = 0; j < regenerations.size(); ++j)
                    if (regenerationClasses.get(regenerations.get(i)) != null && regenerationClasses
                            .get(regenerations.get(i)).get(regenerations.get(j)) != null)
                        logger.log(regenerationClasses.get(regenerations.get(i))
                                .get(regenerations.get(j)).size() + " ");
                    else
                        logger.log("0 ");
                logger.log("\n");
            }
            for (int i = 0; i < regenerations.size(); ++i) {
                for (int j = 0; j < columnMarkings.size(); ++j)
                    if (localClasses.get(regenerations.get(i)) != null && localClasses
                            .get(regenerations.get(i)).get(columnMarkings.get(j)) != null)
                        logger.log(localClasses.get(regenerations.get(i)).get(columnMarkings.get(j))
                                .size() + " ");
                    else
                        logger.log("0 ");
                logger.log("\n");
            }
        }

        // Discretizes the global and local kernel (one row at a time) for each
        // time instant before
        // convergence
        BigDecimal timeValue = BigDecimal.ZERO;
        for (int t = 0; t < (globalKernel.length > localKernel.length
                ? globalKernel.length : localKernel.length); ++t) {
            if (monitor != null)
                monitor.notifyMessage("Evaluating kernels at time t=" + timeValue);

            for (int i = 0; i < regenerations.size(); ++i) {
                // Computes the i-th global kernel row at time t (if convergence
                // has not been reached)
                if (t < localKernel.length) {
                    if (localClasses.get(regenerations.get(i)) != null)
                        // Local kernel is regenerations.size() x
                        // columnMarkings.size()
                        for (int j = 0; j < columnMarkings.size(); ++j)
                            if (localClasses.get(regenerations.get(i))
                                    .get(columnMarkings.get(j)) != null) {
                                // sums over probabilities of being at time t in
                                // a class with marking j
                                // starting the analysis from marking i and not
                                // having reached a regeneration
                                for (State s : localClasses.get(regenerations.get(i))
                                        .get(columnMarkings.get(j))) {
                                    if (s.hasFeature(LocalStop.class))
                                        localKernel[t][i][j] += s
                                                .getFeature(TransientStochasticStateFeature.class)
                                                .computeVisitedProbability(OmegaBigDecimal.ZERO,
                                                        new OmegaBigDecimal(timeValue),
                                                        s.getFeature(StochasticStateFeature.class))
                                                .doubleValue();
                                    else
                                        localKernel[t][i][j] += s
                                                .getFeature(TransientStochasticStateFeature.class)
                                                .computeTransientClassProbability(
                                                        new OmegaBigDecimal(timeValue),
                                                        s.getFeature(StochasticStateFeature.class))
                                                .doubleValue();

                                    if (monitor != null && monitor.interruptRequested()) {
                                        monitor.notifyMessage("Aborted");
                                        return null;
                                    }
                                }
                            }
                }

                // Computes the i-th global kernel row at time t (if convergence
                // has not been reached)
                if (t < globalKernel.length) {
                    if (regenerationClasses.get(regenerations.get(i)) != null)
                        // Global kernel is regenerations.size() x
                        // regenerations.size()
                        for (int j = 0; j < regenerations.size(); ++j)
                            if (regenerationClasses.get(regenerations.get(i))
                                    .get(regenerations.get(j)) != null)
                                // sums over probabilities of having visited at
                                // time t a regeneration class
                                // with marking j starting the analysis from
                                // initial marking i
                                for (State s : regenerationClasses.get(regenerations.get(i))
                                        .get(regenerations.get(j))) {
                                    globalKernel[t][i][j] += s
                                            .getFeature(TransientStochasticStateFeature.class)
                                            .computeVisitedProbability(OmegaBigDecimal.ZERO,
                                                    new OmegaBigDecimal(timeValue),
                                                    s.getFeature(StochasticStateFeature.class))
                                            .doubleValue();

                                    if (monitor != null && monitor.interruptRequested()) {
                                        monitor.notifyMessage("Aborted");
                                        return null;
                                    }
                                }

                    if (normalizeGlobalKernel) {
                        // normalizes as G[i,j] = G[i,j]*(1-sum_k L[i,k])/(sum_k
                        // G[i,k])

                        double globalRowSum = 0;
                        for (int j = 0; j < regenerations.size(); ++j)
                            globalRowSum += globalKernel[t][i][j];

                        double localRowSum = 0;
                        for (int j = 0; j < columnMarkings.size(); ++j)
                            localRowSum += localKernel[t < localKernel.length ? t
                                    : localKernel.length - 1][i][j];

                        if (globalRowSum > 0.000000001)
                            for (int j = 0; j < regenerations.size(); ++j)
                                globalKernel[t][i][j] = globalKernel[t][i][j] * (1 - localRowSum)
                                        / globalRowSum;

                    }
                }
            }

            timeValue = timeValue.add(step);
        }

        if (logger != null) {
            logger.log(">> Discretization took " + (System.currentTimeMillis() - startTime) / 1000
                    + "s\n");
        }

        startTime = System.currentTimeMillis();

        // Solves the Markov Renewal Equation employing the trapezoidal rule
        if (monitor != null)
            monitor.notifyMessage("Solving the system of Markov renewal equations");

        timeValue = BigDecimal.ZERO;
        for (int t = 0; t < p.getSamplesNumber(); ++t) {
            for (int i = 0; i < regenerations.size(); ++i) {
                for (int j = 0; j < columnMarkings.size(); ++j) {
                    // adds the local kernel value at time t or at convergence
                    p.getSolution()[t][i][j] = localKernel[t < localKernel.length ? t
                            : localKernel.length - 1][i][j];

                    // convolution truncated after global kernel convergence
                    for (int u = 1; u <= (t < globalKernel.length ? t
                            : globalKernel.length - 1); ++u)
                        for (int k = 0; k < regenerations.size(); ++k) {
                            p.getSolution()[t][i][j] += (globalKernel[u][i][k]
                                    - globalKernel[u - 1][i][k]) * p.getSolution()[t - u][k][j];
                        }
                }
            }

            if (logger != null) {
                logger.log(timeValue.toString());
                for (int j = 0; j < columnMarkings.size(); ++j)
                    logger.log(" " + p.getSolution()[t][initialRegenerationIndex][j]);
                logger.log("\n");
            }

            if (monitor != null && monitor.interruptRequested()) {
                monitor.notifyMessage("Aborted");
                return null;
            }

            timeValue = timeValue.add(step);
        }

        if (logger != null)
            logger.log(">> Markov renewal equation solved in "
                    + (System.currentTimeMillis() - startTime) / 1000 + "s\n");

        if (monitor != null)
            monitor.notifyMessage("Computation completed");

        return p;
    }
}
