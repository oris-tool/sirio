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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;
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
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

import com.google.auto.value.AutoValue;

/**
 * Transient analysis of STPNs with at most one general transition enabled in
 * each state.
 */
@AutoValue
public abstract class OneGenTransient
        implements Engine<PetriNet, Marking,
            TransientSolution<DeterministicEnablingState, Marking>> {

    /**
     * Forbids subclassing outside of this package.
     */
    OneGenTransient() {

    }

    /**
     * Returns the maximum time bound for the analysis.
     *
     * <p>This parameter has no default value; it must be specified by the user.
     *
     * @return time bound of transient probabilities
     */
    public abstract BigDecimal timeBound();

    /**
     * Returns the step used to compute transient probabilities from 0 to
     * {@code this.timeBound()}.
     *
     * <p>This parameter has no default value; it must be specified by the user.
     *
     * @return step of transient probabilities
     */
    public abstract BigDecimal timeStep();

    /**
     * Returns the maximum allowed error for the computation of transient
     * probabilities of subordinated CTMCs.
     *
     * <p>This parameter has no default value; it must be specified by the user.
     *
     * @return error of subordinated transient probabilities
     */
    public abstract BigDecimal error();

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
        return new AutoValue_OneGenTransient.Builder()
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
         * Sets the maximum time bound for the analysis.
         *
         * <p>This parameter has no default value; it must be specified by the user.
         *
         * @param value bound of transient probabilities
         * @return this builder instance
         */
        public abstract Builder timeBound(BigDecimal value);

        /**
         * Sets the step used to compute transient probabilities from 0 to
         * {@code this.timeBound()}.
         *
         * <p>This parameter has no default value; it must be specified by the user.
         *
         * @param value of transient probabilities
         * @return this builder instance
         */
        public abstract Builder timeStep(BigDecimal value);

        /**
         * Sets the maximum allowed error for the computation of transient probabilities
         * of subordinated CTMCs.
         *
         * <p>This parameter has no default value; it must be specified by the user.
         *
         * @param value of maximum allowed error
         * @return error of subordinated transient probabilities
         */
        public abstract Builder error(BigDecimal value);

        /**
         * Uses a marking condition to create local stop criterion instances used by
         * this analysis. It can be used to avoid the expansion of some state classes,
         * as if their states were absorbing.
         *
         * @param value the supplier of local stop criterion
         * @return this builder instance
         */
        public Builder stopOn(MarkingCondition value) {
            Predicate<State> p = s ->
                value.evaluate(s.getFeature(PetriStateFeature.class).getMarking());
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
        public abstract OneGenTransient build();
    }

    @Override
    public TransientSolution<DeterministicEnablingState, Marking> compute(PetriNet pn, Marking m) {

        if (!canAnalyze(pn))
            throw new IllegalArgumentException("Cannot analyze the input Petri net");

        EnablingRestrictionTransientAnalysis analysis =
                new EnablingRestrictionTransientAnalysis(pn, m,
                        stopOn().get(), logger(), monitor());

        TransientSolution<DeterministicEnablingState, Marking> solution =
                analysis.computeSolution(error(), timeStep(), timeBound());

        return solution;
    }

    @Override
    public boolean canAnalyze(PetriNet pn, ValidationMessageCollector c) {

        boolean canAnalyze = true;

        for (Transition t : pn.getTransitions()) {
            if (!t.hasFeature(StochasticTransitionFeature.class)) {
                canAnalyze = false;
                c.addError("Transition '" + t + "' is not stochastic");

            } else if (!t.getFeature(StochasticTransitionFeature.class).isEXP()
                    && !t.getFeature(StochasticTransitionFeature.class)
                        .clockRate().equals(MarkingExpr.ONE)) {
                canAnalyze = false;
                c.addError("GEN transition '" + t + "' has rate different than 1");
            }
        }

        return canAnalyze;
    }
}
