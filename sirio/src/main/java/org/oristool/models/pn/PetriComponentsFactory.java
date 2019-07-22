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

package org.oristool.models.pn;

import java.util.Set;

import org.oristool.analyzer.Analyzer;
import org.oristool.analyzer.AnalyzerComponentsFactory;
import org.oristool.analyzer.EnabledEventsBuilder;
import org.oristool.analyzer.NoOpProcessor;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Factory of objects to explore the state space of a Petri net using
 * {@link Analyzer}.
 */
public final class PetriComponentsFactory implements
        AnalyzerComponentsFactory<PetriNet, Transition> {

    private final EnumerationPolicy policy;
    private final PetriSuccessionEvaluator petriSuccessionEvaluator;

    private final StopCriterion localStopCriterion;
    private final StopCriterion globalStopCriterion;

    /**
     * Builds a factor from a set of input objects.
     *
     * @param tokensRemover the token remover function
     * @param tokensAdder the token adder function
     * @param checkNewlyEnabled whether to check the set of newly enabled
     *        transitions when comparing states
     * @param policy the policy used to select the next state
     */
    public PetriComponentsFactory(MarkingUpdater tokensRemover,
            MarkingUpdater tokensAdder, boolean checkNewlyEnabled,
            EnumerationPolicy policy) {

        this.policy = policy;

        this.petriSuccessionEvaluator = new PetriSuccessionEvaluator(
                tokensRemover != null ? tokensRemover : new PetriTokensRemover(),
                tokensAdder != null ? tokensAdder : new PetriTokensAdder(),
                checkNewlyEnabled);

        this.localStopCriterion = new AlwaysFalseStopCriterion();
        this.globalStopCriterion = new AlwaysFalseStopCriterion();
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
        return petriSuccessionEvaluator;
    }

    @Override
    public SuccessionProcessor getPreProcessor() {
        return NoOpProcessor.INSTANCE;
    }

    @Override
    public SuccessionProcessor getPostProcessor() {
        return NoOpProcessor.INSTANCE;
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
