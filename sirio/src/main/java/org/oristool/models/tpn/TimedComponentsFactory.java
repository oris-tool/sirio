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

package org.oristool.models.tpn;

import java.util.Set;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.AnalyzerComponentsFactory;
import org.oristool.analyzer.EnabledEventsBuilder;
import org.oristool.analyzer.NoOpProcessor;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.policy.FIFOPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.MonitorStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.pn.MarkingUpdater;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.PetriTokensAdder;
import org.oristool.models.pn.PetriTokensRemover;
import org.oristool.models.stpn.EnablingSyncsEvaluator;
import org.oristool.models.stpn.NewlyEnablingEvaluator;
import org.oristool.models.stpn.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Factory of objects to explore the state space of a time Petri net using
 * {@link Analyzer}.
 */
public final class TimedComponentsFactory implements
        AnalyzerComponentsFactory<PetriNet, Transition> {

    private EnumerationPolicy policy;
    private TimedSuccessionEvaluator timedSuccessionEvaluator;

    private StopCriterion localStopCriterion;
    private StopCriterion globalStopCriterion;
    private SuccessionProcessor postProcessor;

    private InitialTimedStateBuilder timedInitialStateBuilder;

    /**
     * Builds a default factory for TPN analysis.
     */
    public TimedComponentsFactory() {
        this(false, false, false, false, false, new FIFOPolicy(), null, null);
    }

    /**
     * Builds a factory for TPN analysis.
     *
     * @param transientAnalysis whether to include {@code Variable.AGE}
     * @param checkNewlyEnabled whether to check newly-enabled sets in state
     *        comparisons
     * @param excludeZeroProb whether to exclude firing with zero probability
     * @param markRegenerations whether to annotate states with {@code Regeneration}
     *        objects
     * @param enablingSyncs whether to use enabling synchronizations when checking
     *        for regenerations
     * @param policy state enumeration policy
     * @param stopCondition stop condition
     * @param monitor analysis monitor
     */
    public TimedComponentsFactory(boolean transientAnalysis,
            boolean checkNewlyEnabled, boolean excludeZeroProb,
            boolean markRegenerations, boolean enablingSyncs,
            EnumerationPolicy policy, MarkingCondition stopCondition,
            AnalysisMonitor monitor) {

        this(transientAnalysis, checkNewlyEnabled,
                excludeZeroProb, markRegenerations, enablingSyncs, policy,
                stopCondition, monitor, null, null);
    }

    /**
     * Builds a factory for TPN analysis.
     *
     * @param transientAnalysis whether to include {@code Variable.AGE}
     * @param checkNewlyEnabled whether to check newly-enabled sets in state
     *        comparisons
     * @param excludeZeroProb whether to exclude firing with zero probability
     * @param markRegenerations whether to annotate states with {@code Regeneration}
     *        objects
     * @param enablingSyncs whether to use enabling synchronizations when checking
     *        for regenerations
     * @param policy state enumeration policy
     * @param stopCondition stop condition
     * @param monitor analysis monitor
     * @param tokensRemover component used to remove tokens
     * @param tokensAdder component used to add tokens
     */
    public TimedComponentsFactory(boolean transientAnalysis,
            boolean checkNewlyEnabled, boolean excludeZeroProb,
            boolean markRegenerations, boolean enablingSyncs,
            EnumerationPolicy policy, MarkingCondition stopCondition,
            AnalysisMonitor monitor, MarkingUpdater tokensRemover,
            MarkingUpdater tokensAdder) {

        this(transientAnalysis, checkNewlyEnabled,
                excludeZeroProb, markRegenerations, enablingSyncs, policy,
                stopCondition != null ? new MarkingConditionStopCriterion(
                        stopCondition) : null, monitor, null, null);
    }

    /**
     * Builds a factory for TPN analysis.
     *
     * @param transientAnalysis whether to include {@code Variable.AGE}
     * @param checkNewlyEnabled whether to check newly-enabled sets in state
     *        comparisons
     * @param excludeZeroProb whether to exclude firing with zero probability
     * @param markRegenerations whether to annotate states with {@code Regeneration}
     *        objects
     * @param enablingSyncs whether to use enabling synchronizations when checking
     *        for regenerations
     * @param policy state enumeration policy
     * @param stopCondition stop condition
     * @param monitor analysis monitor
     * @param tokensAdder object used to remove tokens
     * @param tokensRemover object used to add tokens
     */
    public TimedComponentsFactory(boolean transientAnalysis,
            boolean checkNewlyEnabled, boolean excludeZeroProb,
            boolean markRegenerations, boolean enablingSyncs,
            EnumerationPolicy policy, StopCriterion stopCondition,
            AnalysisMonitor monitor, MarkingUpdater tokensRemover,
            MarkingUpdater tokensAdder) {

        this.policy = policy;

        timedSuccessionEvaluator = new TimedSuccessionEvaluator(
                tokensRemover != null ? tokensRemover
                        : new PetriTokensRemover(),
                tokensAdder != null ? tokensAdder : new PetriTokensAdder(),
                        checkNewlyEnabled, excludeZeroProb);

        if (stopCondition == null)
            localStopCriterion = new AlwaysFalseStopCriterion();
        else
            localStopCriterion = stopCondition;

        if (monitor == null)
            globalStopCriterion = new AlwaysFalseStopCriterion();
        else
            globalStopCriterion = new MonitorStopCriterion(monitor);

        if (markRegenerations) {
            if (enablingSyncs)
                postProcessor = new EnablingSyncsEvaluator();
            else
                postProcessor = new NewlyEnablingEvaluator();
        } else {
            postProcessor = NoOpProcessor.INSTANCE;
        }

        // for the construction of initial states
        timedInitialStateBuilder = new InitialTimedStateBuilder(
                transientAnalysis, checkNewlyEnabled);
    }

    @Override
    public EnumerationPolicy getEnumerationPolicy() {
        return policy;
    }

    @Override
    public EnabledEventsBuilder<PetriNet, Transition> getEnabledEventsBuilder() {

        // we exclude enabled transitions that are not firable
        // by returning null in the successor evaluator
        return new EnabledEventsBuilder<PetriNet, Transition>() {
            @Override
            public Set<Transition> getEnabledEvents(PetriNet petriNet,
                    State state) {
                return state.getFeature(PetriStateFeature.class).getEnabled();
            }
        };
    }

    @Override
    public SuccessionEvaluator<PetriNet, Transition> getSuccessionEvaluator() {
        return timedSuccessionEvaluator;
    }

    @Override
    public SuccessionProcessor getPreProcessor() {
        return NoOpProcessor.INSTANCE;
    }

    @Override
    public SuccessionProcessor getPostProcessor() {
        return postProcessor;
    }

    @Override
    public StopCriterion getLocalStopCriterion() {
        return localStopCriterion;
    }

    @Override
    public StopCriterion getGlobalStopCriterion() {
        return globalStopCriterion;
    }

    /**
     * Builds an initial {@link State} instance from a marking.
     *
     * <p>The state includes a {@code PetriStateFeature} and a
     * {@code TimedStateFeature}.
     *
     * <p>The {@code TimedStateFeature} will include {@code Variable.AGE} if this
     * factory was initialized with {@code transientAnalysis == true}.
     *
     * <p>The {@code PetriStateFeature} will distinguish states with the same
     * marking but different sets of newly enabled transitions if this factory was
     * initialized with {@code checkNewlyEnabled == true}.
     *
     * @param pn Petri net associated with the marking
     * @param initialMarking initial marking
     * @return a state instance with {@code PetriStateFeature} and
     *         {@code TimedStateFeature}
     */
    public State buildInitialState(PetriNet pn, Marking initialMarking) {

        // adds a TimedFeature to STPNs
        for (Transition t : pn.getTransitions())
            if (!t.hasFeature(TimedTransitionFeature.class)
                    && t.hasFeature(StochasticTransitionFeature.class))
                t.addFeature(t.getFeature(StochasticTransitionFeature.class)
                        .asTimedTransitionFeature());

        return timedInitialStateBuilder.computeInitialState(pn,
                initialMarking);
    }
}
