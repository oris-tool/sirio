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

package org.oristool.models.tpn;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.analyzer.log.NoOpMonitor;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.policy.FIFOPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.StateStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.models.Engine;
import org.oristool.models.ValidationMessageCollector;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

import com.google.auto.value.AutoValue;


/**
 * State-class graph builder for time Petri nets.
 */
@AutoValue
public abstract class TimedAnalysis implements Engine<PetriNet, Marking, SuccessionGraph> {

    /**
     * Forbids subclassing outside of this package.
     */
    TimedAnalysis() {

    }

    /**
     * Returns whether or not this analysis adds {@code Variable.AGE} to the set of
     * enabled variables.
     *
     * <p>The age variable causes all state classes to include the possible values
     * for the time of the last firing, which are encoded by the opposite of
     * {@code Variable.AGE}.
     *
     * <p>In most cases, this turns the state class graph in a tree.
     *
     * <p>This property is false by default.
     *
     * @return whether the analysis includes the age variable
     */
    public abstract boolean includeAge();

    /**
     * Returns whether or not this analysis adds the {@link Regeneration} property
     * to states.
     *
     * <p>All transitions must include a {@link StochasticTransitionFeature}.
     *
     * <p>This property is false by default.
     *
     * @return whether the analysis finds regenerations
     */
    public abstract boolean markRegenerations();

    /**
     * Returns whether or not this analysis excludes transition firings with zero
     * probability.
     *
     * <p>These are firings such that some transition has time-to-fire values
     * {@code [a,b]} with {@code b > a} in the predecessor state class, and
     * {@code b == a} in the successor state class.
     *
     * <p>This property is false by default.
     *
     * @return whether the analysis excludes transitions with zero probability
     */
    public abstract boolean excludeZeroProb();

    /**
     * Returns the supplier of enumeration policies used by this analysis.
     *
     * <p>A new policy instance is generated for each run.
     *
     * <p>By default, a FIFO policy is used.
     *
     * @return the supplier of state class expansion policies
     */
    public abstract Supplier<EnumerationPolicy> policy();

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
        return new AutoValue_TimedAnalysis.Builder()
                .includeAge(false)
                .markRegenerations(false)
                .excludeZeroProb(false)
                .policy(FIFOPolicy::new)
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
         * Sets whether or not this analysis should add {@code Variable.AGE} to the set
         * of enabled variables.
         *
         * <p>This property is false by default.
         *
         * @param value whether the analysis should include the age variable
         * @return this builder instance
         */
        public abstract Builder includeAge(boolean value);

        /**
         * Sets whether or not this analysis should add the {@link Regeneration}
         * property to states.
         *
         * <p>This property is false by default.
         *
         * @param value whether the analysis should find regenerations
         * @return this builder instance
         */
        public abstract Builder markRegenerations(boolean value);

        /**
         * Sets whether or not this analysis should exclude transition firings with zero
         * probability.
         *
         * <p>This property is false by default.
         *
         * @param value whether the analysis should exclude transitions with zero
         *        probability
         * @return this builder instance
         */
        public abstract Builder excludeZeroProb(boolean value);

        /**
         * Sets the supplier of enumeration policies used by this analysis.
         *
         * <p>A new policy instance is generated for each run.
         *
         * <p>By default, a FIFO policy is used.
         *
         * @param value the supplier of state class expansion policies
         * @return this builder instance
         */
        public abstract Builder policy(Supplier<EnumerationPolicy> value);

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
        public abstract TimedAnalysis build();

    }

    /**
     * Runs this analysis on a given Petri net from an initial marking.
     *
     * @param pn the input Petri net
     * @param m the initial marking
     * @return a succession graph encoding the state class graph
     *
     * @throws IllegalArgumentException if the analysis is not applicable to the
     *         input Petri net
     */
    @Override
    public SuccessionGraph compute(PetriNet pn, Marking m) {

        if (!canAnalyze(pn))
            throw new IllegalArgumentException("Cannot analyze the input Petri net");

        TimedComponentsFactory components = new TimedComponentsFactory(includeAge(), false,
                excludeZeroProb(), markRegenerations(), true, policy().get(), stopOn().get(),
                monitor(), null, null);

        Analyzer<PetriNet, Transition> analyzer = new Analyzer<>(components, pn,
                components.buildInitialState(pn, m));

        return analyzer.analyze();
    }

    @Override
    public boolean canAnalyze(PetriNet pn, ValidationMessageCollector c) {

        boolean canAnalyze = true;

        if (markRegenerations()) {
            for (Transition t : pn.getTransitions()) {
                if (!t.hasFeature(StochasticTransitionFeature.class)) {
                    canAnalyze = false;
                    c.addError("Transition '" + t + "' is not stochastic");
                }
            }
        }

        for (Transition t : pn.getTransitions()) {
            if (!t.hasFeature(TimedTransitionFeature.class)
                    && !t.hasFeature(StochasticTransitionFeature.class)) {
                canAnalyze = false;
                c.addError("Transition '" + t
                        + "' is neither timed nor stochastic");
            }
        }

        return canAnalyze;
    }
}
