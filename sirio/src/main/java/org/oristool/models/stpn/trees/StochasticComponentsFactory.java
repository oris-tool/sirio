/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2019 The ORIS Authors.
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

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.util.Set;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.AnalyzerComponentsFactory;
import org.oristool.analyzer.EnabledEventsBuilder;
import org.oristool.analyzer.NoOpProcessor;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.MonitorStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.pn.MarkingUpdater;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.PetriTokensAdder;
import org.oristool.models.pn.PetriTokensRemover;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Factory of objects to build the state space and PDFs of a stochastic time
 * Petri net using {@link Analyzer}.
 */
public final class StochasticComponentsFactory implements
        AnalyzerComponentsFactory<PetriNet, Transition> {

    private final EnumerationPolicy policy;
    private final StochasticSuccessionEvaluator stochasticSuccessionEvaluator;
    private final StopCriterion localStopCriterion;
    private final StopCriterion globalStopCriterion;
    private final SuccessionProcessor postProcessor;

    /**
     * Builds a factory for STPN analysis.
     *
     * @param transientAnalysis whether to include {@code Variable.AGE}
     * @param tokensRemover the token remover function
     * @param tokensAdder the token adder function
     * @param checkNewlyEnabled whether to check newly-enabled sets in state
     *        comparisons
     * @param policy policy for the selection of new nodes
     * @param tauAgeLimit time bound for the analysis
     * @param stopCondition stop condition for the analysis
     * @param epsilon allowed error when comparing states
     * @param numSamples number of samples used to compare states
     * @param monitor analysis monitor
     */
    public StochasticComponentsFactory(boolean transientAnalysis,
            MarkingUpdater tokensRemover, MarkingUpdater tokensAdder,
            boolean checkNewlyEnabled, EnumerationPolicy policy,
            OmegaBigDecimal tauAgeLimit, MarkingCondition stopCondition,
            BigDecimal epsilon, int numSamples, AnalysisMonitor monitor) {

        this(transientAnalysis, tokensRemover, tokensAdder, checkNewlyEnabled,
                policy, tauAgeLimit, new MarkingConditionStopCriterion(stopCondition),
                epsilon, numSamples, monitor);
    }

    /**
     * Builds a factory for STPN analysis.
     *
     * @param transientAnalysis whether to include {@code Variable.AGE}
     * @param tokensRemover the token remover function
     * @param tokensAdder the token adder function
     * @param checkNewlyEnabled whether to check newly-enabled sets in state
     *        comparisons
     * @param policy policy for the selection of new nodes
     * @param tauAgeLimit time bound for the analysis
     * @param stopCriterion local stop criterion for the analysis
     * @param epsilon allowed error when comparing states
     * @param numSamples number of samples used to compare states
     * @param monitor analysis monitor
     */
    public StochasticComponentsFactory(boolean transientAnalysis,
            MarkingUpdater tokensRemover, MarkingUpdater tokensAdder,
            boolean checkNewlyEnabled, EnumerationPolicy policy,
            OmegaBigDecimal tauAgeLimit, StopCriterion stopCriterion,
            BigDecimal epsilon, int numSamples, AnalysisMonitor monitor) {

        this.policy = policy;
        this.stochasticSuccessionEvaluator = new StochasticSuccessionEvaluator(
                transientAnalysis, tokensRemover != null ? tokensRemover
                        : new PetriTokensRemover(),
                tokensAdder != null ? tokensAdder : new PetriTokensAdder(),
                checkNewlyEnabled, tauAgeLimit);

        if (stopCriterion == null)
            localStopCriterion = new AlwaysFalseStopCriterion();
        else
            localStopCriterion = stopCriterion;

        globalStopCriterion = monitor != null ? new MonitorStopCriterion(
                monitor) : new AlwaysFalseStopCriterion();
        postProcessor = new NewlyEnablingEvaluator();
    }

    @Override
    public EnumerationPolicy getEnumerationPolicy() {
        return policy;
    }

    @Override
    public EnabledEventsBuilder<PetriNet, Transition> getEnabledEventsBuilder() {
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
        return stochasticSuccessionEvaluator;
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
}
