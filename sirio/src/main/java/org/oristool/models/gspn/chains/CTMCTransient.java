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

package org.oristool.models.gspn.chains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.dense.row.CommonOps_DDRM;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.analyzer.log.NoOpMonitor;
import org.oristool.util.Pair;

import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableValueGraph;

/**
 * Computation of the transient probabilities of a CTMC.
 *
 * <p>Uniformization is used to compute transient probabilities for each input
 * time point. Poisson probabilities are computed using Fox-Glynn algorithm,
 * while transition probabilities of the uniformized DTMC are computed through
 * successive matrix multiplications.
 *
 * <p>A sparse matrix is used for the transition matrix of the embedded DTMC.
 *
 * @param <M> type of logic state
 * @param <S> type of CTMC states (including a logic state and an exit rate)
 */
@AutoValue
public abstract class CTMCTransient<M, S extends CTMCState<M>>
        implements BiFunction<DTMC<S>, double[], Pair<Map<M, Integer>, double[][]>> {

    /**
     * Forbids subclassing outside of this package.
     */
    CTMCTransient() {

    }

    /**
     * Returns the allowed error in the computation of Poisson probabilities with
     * Fox-Glynn algorithm (for each time point).
     *
     * <p>By default, it is equal to {@code 1e-6}.
     *
     * @return allowed error in Poisson probabilities
     */
    public abstract double error();

    /**
     * Returns the threshold used to decide whether a probability value should be
     * considered equal to {@code 0.0}.
     *
     * <p>By default, it is equal to {@code 1e-9}.
     *
     * @return the threshold used in comparisons to {@code 0.0}
     */
    public abstract double epsilon();

    /**
     * Returns the monitor used by this analysis. It is used to stop the analysis
     * early and to notify messages to the user.
     *
     * <p>By default, an always-false, message-discarding monitor is used.
     *
     * @return the monitor used by this analysis
     */
    public abstract AnalysisMonitor monitor();

    /**
     * Returns the logger used by this analysis. It is used to print progress
     * information.
     *
     * <p>By default, logs are discarded.
     *
     * @return the logger used by this analysis
     */
    public abstract AnalysisLogger logger();

    /**
     * Creates a builder for analysis configurations (with default values).
     *
     * @param <M> type of logic state
     * @param <S> type of CTMC states (including a logic state and an exit rate)
     * @return a builder of {@code TimedAnalysis} instances.
     */
    public static <M, S extends CTMCState<M>> Builder<M, S> builder() {
        return new AutoValue_CTMCTransient.Builder<M, S>()
                .error(1e-6)
                .epsilon(1e-9)
                .monitor(NoOpMonitor.INSTANCE)
                .logger(NoOpLogger.INSTANCE);
    }

    @AutoValue.Builder
    public abstract static class Builder<M, S extends CTMCState<M>> {

        /**
         * Forbids subclassing outside of this package.
         */
        Builder() {

        }

        /**
         * Sets the allowed error in the computation of Poisson probabilities with
         * Fox-Glynn algorithm (for each time point).
         *
         * <p>By default, it is equal to {@code 1e-6}.
         *
         * @param value allowed error in Poisson probabilities
         * @return this builder instance
         */
        public abstract Builder<M, S> error(double value);

        /**
         * Sets the threshold used to decide whether a probability value should be
         * considered equal to {@code 0.0}.
         *
         * <p>By default, it is equal to {@code 1e-9}.
         *
         * @param value the threshold used in comparisons to {@code 0.0}
         * @return this builder instance
         */
        public abstract Builder<M, S> epsilon(double value);

        /**
         * Sets the monitor used by this analysis. It is used to stop the analysis early
         * and to notify messages to the user.
         *
         * <p>By default, an always-false, message-discarding monitor is used.
         *
         * @param value the monitor used by this analysis
         * @return this builder instance
         */
        public abstract Builder<M, S> monitor(AnalysisMonitor value);

        /**
         * Sets the logger used by this analysis. It is used to print progress
         * information.
         *
         * <p>By default, logs are discarded.
         *
         * @param value the logger used by this analysis
         * @return this builder instance
         */
        public abstract Builder<M, S> logger(AnalysisLogger value);

        /**
         * Builds a new instance with the provided configurations.
         *
         * @return a new {@code CTMCTransient} instance
         */
        public abstract CTMCTransient<M, S> build();
    }

    private static <S extends CTMCState<?>> double uniformizationRate(
            MutableValueGraph<S, Double> graph) {

        double maxRate = 0.0;
        for (S state : graph.nodes())
            maxRate = Math.max(maxRate, state.exitRate());
        maxRate *= 1.02;

        return maxRate;
    }

    private static <M, S extends CTMCState<M>> DMatrixSparseCSC uniformizedDTMC(
            MutableValueGraph<S, Double> graph, List<S> states, Map<M, Integer> pos,
            double unifRate, double epsilon) {

        int n = states.size();
        DMatrixSparseCSC transposeOneStep = new DMatrixSparseCSC(n, n, graph.edges().size());

        for (int i = 0; i < n; i++) {
            S fromState = states.get(i);

            double totalExitProb = 0.0;
            for (S toState : graph.successors(fromState)) {
                if (!toState.equals(fromState)) {
                    double edgeProb = graph.edgeValueOrDefault(fromState, toState, 0.0);
                    if (edgeProb > epsilon) {
                        int j = pos.get(toState.state());
                        edgeProb = fromState.exitRate() / unifRate * edgeProb;
                        transposeOneStep.set(j, i, edgeProb);  // reversed indices
                        totalExitProb += edgeProb;
                    }
                }
            }

            transposeOneStep.set(i, i, 1.0 - totalExitProb);
        }

        return transposeOneStep;
    }

    /**
     * Runs this analysis on an input CTMC.
     *
     * @param dtmc the embedded DTMC of the input CTMC
     * @param timePoints an ordered list of time points
     * @return transient probabilities for each time-point and state
     */
    @Override
    public Pair<Map<M, Integer>, double[][]> apply(DTMC<S> dtmc, double[] timePoints) {

        MutableValueGraph<S, Double> graph = dtmc.probsGraph();

        // prepare ordered list of states and map from state to position
        int n = graph.nodes().size();
        List<S> nodes = new ArrayList<>(n);
        Map<M, Integer> pos = new HashMap<>(n);
        for (S node : graph.nodes()) {
            nodes.add(node);
            pos.put(node.state(), pos.size());
        }

        // prepare initial distribution from that of CTMC
        DMatrixRMaj initialProbs = new DMatrixRMaj(n, 1);
        for (int s = 0; s < dtmc.initialStates().size(); s++) {
            S initialNode = dtmc.initialStates().get(s);
            double initialProb = dtmc.initialProbs().get(s);
            int i = pos.get(initialNode.state());
            initialProbs.set(i, 0, initialProb);
        }

        // if unifRate is zero, every state is absorbing
        double unifRate = uniformizationRate(graph);
        if (unifRate == 0.0) {
            double[][] arrayRepr = new double[timePoints.length][];
            for (int t = 0; t < timePoints.length; t++)
                arrayRepr[t] = initialProbs.copy().getData();
            return Pair.of(pos, arrayRepr);
        }

        // uniformization of the CTMC (use transpose one-step matrix)
        DMatrixSparseCSC transposedOneStep =
                uniformizedDTMC(graph, nodes, pos, unifRate, epsilon());

        // compute Poisson weights and result vectors for all time points
        IntervalScanner.Builder<FoxGlynn> scannerBuilder = IntervalScanner.builder();
        DMatrixRMaj[] results = new DMatrixRMaj[timePoints.length];
        Map<FoxGlynn, Integer> timePointOf = new HashMap<>(timePoints.length);

        for (int t = 0; t < timePoints.length; t++) {
            double time = timePoints[t];
            if (time > 0.0) {
                results[t] = initialProbs.createLike();
                FoxGlynn range = FoxGlynn.computeReduced(unifRate * time, error());
                scannerBuilder.addInterval(range);
                timePointOf.put(range, t);

            } else if (time == 0.0) {
                results[t] = initialProbs.copy();

            } else {
                throw new IllegalArgumentException("Negative time " + time);
            }
        }

        // compute DTMC transient until last Poisson event required by any time point
        DTMCTransientIter stateDist = DTMCTransientIter.from(transposedOneStep, initialProbs);
        IntervalScanner<FoxGlynn> scanner = scannerBuilder.build();

        while (!scanner.intervals().isEmpty()) {
            Iterator<FoxGlynn> it = scanner.nextTimeIntervals();
            int time = scanner.time();
            stateDist.advanceTo(time);

            while (it.hasNext()) {
                // add weight * stateDist to result at time-point
                FoxGlynn range = it.next();
                DMatrixRMaj result = results[timePointOf.get(range)];
                CommonOps_DDRM.addEquals(result,
                        range.poissonProb(time),
                        stateDist.currentProbs());

                // if (time == range.rightPoint())
                //     CommonOps_DDRM.divide(range.totalWeight(), result);
            }
        }

        // avoid exposing matrix library classes
        double[][] arrayRepr = new double[results.length][];
        for (int i = 0; i < results.length; i++)
            arrayRepr[i] = results[i].getData();

        return Pair.of(pos, arrayRepr);
    }
}
