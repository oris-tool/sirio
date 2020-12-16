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

package org.oristool.models.gspn.chains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.analyzer.log.NoOpMonitor;

import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableValueGraph;

/**
 * Computation of the stationary distribution of a DTMC.
 *
 * <p>LU factorization is used to solve the linear system of stationary
 * probabilities.
 *
 * <p>The DTMC can be periodic, but it must be irreducible.
 *
 * @param <S> type of DTMC states
 */
@AutoValue
public abstract class DTMCStationary<S> implements
        Function<MutableValueGraph<S, Double>, Map<S, Double>> {

    /**
     * Forbids subclassing outside of this package.
     */
    DTMCStationary() {

    }

    /**
     * Returns the threshold used to decide whether a probability value should be
     * considered equal to {@code 0.0} in the transition probabilities between
     * states and in the output distribution.
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
     * @param <S> type of DTMC states
     * @return a builder of {@code TimedAnalysis} instances.
     */
    public static <S> Builder<S> builder() {
        return new AutoValue_DTMCStationary.Builder<S>()
                .epsilon(1e-9)
                .monitor(NoOpMonitor.INSTANCE)
                .logger(NoOpLogger.INSTANCE);
    }

    @AutoValue.Builder
    public abstract static class Builder<S> {

        /**
         * Forbids subclassing outside of this package.
         */
        Builder() {

        }

        /**
         * Sets the threshold used to decide whether a probability value should be
         * considered equal to {@code 0.0} in the transition probabilities between
         * states and in the output distribution.
         *
         * <p>By default, it is equal to {@code 1e-9}.
         *
         * @param value the threshold used in comparisons to {@code 0.0}
         * @return this builder instance
         */
        public abstract Builder<S> epsilon(double value);

        /**
         * Sets the monitor used by this analysis. It is used to stop the analysis early
         * and to notify messages to the user.
         *
         * <p>By default, an always-false, message-discarding monitor is used.
         *
         * @param value the monitor used by this analysis
         * @return this builder instance
         */
        public abstract Builder<S> monitor(AnalysisMonitor value);

        /**
         * Sets the logger used by this analysis. It is used to print progress
         * information.
         *
         * <p>By default, logs are discarded.
         *
         * @param value the logger used by this analysis
         * @return this builder instance
         */
        public abstract Builder<S> logger(AnalysisLogger value);

        /**
         * Builds a new instance with the provided configurations.
         *
         * @return a new {@code DTMCStationary} instance
         */
        public abstract DTMCStationary<S> build();
    }

    /**
     * Runs this analysis on a DTMC.
     *
     * @param dtmc the input DTMC
     * @return a map from states of the DTMC to their stationary probability
     */
    @Override
    public Map<S, Double> apply(MutableValueGraph<S, Double> dtmc) {
        List<S> nodes = new ArrayList<>(dtmc.nodes());
        int n = nodes.size();

        // trivial cases
        if (n == 0) return Map.of();
        if (n == 1) return Map.of(nodes.get(0), 1.0);

        Map<S, Integer> pos = new HashMap<>(n);
        for (int i = 0; i < n; i++)
            pos.put(nodes.get(i), i);

        final DMatrixRMaj a = new DMatrixRMaj(n, n);  // transpose(P - I), with last row set to 1
        final DMatrixRMaj b = new DMatrixRMaj(n, 1);  // columns of all zeros followed by 1
        final DMatrixRMaj x = new DMatrixRMaj(n, 1);  // solution

        for (int i = 0; i < n; i++) {
            S toState = nodes.get(i);
            for (S fromState : dtmc.predecessors(toState)) {
                double prob = dtmc.edgeValueOrDefault(fromState, toState, 0.0);
                if (prob > epsilon()) {
                    int j = pos.get(fromState);
                    a.set(i, j, prob);
                }
            }
        }

        // subtract identity and fill last row with 1's
        for (int i = 0; i < n; i++) {
            a.add(i, i, -1.0);     // subtract identity from transpose(P)
            a.set(n - 1, i, 1.0);  // set last row to 1 (to sum solution elements)
        }

        b.set(n - 1, 0, 1.0);      // sum of elements must be equal to 1
        assert validMatrix(a, dtmc, nodes);

        LinearSolver<DMatrixRMaj, DMatrixRMaj> solver = LinearSolverFactory_DDRM.lu(n);
        if (!solver.setA(a))
            throw new IllegalArgumentException("Singular matrix");
        solver.solve(b, x);

        Map<S, Double> solution = new HashMap<>();
        for (int i = 0; i < n; i++) {
            double prob = x.get(i, 0);
            assert prob > -1e-12;
            if (prob > epsilon()) {
                solution.put(nodes.get(i), prob);
            }
        }

        return solution;
    }

    private boolean validMatrix(DMatrixRMaj a, MutableValueGraph<S, Double> dtmc, List<S> nodes) {

        // transpose(P - I) has columns that sum to
        // two minus incoming probabilities of last node
        S lastNode = nodes.get(nodes.size() - 1);
        DMatrixRMaj sumByCol = CommonOps_DDRM.sumCols(a, null);
        for (int i = 0; i < sumByCol.numCols; i++) {
            double incomingProb = dtmc.edgeValueOrDefault(nodes.get(i), lastNode, 0.0);
            double identityValue = i < sumByCol.numCols - 1 ? 1.0 : 0.0;
            double expected = (1.0 - incomingProb) - identityValue + 1.0;
            if (Math.abs(expected - sumByCol.get(0, i)) > epsilon())
                return false;
        }

        return true;
    }
}
