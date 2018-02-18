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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.state.State;
import org.oristool.models.ValidationMessageCollector;
import org.oristool.models.gspn.CTMCTransientSolution.EvaluationMode;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

public final class GSPNOperator {

    private final BigDecimal timeLimit;
    private final BigDecimal step;
    private final BigDecimal error;
    private final PetriNet petrinet;
    private final Marking initialMarking;
    private final boolean steadyState;
    private final MarkingCondition stopCondition;
    private final AnalysisLogger logger;
    private final AnalysisMonitor monitor;

    private TransientSolution<Marking, Marking> transientS;
    private List<State> tangibleStateList;

    public TransientSolution<Marking, Marking> getTransientS() {
        return transientS;
    }

    public Marking getInitialMarking() {
        return new Marking(initialMarking);
    }

    /**
     * Checks whether this method can analyze the given Petri net.
     *
     * @param petriNet input Petri net
     * @param c collector of validation messages
     * @return true if the Petri net can be analyzed
     */
    public boolean canAnalyze(PetriNet petriNet, ValidationMessageCollector c) {

        boolean canAnalyze = true;
        for (Transition t : petriNet.getTransitions()) {
            if (!t.hasFeature(WeightExpressionFeature.class)
                    && !t.hasFeature(RateExpressionFeature.class)) {
                canAnalyze = false;
                c.addError("Transition '" + t + "' is neither EXP nor IMM");
            }
        }

        return canAnalyze;
    }

    /**
     * Builds a solver for transient and steady-state probabilities of a GSPN.
     *
     * @param timeLimit time limit of transient probabilities
     * @param step step of transient probabilities
     * @param error uniformization error
     * @param petrinet Petri net
     * @param initialMarking initial marking
     * @param steadyState whether to compute steady-state probabilities
     * @param stopCondition stop condition of the analysis
     * @param logger logger
     * @param monitor monitor
     */
    public GSPNOperator(BigDecimal timeLimit, BigDecimal step, BigDecimal error, PetriNet petrinet,
            Marking initialMarking, boolean steadyState, MarkingCondition stopCondition,
            AnalysisLogger logger, AnalysisMonitor monitor) {

        this.timeLimit = timeLimit;
        this.step = step;
        this.error = error;
        this.petrinet = petrinet;
        this.initialMarking = initialMarking;
        this.steadyState = steadyState;
        this.stopCondition = stopCondition;
        this.logger = logger;
        this.monitor = monitor;
    }

    /**
     * Runs the analysis.
     */
    public void run() {
        // 1- Evaluate the graph and prepare analyzer to be used later
        // catching presence of not GSPN transitions(generateGraphs) and presence of
        // Time-Locks(GAnalyzer)
        SuccessionGraph graph = GSPNGraphGenerator.generateGraph(petrinet, initialMarking,
                stopCondition, monitor);
        GSPNGraphAnalyzer analyzer = new GSPNGraphAnalyzer(graph, petrinet);

        // 2- Evaluate the list of tangible states
        this.tangibleStateList = analyzer.getTangiblesStateList();

        // 3- Evaluate transient solution
        this.transientS = generateTransientSolution(graph, analyzer);

        // 4- Evaluate steady state solution if required and not aborted for some reason
        if (transientS != null && steadyState) {
            reportSteadyState(graph, analyzer);
        }
    }

    private TransientSolution<Marking, Marking> generateTransientSolution(SuccessionGraph graph,
            GSPNGraphAnalyzer analyzer) {

        if (logger != null) {
            logger.log(">> Solving in [0, " + timeLimit + "] with step " + step + "\n");
        }

        long startTime = System.currentTimeMillis();

        List<double[]> valuesList = calculateTransient(graph, analyzer);

        // from catching exception or abort
        if (valuesList == null)
            return null;

        // The initial marking is the only row
        List<Marking> rowMarkings = new ArrayList<Marking>();
        rowMarkings.add(this.getInitialMarking());

        if (monitor != null && monitor.interruptRequested()) {
            monitor.notifyMessage("Aborted");
            return null;
        }

        // Adds a column for any marking satisfying the marking condition
        List<Marking> columnMarkings = new ArrayList<Marking>();
        for (State n : tangibleStateList) {
            Marking m = n.getFeature(PetriStateFeature.class).getMarking();

            columnMarkings.add(m);
        }
        if (logger != null) {
            logger.log("Row markings: " + rowMarkings + "\n");
            logger.log("Column markings: " + columnMarkings + "\n");
        }

        // Builds a representation of the transient solution
        TransientSolution<Marking, Marking> p = new TransientSolution<Marking, Marking>(timeLimit,
                step, rowMarkings, columnMarkings);

        for (int t = 0; t < p.getSamplesNumber(); ++t) {

            String timeValue = String.valueOf(t * step.doubleValue());
            if (logger != null)
                logger.log(timeValue);

            for (int j = 0; j < columnMarkings.size(); ++j) {
                p.getSolution()[t][0][j] = valuesList.get(t)[j];

                if (logger != null)
                    logger.log(" " + p.getSolution()[t][0][j]);
            }

            if (logger != null)
                logger.log("\n");
        }

        if (logger != null) {
            logger.log(
                    ">> Analysis took " + (System.currentTimeMillis() - startTime) / 1000 + "s\n");
        }

        return p;
    }

    private void reportSteadyState(SuccessionGraph graph, GSPNGraphAnalyzer analyzer) {

        Map<Node, Double> rootMap = GSPNGraphGenerator.calculateRootAbsorption(analyzer,
                graph.getRoot());
        Map<SuccessionGraph, Double> mapOfGraphs = GSPNGraphGenerator
                .generateReducedGraphs(analyzer, rootMap);

        if (logger != null) {
            monitor.notifyMessage("Calculating Bottom Strongly Connected Components");

            Map<SuccessionGraph, Map<Set<State>, Double>> bSCC = TransientAndSteadyMatrixes
                    .obtainBSCC(mapOfGraphs);
            Map<Set<State>, Double> bSCCprob = getBSCCprob(bSCC, mapOfGraphs);
            Map<Set<State>, double[]> steady = calculateSteady(bSCC);
            monitor.notifyMessage("Finished calculating BSCCs");

            int i = 0;
            int size = steady.keySet().size();

            for (Set<State> set : steady.keySet()) {
                i++;
                logger.log("BSCC " + i + " of " + size + ", probability of absorption: "
                        + bSCCprob.get(set).toString() + "\n");
                logger.log("In this BSCC: \n");
                for (State n : set) {
                    logger.log(n.getFeature(PetriStateFeature.class).getMarking().toString()
                            + "  has a probability of "
                            + steady.get(set)[tangibleStateList.indexOf(n)] + "\n");
                }
            }
        }

    }

    private List<double[]> calculateTransient(SuccessionGraph graph, GSPNGraphAnalyzer analyzer) {

        List<BigDecimal> ticks = new ArrayList<>();
        for (BigDecimal t = BigDecimal.ZERO; t.compareTo(timeLimit) <= 0; t = t.add(step)) {
            ticks.add(t);
        }

        CTMCTransientSolution ctmcTransientSolver = new CTMCTransientSolution(graph, petrinet,
                logger, monitor, EvaluationMode.ADAPTIVE);

        if (monitor != null && monitor.interruptRequested()) {
            return null;
        }

        Map<State, double[]> solution = ctmcTransientSolver.computeTransients(ticks, step, error);

        if (monitor != null && monitor.interruptRequested()) {
            return null;
        }

        List<double[]> transientValues = new LinkedList<double[]>();

        for (int t = 0; t < ticks.size(); t++) {
            transientValues.add(new double[tangibleStateList.size()]);
            for (int s = 0; s < tangibleStateList.size(); s++) {
                transientValues.get(t)[s] = solution.get(tangibleStateList.get(s))[t];
            }

        }

        if (monitor != null)
            monitor.notifyMessage("Computation completed");

        return transientValues;
    }

    // for debug: methods "get" and "contains" in SuccessionGraph
    // (ex.getOutgoingSucc) refer to set.equal that controls the elements in the
    // set
    private Map<Set<State>, double[]> calculateSteady(
            Map<SuccessionGraph, Map<Set<State>, Double>> bscc) {

        Set<Set<State>> discovered = new HashSet<Set<State>>();

        Map<Set<State>, double[]> steadyStateProb = new HashMap<Set<State>, double[]>();

        for (SuccessionGraph graph : bscc.keySet()) {
            for (Set<State> set : bscc.get(graph).keySet()) {
                boolean firstTime;
                if (discovered.contains(set)) {
                    firstTime = false;

                } else {
                    discovered.add(set);
                    firstTime = true;
                }

                if (firstTime == true) {

                    if (set.size() == 1) {
                        double[] arr = new double[tangibleStateList.size()];
                        State s = new State();
                        for (State x : set) {
                            s = x;
                            break;
                        }
                        arr[tangibleStateList.indexOf(s)] = 1.0;
                        steadyStateProb.put(set, arr);

                    } else {

                        LinkedList<State> stateList = new LinkedList<State>();
                        stateList.addAll(set);

                        double[][] matrixQ = TransientAndSteadyMatrixes
                                .createInfinitesimalGeneratorForSteady(stateList, graph, set);
                        double[][] matrixP = TransientAndSteadyMatrixes
                                .createMatrixProbabilityDistribution(matrixQ);

                        double[] steadyP = TransientAndSteadyMatrixes
                                .steadyStateProbability(matrixP, matrixQ);
                        double[] newArray = new double[tangibleStateList.size()];
                        for (int i = 0; i < steadyP.length; i++) {
                            newArray[tangibleStateList.indexOf(stateList.get(i))] = steadyP[i];
                        }
                        steadyStateProb.put(set, newArray);
                    }
                }
            }
        }

        return steadyStateProb;
    }

    private Map<Set<State>, Double> getBSCCprob(Map<SuccessionGraph, Map<Set<State>, Double>> bscc,
            Map<SuccessionGraph, Double> mapOfGraphs) {

        Map<Set<State>, Double> bSCCprob = new HashMap<Set<State>, Double>();
        Set<Set<State>> discovered = new HashSet<Set<State>>();

        for (SuccessionGraph graph : bscc.keySet()) {
            for (Set<State> set : bscc.get(graph).keySet()) {
                double value = bscc.get(graph).get(set).doubleValue()
                        * mapOfGraphs.get(graph).doubleValue();

                if (discovered.contains(set)) {
                    bSCCprob.put(set, bSCCprob.get(set).doubleValue() + value);
                } else {
                    discovered.add(set);
                    bSCCprob.put(set, value);
                }
            }
        }
        return bSCCprob;
    }
}
