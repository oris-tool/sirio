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

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.analyzer.log.NoOpMonitor;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.StateStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.models.Engine;
import org.oristool.models.ValidationMessageCollector;
import org.oristool.models.gspn.chains.CTMCTransient;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.gspn.reachability.GSPNReachability;
import org.oristool.models.gspn.reachability.SPNState;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

import com.google.auto.value.AutoValue;

/**
 * Computes transient probabilities for the underlying CTMC of a GSPN.
 *
 * <p>Immediate events are removed by solving absorption probabilities of
 * immediate components that may include cycles.
 *
 * <p>Transient probabilities are computed using uniformization.
 */
@AutoValue
public abstract class GSPNTransient
        implements Engine<PetriNet, Marking, Pair<Map<Marking, Integer>, double[][]>> {

    /**
     * Forbids subclassing outside of this package.
     */
    GSPNTransient() {

    }

    /**
     * Returns the time points of transient probabilities.
     *
     * @return target time points
     */
    @SuppressWarnings("mutable")
    public abstract double[] timePoints();

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
     * Returns the supplier of local stop criterion instances used by this analysis.
     * It can be used to avoid the expansion of some state classes, as if their
     * states were absorbing.
     *
     * <p>A stop criterion instance is generated for each run.
     *
     * <p>By default, an always-false local stop criterion is used.
     *
     * @return the supplier of local stop criterion
     */
    public abstract Supplier<StopCriterion> stopOn();

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
     * @return a builder of {@code TimedAnalysis} instances.
     */
    public static Builder builder() {
        return new AutoValue_GSPNTransient.Builder()
                .error(1e-6)
                .epsilon(1e-9)
                .stopOn(AlwaysFalseStopCriterion::new)
                .monitor(NoOpMonitor.INSTANCE)
                .logger(NoOpLogger.INSTANCE);
    }

    @AutoValue.Builder
    public abstract static class Builder {

        /**
         * Forbids subclassing outside of this package.
         */
        Builder() {

        }

        /**
         * Sets evenly spaced values as time points for transient probabilities.
         *
         * <p>The distance between points is given by {@code step}. The first point is
         * {@code start} and the last one is greater or equal to {@code end}.
         *
         * @param start initial point
         * @param end lower bound for the last point
         * @param step distance between points
         * @return this builder instance
         */
        public Builder timePoints(double start, double end, double step) {
            int n = (int) Math.ceil((end - start) / step) + 1;
            double[] timePoints = new double[n];
            for (int i = 0; i < n; i++)
                timePoints[i] = i * step;

            timePoints(timePoints);
            return this;
        }

        /**
         * Sets the time points for which transient probabilities must be computed.
         *
         * @param value target time points
         * @return this builder instance
         */
        public abstract Builder timePoints(double[] value);

        /**
         * Sets the allowed error in the computation of Poisson probabilities with
         * Fox-Glynn algorithm (for each time point).
         *
         * <p>By default, it is equal to {@code 1e-6}.
         *
         * @param value allowed error in Poisson probabilities
         * @return this builder instance
         */
        public abstract Builder error(double value);

        /**
         * Sets the threshold used to decide whether a probability value should be
         * considered equal to {@code 0.0}.
         *
         * <p>By default, it is equal to {@code 1e-9}.
         *
         * @param value the threshold used in comparisons to {@code 0.0}
         * @return this builder instance
         */
        public abstract Builder epsilon(double value);

        /**
         * Uses a marking condition to create local stop criterion instances used by
         * this analysis. It can be used to avoid the expansion of some state classes,
         * as if their states were absorbing.
         *
         * @param value the supplier of local stop criterion
         * @return this builder instance
         */
        public Builder stopOn(MarkingCondition value) {
            Predicate<State> p = s -> value.evaluate(s.getFeature(SPNState.class).state());
            stopOn(() -> new StateStopCriterion(p));
            return this;
        }

        /**
         * Sets the supplier of local stop criterion instances used by this analysis. It
         * can be used to avoid the expansion of some state classes, as if their states
         * were absorbing.
         *
         * <p>A stop criterion instance is generated for each run.
         *
         * <p>By default, an always-false local stop criterion is used.
         *
         * @param value the supplier of local stop criterion
         * @return this builder instance
         */
        public abstract Builder stopOn(Supplier<StopCriterion> value);

        /**
         * Sets the monitor used by this analysis. It is used to stop the analysis early
         * and to notify messages to the user.
         *
         * <p>By default, an always-false, message-discarding monitor is used.
         *
         * @param value the monitor used by this analysis
         * @return this builder instance
         */
        public abstract Builder monitor(AnalysisMonitor value);

        /**
         * Sets the logger used by this analysis. It is used to print progress
         * information.
         *
         * <p>By default, logs are discarded.
         *
         * @param value the logger used by this analysis
         * @return this builder instance
         */
        public abstract Builder logger(AnalysisLogger value);

        /**
         * Builds a new instance with the provided configurations.
         *
         * @return a new {@code TimedAnalysis} instance
         */
        public abstract GSPNTransient build();
    }

    /**
     * Runs this analysis on a given Petri net from an initial marking.
     *
     * @param pn the input Petri net
     * @param m the initial marking
     * @return a probability distribution over markings of the Petri net
     *
     * @throws IllegalArgumentException if the analysis is not applicable to the
     *         input Petri net
     */
    @Override
    public Pair<Map<Marking, Integer>, double[][]> compute(PetriNet pn, Marking m) {

        if (!canAnalyze(pn))
            throw new IllegalArgumentException("Cannot analyze the input Petri net");

        DTMC<SPNState> dtmc = GSPNReachability.builder()
            .stopOn(stopOn())
            .monitor(monitor())
            .logger(logger()).build().compute(pn, m);

        Pair<Map<Marking, Integer>, double[][]> solution =
                CTMCTransient.<Marking, SPNState>builder()
                .error(error())
                .epsilon(epsilon())
                .monitor(monitor())
                .logger(logger())
                .build().apply(dtmc, timePoints());

        return solution;
    }

    @Override
    public boolean canAnalyze(PetriNet pn, ValidationMessageCollector c) {

        boolean canAnalyze = true;

        for (Transition t : pn.getTransitions()) {
            if (!t.hasFeature(StochasticTransitionFeature.class)) {
                canAnalyze = false;
                c.addError("Transition '" + t + "' is not stochastic");

            } else {

                StochasticTransitionFeature f = t.getFeature(StochasticTransitionFeature.class);

                if (!f.isIMM() && !f.isEXP()) {
                    canAnalyze = false;
                    c.addError("Transition '" + t + "' is neither EXP nor IMM");
                }

                if (f.clockRate() != MarkingExpr.ONE && !f.isEXP()) {
                    canAnalyze = false;
                    c.addError("Transition '" + t + "' has non-unitary rate but is not EXP");
                }
            }
        }

        return canAnalyze;
    }
}
