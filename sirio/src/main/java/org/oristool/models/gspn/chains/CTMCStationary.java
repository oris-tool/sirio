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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.analyzer.log.NoOpMonitor;

import com.google.auto.value.AutoValue;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;

/**
 * Computation of the stationary distribution of a CTMC.
 *
 * <p>LU factorization is used to solve the linear system of stationary
 * probabilities for the embedded DTMC. Exit rates are then used to obtain
 * stationary probabilities of the CTMC from those of the DTMC.
 *
 * <p>This analysis can be applied to CTMCs with reducible state space and
 * multiple BSCCs. In this case, the stationary probabilities within each BSCC
 * are weighted according to its absorption probability from the initial
 * distribution of the CTMC.
 *
 * @param <M> type of logic state
 * @param <S> type of CTMC states (including a logic state and an exit rate)
 */
@AutoValue
public abstract class CTMCStationary<M, S extends CTMCState<M>>
        implements Function<DTMC<S>, Map<M, Double>> {

    /**
     * Forbids subclassing outside of this package.
     */
    CTMCStationary() {

    }

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
        return new AutoValue_CTMCStationary.Builder<M, S>()
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
         * @return a new {@code CTMCStationary} instance
         */
        public abstract CTMCStationary<M, S> build();
    }

    /**
     * Runs this analysis on a CTMC.
     *
     * @param dtmc the embedded DTMC of the input CTMC
     * @return a distribution over the states of the CTMC
     */
    @Override
    public Map<M, Double> apply(DTMC<S> dtmc) {

        AbsorptionProbs<S> absorption = AbsorptionProbs.compute(dtmc.probsGraph());

        // BSCC absorption probabilities from initial distribution
        double[] bsccProbs = new double[absorption.bscc().size()];
        for (int i = 0; i < dtmc.initialStates().size(); i++) {
            S initialState = dtmc.initialStates().get(i);
            double initialStateProb = dtmc.initialProbs().get(i);

            if (absorption.isBottom(initialState)) {
                bsccProbs[absorption.bsccIndexFor(initialState)] +=  initialStateProb;

            } else {
                int transientIndex = absorption.transientIndex(initialState);
                for (int j = 0; j < bsccProbs.length; j++) {
                    bsccProbs[j] +=  initialStateProb * absorption.probs(transientIndex, j);
                }
            }
        }

        // common configuration for each stationary analysis
        DTMCStationary<S> stationaryAnalysis = DTMCStationary.<S>builder()
                .epsilon(epsilon())
                .logger(logger())
                .monitor(monitor())
                .build();

        // multiply the stationary distribution within each BSCC by its absorption probability
        Map<M, Double> solution = new HashMap<>();
        for (int j = 0; j < bsccProbs.length; j++) {
            Set<S> bscc = absorption.bscc().get(j);
            double bsccProb = bsccProbs[j];
            if (bscc.size() == 1) {
                solution.merge(bscc.iterator().next().state(), bsccProb, Double::sum);

            } else {
                MutableValueGraph<S, Double> bsccGraph =
                        Graphs.inducedSubgraph(dtmc.probsGraph(), absorption.bscc().get(j));
                Map<S, Double> stationaryDist = stationaryAnalysis.apply(bsccGraph);

                double meanSojourn = 0.0;
                for (Map.Entry<S, Double> e : stationaryDist.entrySet())
                    meanSojourn += e.getValue() / e.getKey().exitRate();

                for (Map.Entry<S, Double> e : stationaryDist.entrySet()) {
                    solution.merge(e.getKey().state(), bsccProb
                            * e.getValue() / e.getKey().exitRate() / meanSojourn, Double::sum);
                }
            }
        }

        return solution;
    }
}
