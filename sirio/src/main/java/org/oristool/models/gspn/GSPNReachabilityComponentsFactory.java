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

import java.util.LinkedHashSet;
import java.util.Set;

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
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.pn.MarkingUpdater;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.PetriSuccessionEvaluator;
import org.oristool.models.pn.PetriTokensAdder;
import org.oristool.models.pn.PetriTokensRemover;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

class GSPNReachabilityComponentsFactory implements
        AnalyzerComponentsFactory<PetriNet, Transition> {

    private EnumerationPolicy policy;
    private PetriSuccessionEvaluator petriSuccessionEvaluator;

    private StopCriterion localStopCriterion;
    private StopCriterion globalStopCriterion;

    public GSPNReachabilityComponentsFactory(MarkingUpdater tokensRemover,
            MarkingUpdater tokensAdder, boolean distinctNewlyEnabledConditions,
            EnumerationPolicy policy, MarkingCondition stopCondition,
            AnalysisMonitor monitor) {

        this.policy = policy;

        this.petriSuccessionEvaluator = new PetriSuccessionEvaluator(
                tokensRemover != null ? tokensRemover
                        : new PetriTokensRemover(),
                tokensAdder != null ? tokensAdder : new PetriTokensAdder(),
                distinctNewlyEnabledConditions);

        if (stopCondition == null)
            localStopCriterion = new AlwaysFalseStopCriterion();
        else
            localStopCriterion = new MarkingConditionStopCriterion(
                    stopCondition);

        if (monitor == null)
            globalStopCriterion = new AlwaysFalseStopCriterion();
        else
            globalStopCriterion = new MonitorStopCriterion(monitor);
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
                Set<Transition> enabled = state.getFeature(PetriStateFeature.class).getEnabled();
                Marking m = state.getFeature(PetriStateFeature.class).getMarking();

                Set<Transition> imm = new LinkedHashSet<Transition>();
                Set<Transition> exp = new LinkedHashSet<Transition>();

                for (Transition t : enabled)
                    if (t.hasFeature(RateExpressionFeature.class)
                            && t.getFeature(RateExpressionFeature.class)
                                .getRate(petriNet, m) > 0.0) {

                        exp.add(t);

                    } else if (t.hasFeature(WeightExpressionFeature.class)
                               && t.getFeature(WeightExpressionFeature.class)
                                   .getWeight(petriNet, m) > 0.0
                               && (!t.hasFeature(StochasticTransitionFeature.class)
                               || t.getFeature(StochasticTransitionFeature.class).isIMM())) {

                        imm.add(t);

                    } else {
                        throw new IllegalArgumentException(
                                "In GSPNs, transitions should either be EXP or IMM");
                    }

                if (imm.size() > 0)
                    return imm;
                else
                    return exp;
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
