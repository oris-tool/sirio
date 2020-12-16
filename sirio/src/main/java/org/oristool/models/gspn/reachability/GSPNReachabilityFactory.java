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

package org.oristool.models.gspn.reachability;

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
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * Factory of objects driving the state-space exploration for GSPNs.
 */
class GSPNReachabilityFactory implements
        AnalyzerComponentsFactory<PetriNet, Transition> {

    private EnumerationPolicy policy;
    private SuccessionEvaluator<PetriNet, Transition> successionEvaluator;

    private StopCriterion localStopCriterion;
    private StopCriterion globalStopCriterion;

    public GSPNReachabilityFactory(EnumerationPolicy policy,
            StopCriterion stopCondition, AnalysisMonitor monitor) {

        this.policy = policy;

        this.successionEvaluator = new GSPNSuccessionEvaluator();

        if (stopCondition == null)
            localStopCriterion = new AlwaysFalseStopCriterion();
        else
            localStopCriterion = stopCondition;

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
            public Set<Transition> getEnabledEvents(PetriNet pn, State state) {
                Marking m = state.getFeature(SPNState.class).state();
                Pair<Set<Transition>, Set<Transition>> enabled =
                        GSPNSuccessionEvaluator.enabledTransitions(m, pn);

                Set<Transition> imm = enabled.first();
                Set<Transition> exp = enabled.second();

                if (!imm.isEmpty())
                    return imm;
                else
                    return exp;
            }
        };
    }

    @Override
    public SuccessionEvaluator<PetriNet, Transition> getSuccessionEvaluator() {
        return successionEvaluator;
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
