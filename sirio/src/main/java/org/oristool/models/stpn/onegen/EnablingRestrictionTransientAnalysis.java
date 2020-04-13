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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.policy.FIFOPolicy;
import org.oristool.analyzer.state.LocalStop;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateFeature;
import org.oristool.analyzer.stop.OrStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.onegen.KernelRow.KernelRowEvaluator;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.models.stpn.trees.RegenerativeStopCriterion;
import org.oristool.models.tpn.TimedAnalysis;
import org.oristool.models.tpn.TimedComponentsFactory;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

class EnablingRestrictionTransientAnalysis {

    class KernelEvaluator {
        private final Map<State, KernelRowEvaluator> rowEvaluators;
        private final int evaluationLength;

        private KernelEvaluator(Ticks ticks) {
            rowEvaluators = new HashMap<>();
            this.evaluationLength = ticks.getNumKernelTicks();
        }

        public double[] evaluateLocalKernel(State source, State destination) {
            double[] evaluation = new double[evaluationLength];
            if (rowEvaluators.containsKey(source)) {
                evaluation = rowEvaluators.get(source).evaluateLocalKernel(destination);
                if (evaluation.length != evaluationLength) {
                    throw new IllegalStateException("Evaluation length mismatch");
                }
            }

            return evaluation;
        }

        public double[] evaluateGlobalKernel(State source, State destination) {
            double[] evaluation = new double[evaluationLength];

            if (rowEvaluators.containsKey(source)) {
                evaluation = rowEvaluators.get(source).evaluateGlobalKernel(destination);
                if (evaluation.length != evaluationLength) {
                    throw new IllegalStateException("Evaluation length mismatch");
                }
            }

            return evaluation;
        }

        public Set<State> getRegenerativeStates() {
            return Collections.unmodifiableSet(rowEvaluators.keySet());
        }
    }

    private final PetriNet petriNet;
    private final Marking initialMarking;
    private final SuccessionGraph successionGraph;
    private final VanishingStateAnalyzer analyzer;
    private final Map<State, KernelRow> kernel;
    private final AnalysisLogger log;
    private final AnalysisMonitor monitor;

    private double[][][] globalKernel;
    private double[][][] localKernel;

    private List<State> globalStates;
    private List<State> localStates;
    private List<Marking> markings;

    /**
     * Configures a class for transient analysis under enabling restriction.
     *
     * @param petriNet Petri net
     * @param initialMarking initial marking
     * @param stopCondition stop condition
     * @param log logger
     * @param monitor monitor to stop the analysis
     */
    public EnablingRestrictionTransientAnalysis(PetriNet petriNet, Marking initialMarking,
            StopCriterion stopCondition, AnalysisLogger log, AnalysisMonitor monitor) {

        this.petriNet = petriNet;
        this.initialMarking = initialMarking;
        this.log = log;
        this.monitor = monitor;

        if (monitor != null)
            monitor.notifyMessage("Computing transient analysis under enabling restriction...");

        this.successionGraph = buildSuccessionGraph(initialMarking, stopCondition);
        this.analyzer = new VanishingStateAnalyzer(successionGraph);

        this.kernel = new HashMap<>();
        for (State state : successionGraph.getStates()) {
            if (monitor != null && monitor.interruptRequested()) {
                monitor.notifyMessage("Interrupted\n");
                break;
            }
            if (state.hasFeature(Regeneration.class)) {
                kernel.put(state, buildKernelRow(state, stopCondition));
            }
        }
    }

    private KernelEvaluator getKernelEvaluator(Ticks ticks, BigDecimal step, BigDecimal error) {

        if (monitor != null)
            monitor.notifyMessage("Evaluating kernel formulas...");

        KernelEvaluator evaluator = new KernelEvaluator(ticks);
        for (Entry<State, KernelRow> e : kernel.entrySet()) {
            if (monitor != null && monitor.interruptRequested()) {
                if (log != null)
                    log.log("Interrupted");
                break;
            }

            State state = e.getKey();
            KernelRow kernelRow = e.getValue();
            evaluator.rowEvaluators.put(state, kernelRow.getEvaluator(ticks, step, error));
        }

        return evaluator;
    }

    /**
     * Runs the analysis.
     *
     * @param error allowed error during uniformization
     * @param step time step
     * @param timeLimit time limit
     * @return a transient solution
     */
    public TransientSolution<DeterministicEnablingState, Marking> computeSolution(BigDecimal error,
            BigDecimal step, BigDecimal timeLimit) {

        int ticksCount = timeLimit.divide(step, MathContext.DECIMAL128).intValue() + 1;
        Ticks ticks = new Ticks(timeLimit, ticksCount, 1000);
        KernelEvaluator kernelEvaluator = this.getKernelEvaluator(ticks, step, error);

        globalStates = new ArrayList<>(kernelEvaluator.getRegenerativeStates());
        localStates = new ArrayList<>(successionGraph.getStates());
        markings = new ArrayList<>();

        // Marking aggregation
        for (int j = 0; j < localStates.size(); j++) {
            int firstJMarking = j;
            // searching for a duplicate
            // if a state with the same marking has already been found
            // it should aggregate the probabilities of the two states with
            // the same marking
            for (int j2 = 0; j2 < j; j2++) {
                if (localStates.get(j2).getFeature(PetriStateFeature.class).getMarking().equals(
                        localStates.get(j).getFeature(PetriStateFeature.class).getMarking())) {
                    firstJMarking = j2;
                    break;
                }
            }
            if (firstJMarking == j) {
                markings.add(localStates.get(j).getFeature(PetriStateFeature.class).getMarking());
            }
        }

        int n = globalStates.size();
        int m = markings.size();

        this.globalKernel = new double[ticksCount][n][n];
        this.localKernel = new double[ticksCount][n][m];

        if (monitor != null)
            monitor.notifyMessage("Evaluating local and global kernel...");

        if (monitor != null && monitor.interruptRequested())
            return null;

        List<DeterministicEnablingState> regenerations =
                new ArrayList<DeterministicEnablingState>();

        // local and global kernel creation
        for (int i = 0; i < globalStates.size(); i++) {

            // local kernel
            for (int j = 0; j < localStates.size(); j++) {
                double[] lij = kernelEvaluator.evaluateLocalKernel(globalStates.get(i),
                        localStates.get(j));
                int index = -1;
                for (int mIndex = 0; mIndex < markings.size(); mIndex++) {
                    if (markings.get(mIndex).equals(
                            localStates.get(j).getFeature(PetriStateFeature.class).getMarking())) {
                        index = mIndex;
                        break;
                    }
                }
                if (index == -1)
                    throw new IllegalStateException(
                            "Some problems are present in the local state aggregation routine!");

                for (int t = 0; t < ticksCount; t++) {
                    localKernel[t][i][index] += lij[t];
                }
            }

            // global kernel
            for (int k = 0; k < globalStates.size(); k++) {
                double[] gik = kernelEvaluator.evaluateGlobalKernel(globalStates.get(i),
                        globalStates.get(k));
                for (int t = 0; t < ticksCount; t++) {
                    globalKernel[t][i][k] = gik[t];
                }
            }

            // creates a list of regenerations
            regenerations.add((DeterministicEnablingState) globalStates.get(i)
                    .getFeature(Regeneration.class).getValue());
        }

        long startTime = System.currentTimeMillis();

        // Solves the Markov Renewal Equation employing the trapezoidal rule
        if (monitor != null)
            monitor.notifyMessage("Solving the system of Markov renewal equations...");

        TransientSolution<DeterministicEnablingState, Marking> p =
                new TransientSolution<DeterministicEnablingState, Marking>(
                    timeLimit, step, regenerations, markings,
                    new DeterministicEnablingState(initialMarking, petriNet));

        BigDecimal timeValue = BigDecimal.ZERO;
        for (int t = 0; t < p.getSamplesNumber(); ++t) {
            double[][][] solution = p.getSolution();
            for (int i = 0; i < regenerations.size(); ++i) {
                for (int j = 0; j < markings.size(); ++j) {
                    solution[t][i][j] = localKernel[t][i][j];
                    for (int u = 1; u <= t; ++u)
                        for (int k = 0; k < regenerations.size(); ++k) {
                            solution[t][i][j] += (globalKernel[u][i][k] - globalKernel[u - 1][i][k])
                                    * solution[t - u][k][j];
                        }
                }
            }

            if (log != null) {
                log.log(timeValue.toString());
                for (int j = 0; j < markings.size(); ++j)
                    log.log(" " + solution[t][0][j]);
                log.log("\n");
            }

            if (monitor != null && monitor.interruptRequested()) {
                monitor.notifyMessage("Aborted");
                return null;
            }

            timeValue = timeValue.add(step);
        }

        if (log != null)
            log.log(">> Markov renewal equation solved in "
                    + (System.currentTimeMillis() - startTime) / 1000 + "s\n");

        if (monitor != null)
            monitor.notifyMessage("Computation completed.");

        return p;
    }

    private KernelRow buildKernelRow(State rowState, StopCriterion stop) {
        KernelRow row = new KernelRow();

        SuccessionGraph firstEpochChain = buildFirstEpochChain(rowState, stop);

        SubordinatedCtmc subordinatedCtmc = buildSubordinatedCtmc(firstEpochChain);
        row.setPdf(subordinatedCtmc.getSubordinatingGenPdf());

        DTMC<OneGenState> ctmc = buildCTMC(subordinatedCtmc.getGraph(), petriNet);
        row.setCTMC(ctmc);

        row.setLocalKernelEntries(
                computeLocalKernelEntries(subordinatedCtmc.getGraph(),
                subordinatedCtmc.getRootRateSum()));

        row.setGlobalKernelEntries(
                computeGlobalKernelEntries(firstEpochChain, subordinatedCtmc.getRootSink()));

        return row;
    }

    private DTMC<OneGenState> buildCTMC(SuccessionGraph successionGraph, PetriNet petriNet) {

        DTMC<OneGenState> dtmc = DTMC.create();

        for (Succession succ : successionGraph.getSuccessions()) {
            State parent = succ.getParent();
            State child = succ.getChild();
            OneGenState i = new OneGenState(parent);
            OneGenState j = new OneGenState(child);
            Set<Transition> enabled = parent.getFeature(PetriStateFeature.class).getEnabled();
            Transition fired = (Transition) succ.getEvent();
            double rate = OneGenState.rate(i.state(), fired);
            double exitRate = OneGenState.exitRate(i.state(), enabled);
            dtmc.probsGraph().putEdgeValue(i, j, rate / exitRate);
        }

        State rootState = successionGraph.getState(successionGraph.getRoot());
        OneGenState initialState = new OneGenState(rootState);
        dtmc.probsGraph().addNode(initialState);
        dtmc.initialStates().add(initialState);
        dtmc.initialProbs().add(1.0);
        return dtmc;
    }

    private SuccessionGraph buildSuccessionGraph(Marking initialMarking,
            StopCriterion stopCondition) {

        TimedAnalysis analysis = TimedAnalysis.builder()
                .excludeZeroProb(true)
                .markRegenerations(true)
                .stopOn(() -> stopCondition)
                .build();

        return analysis.compute(this.petriNet, initialMarking)
                .modifyStates(state -> removeLocalStop(state));
    }

    private SuccessionGraph buildFirstEpochChain(State initialState, StopCriterion stop) {
        boolean extended = true;
        StopCriterion combinedStop = new OrStopCriterion(stop, new RegenerativeStopCriterion());
        Marking initialMarking = initialState.getFeature(PetriStateFeature.class).getMarking();
        TimedComponentsFactory f = new TimedComponentsFactory(false, false, true, true, extended,
                new FIFOPolicy(), combinedStop, null, null, null);

        Analyzer<PetriNet, Transition> analyzer = new Analyzer<PetriNet, Transition>(f, petriNet,
                f.buildInitialState(petriNet, initialMarking));

        return analyzer.analyze().modifyStates(state -> removeLocalStop(state));
    }

    private SubordinatedCtmc buildSubordinatedCtmc(SuccessionGraph firstEpochChain) {
        SubordinatedCtmc subordinatedCTMC = new SubordinatedCtmc(
                firstEpochChain.getState(firstEpochChain.getRoot()));

        Deque<Node> frontier = Utils.newQueue(firstEpochChain.getRoot());
        while (!frontier.isEmpty()) {
            Node currNode = frontier.pop();

            for (Succession s : firstEpochChain.getOutgoingSuccessions(currNode)) {
                Transition t = (Transition) s.getEvent();

                if (Utils.isGeneral(t)) {
                    subordinatedCTMC.setSubordinatingGen(t);
                } else {
                    boolean unvisitedSuccessor = subordinatedCTMC.addSuccession(s);

                    if (unvisitedSuccessor) {
                        frontier.push(firstEpochChain.getNode(s.getChild()));
                    }
                }
            }
        }

        if (subordinatedCTMC.getGraph().getStates().size() == 0) {
            throw new IllegalStateException("Subordinated CTMCs must contain at least one state");
        }
        return subordinatedCTMC;
    }

    private Map<State, KernelFormula> computeLocalKernelEntries(SuccessionGraph subCtmc,
            BigDecimal rootStateRateSum) {
        Map<State, KernelFormula> localKernelEntries = new HashMap<>();

        State root = subCtmc.getState(subCtmc.getRoot());
        localKernelEntries.put(root, new SojournTimeFormula(rootStateRateSum.doubleValue()));

        for (State state : subCtmc.getStates()) {
            if (!state.hasFeature(Regeneration.class) && !analyzer.isVanishing(state)) {
                localKernelEntries.put(state, new NonFiringGenFormula(state));
            }
        }
        return localKernelEntries;
    }

    private Map<State, KernelFormula> computeGlobalKernelEntries(SuccessionGraph firstEpochChain,
            State rootSink) {
        Map<State, KernelFormula> globalKernelEntries = new HashMap<>();
        Map<State, List<KernelFormula>> globalKernelTerms = new HashMap<>();

        State rootState = firstEpochChain.getState(firstEpochChain.getRoot());
        Set<State> preemptiveExpRegStates = new HashSet<>();
        Set<Node> visitedNodes = Utils.newSet(firstEpochChain.getRoot());
        Deque<Node> frontier = Utils.newQueue(firstEpochChain.getRoot());
        while (!frontier.isEmpty()) {
            Node currNode = frontier.pop();

            Set<Succession> currSucc = firstEpochChain.getOutgoingSuccessions(currNode);
            for (Succession s : currSucc) {
                State childState = s.getChild();
                Transition t = (Transition) s.getEvent();

                if (Utils.isGeneral(t)) {
                    Map<State, Double> absorbtionStateProbs;
                    if (analyzer.isVanishing(childState)) {
                        absorbtionStateProbs = analyzer.calculateAbsorptionVector(childState);

                    } else {
                        absorbtionStateProbs = new HashMap<>();
                        absorbtionStateProbs.put(childState, 1.0);
                    }

                    State currState = firstEpochChain.getState(currNode);
                    for (State dest : absorbtionStateProbs.keySet()) {
                        globalKernelTerms.putIfAbsent(dest, new ArrayList<>());
                        globalKernelTerms.get(dest).add(
                                new FiringGenFormula(currState, absorbtionStateProbs.get(dest)));
                    }
                } else {
                    if (childState.hasFeature(Regeneration.class)) {

                        if (!preemptiveExpRegStates.contains(childState)) {
                            preemptiveExpRegStates.add(childState);

                            State transientLookupState = childState.equals(rootState) ? rootSink
                                    : childState;

                            globalKernelTerms.putIfAbsent(childState, new ArrayList<>());
                            globalKernelTerms.get(childState)
                                    .add(new NonFiringGenFormula(transientLookupState));
                            globalKernelTerms.get(childState)
                                    .add(new FiringGenFormula(transientLookupState));
                        }
                    } else {
                        Node child = firstEpochChain.getNode(childState);
                        if (!visitedNodes.contains(child)) {
                            frontier.push(child);
                            visitedNodes.add(child);
                        }
                    }
                }
            }
        }

        for (Entry<State, List<KernelFormula>> e : globalKernelTerms.entrySet()) {
            List<KernelFormula> terms = e.getValue();
            globalKernelEntries.put(e.getKey(), new CompositeFormula(terms));
        }

        return globalKernelEntries;
    }

    private static State removeLocalStop(State state) {
        if (state == null)
            return null;

        State newState = new State();
        for (StateFeature f : state.getFeatures()) {
            if (!(f instanceof LocalStop))
                newState.addFeature(f);
        }

        return newState;
    }
}