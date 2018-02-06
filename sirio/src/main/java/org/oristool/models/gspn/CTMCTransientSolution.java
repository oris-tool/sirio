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
import java.util.List;
import java.util.Map;

import org.oristool.analyzer.graph.Node;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.state.State;
import org.oristool.petrinet.PetriNet;
import org.oristool.util.Pair;

/**
 * Solver for CTMC transient probabilities of a GSPN.
 */
public class CTMCTransientSolution {
    private final double[][] uniformizedQ;
    private final double gamma;
    private final List<State> tangibleStates;
    private double[] rootList;
    private EvaluationMode mode;
    private AnalysisLogger log;
    private AnalysisMonitor monitor;
    private List<Pair<Integer, Integer>> failedIntervals;
    private static final int MAX_ITERATIONS = 10;
    private static final BigDecimal UPPER_ERROR_BOUND = new BigDecimal("0.2");
    private boolean singleStateCTMC = false;

    public enum EvaluationMode {
        ZEROS, ADAPTIVE, LINEARIZE
    }

    /**
     * Analyzes the state space of a GSPN to prepare a solver for CTMC transient
     * probabilities.
     *
     * @param ctmc Input CTMC
     * @param pn Input GSPN
     * @param log analysis logger
     * @param monitor analysis monitor
     * @param mode evaluation mode for Poisson probabilities
     */
    public CTMCTransientSolution(SuccessionGraph ctmc, PetriNet pn, AnalysisLogger log,
            AnalysisMonitor monitor, EvaluationMode mode) {
        this.mode = mode;
        this.log = log;
        this.monitor = monitor;

        if (ctmc.getStates().size() == 1) {
            singleStateCTMC = true;
            uniformizedQ = null;
            gamma = 0;
            tangibleStates = new ArrayList<>(ctmc.getStates());
            rootList = new double[]{1.0};
            return;
        }

        GSPNGraphAnalyzer analyzer = new GSPNGraphAnalyzer(ctmc, pn);
        this.tangibleStates = analyzer.getTangiblesStateList();
        this.failedIntervals = new ArrayList<Pair<Integer, Integer>>();

        Map<Node, Double> rootMap = GSPNGraphGenerator.calculateRootAbsorption(analyzer,
                ctmc.getRoot());
        if (rootMap.size() != 1) {
            throw new IllegalStateException("Invalid root map");
        }

        Map<SuccessionGraph, Double> mapOfGraphs = GSPNGraphGenerator
                .generateReducedGraphs(analyzer, rootMap);
        if (mapOfGraphs.size() != 1) {
            throw new IllegalStateException("Invalid map of graphs");
        }

        double[][] matrixQ = TransientAndSteadyMatrixes
                .createInfinitesimalGeneratorForTransient(tangibleStates, mapOfGraphs);

        this.gamma = TransientAndSteadyMatrixes.findLambda(matrixQ);

        this.uniformizedQ = TransientAndSteadyMatrixes.createMatrixUniformized(matrixQ, gamma);

        this.rootList = new double[matrixQ.length];
        for (Node n : rootMap.keySet()) {
            this.rootList[analyzer.getTangiblesList().indexOf(n)] = rootMap.get(n).doubleValue();
        }
    }

    /**
     * Computes transient probabilities for the given set of time points.
     *
     * @param ticks time points
     * @param step time step
     * @param error allowed error
     * @return transient probabilities for each state
     */
    public Map<State, double[]> computeTransients(List<BigDecimal> ticks, BigDecimal step,
            BigDecimal error) {

        if (log != null) {
            log.log("Starting CTMC transient analysis in [" + ticks.get(0).doubleValue() + ","
                    + ticks.get(ticks.size() - 1) + "]\n");
        }

        Map<State, double[]> transientValues = new HashMap<State, double[]>();

        if (singleStateCTMC) {
            double[] result = new double[ticks.size()];
            for (int tickIndex = 0; tickIndex < ticks.size(); tickIndex++) {
                result[tickIndex] = 1;
            }

            transientValues.put(tangibleStates.get(0), result);
            if (log != null) {
                log.log("CTMC transient analysis done\n");
            }

            return transientValues;
        }

        for (State state : tangibleStates) {
            transientValues.put(state, new double[ticks.size()]);
        }

        Solver solution =
                new Solver(error.doubleValue(), uniformizedQ, rootList, log);

        for (int tickIndex = 0; tickIndex < ticks.size(); tickIndex++) {

            BigDecimal tick = ticks.get(tickIndex);
            double lambda = gamma * tick.doubleValue();
            double[] result;

            if (tick.remainder(step).doubleValue() == 0) {
                if (log != null) {
                    log.log("Computing CTMC transient solution for t=" + tick.doubleValue() + "\n");
                }

                if (monitor != null) {
                    if (monitor.interruptRequested()) {
                        monitor.notifyMessage("Aborted");
                        return null;
                    }
                    monitor.notifyMessage("CTMC transient solution for t=" + tick.doubleValue());
                }
            }

            // Fox&Glynn method assumes lambda is not zero.
            // For lambda=0 we take the initial probability distribution
            if (lambda == 0) {
                result = rootList;
            } else {
                result = solution.computeCtmcTransientProbabilities(lambda, log);
                if (result == null) {
                    switch (mode) {
                        case ZEROS:
                        case LINEARIZE:
                            result = getZerosResult(tickIndex);
                            break;
                        case ADAPTIVE:
                            result = adaptiveUniformization(solution, lambda, error, tickIndex);
                            break;
                        default:
                            break;
                    }
                }
            }

            for (int i = 0; i < result.length; i++) {
                transientValues.get(tangibleStates.get(i))[tickIndex] = result[i];
            }
        }

        if (mode == EvaluationMode.LINEARIZE || mode == EvaluationMode.ADAPTIVE) {
            linearizeResults(ticks, transientValues);
        }

        if (log != null) {
            log.log("CTMC transient analysis done\n");
        }

        return transientValues;
    }

    private double[] getZerosResult(int tickIndex) {
        if (failedIntervals.isEmpty()) {
            failedIntervals.add(Pair.of(tickIndex, tickIndex));
        } else {
            Pair<Integer, Integer> lastInterval = failedIntervals.get(failedIntervals.size() - 1);
            if (tickIndex == lastInterval.second() + 1) {
                failedIntervals.remove(lastInterval);
                failedIntervals.add(Pair.of(lastInterval.first(), tickIndex));
            } else {
                failedIntervals.add(Pair.of(tickIndex, tickIndex));
            }
        }
        double[] result = new double[tangibleStates.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = 0;
        return result;
    }

    private void linearizeResults(List<BigDecimal> ticks, Map<State, double[]> transientValues) {
        int lastIndex = ticks.size() - 1;
        for (Pair<Integer, Integer> failedInterval : failedIntervals) {
            int first = failedInterval.first();
            int second = failedInterval.second();
            if (log != null) {
                log.log("Linearizing in [" + ticks.get(first) + "," + ticks.get(second) + "]\n");
            }
            double[] steps;
            if (second == lastIndex) { // if the interval is at the end
                // if only the first (t=0) is known, keep constant
                // otherwise, maintain the trend
                if (first == 1) {
                    steps = new double[tangibleStates.size()];
                    for (int i = 0; i < steps.length; i++) {
                        steps[i] = 0;
                    }
                } else {
                    steps = computeLinearizationSteps(first - 2, first - 1, transientValues);
                }
            } else { // linearize otherwise
                steps = computeLinearizationSteps(first - 1, second + 1, transientValues);
            }
            for (int i = 0; i < tangibleStates.size(); i++) {
                State state = tangibleStates.get(i);
                double[] transients = transientValues.get(state);
                for (int t = first; t <= second; t++) {
                    transients[t] = Math.min(Math.max(0, transients[t - 1] + steps[i]), 1);
                }
            }
        }
    }

    private double[] computeLinearizationSteps(int first, int second,
            Map<State, double[]> transientValues) {
        double[] steps = new double[tangibleStates.size()];
        int n = second - first;
        for (int i = 0; i < tangibleStates.size(); i++) {
            State state = tangibleStates.get(i);
            double[] transients = transientValues.get(state);
            steps[i] = (transients[second] - transients[first]) / n;
        }
        return steps;
    }

    private double[] adaptiveUniformization(Solver foxGlynnSolver,
            double lambda, BigDecimal originalError, int tickIndex) {

        double[] result = null;
        BigDecimal newError = originalError;
        BigDecimal factor = new BigDecimal("2");
        for (int iterations = 0; iterations < MAX_ITERATIONS && result == null; iterations++) {
            newError = newError.multiply(factor);
            if (newError.compareTo(UPPER_ERROR_BOUND) > 0) {
                factor = new BigDecimal("0.5");
                newError = originalError;
                iterations--;
                continue;
            }

            if (log != null) {
                log.log("Retrying CTMC transient computation with new error: "
                        + newError.doubleValue() + "\n");
            }

            foxGlynnSolver.getData().setRequiredAccuracy(newError.doubleValue());
            result = foxGlynnSolver.computeCtmcTransientProbabilities(lambda, log);
        }

        if (result == null) {
            if (log != null) {
                log.log("MAX_ITERATIONS=" + MAX_ITERATIONS + " reached, transients set to zeros\n");
            }
            result = getZerosResult(tickIndex);
        }

        foxGlynnSolver.getData().setRequiredAccuracy(originalError.doubleValue());
        return result;
    }
}
