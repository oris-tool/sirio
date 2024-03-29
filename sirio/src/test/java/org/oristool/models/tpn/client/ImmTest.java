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

package org.oristool.models.tpn.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.policy.FIFOPolicy;
import org.oristool.analyzer.policy.LIFOPolicy;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.expression.Variable;
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.stpn.trees.EnablingSyncsFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.tpn.TimedAnalysis;
import org.oristool.models.tpn.TimedStateFeature;
import org.oristool.models.tpn.TimedTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * A model with immediate and non-deterministic transitions.
 */
class ImmTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        Place p2 = pn.addPlace("p2");
        Place p3 = pn.addPlace("p3");

        Transition t1 = pn.addTransition("t1");
        Transition t2 = pn.addTransition("t2");
        Transition t3 = pn.addTransition("t3");

        pn.addPrecondition(p0, t1);
        pn.addPostcondition(t1, p1);
        pn.addPrecondition(p0, t2);
        pn.addPostcondition(t2, p2);
        pn.addPrecondition(p0, t3);
        pn.addPostcondition(t3, p3);

        t1.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
        t2.addFeature(new TimedTransitionFeature("0.0", "0.0"));
        t3.addFeature(new TimedTransitionFeature("0", "5"));

        marking = new Marking();
        marking.addTokens(p0, 1);
    }

    @Test
    void timedAnalysisVariants() {

        Supplier<EnumerationPolicy> fifo = FIFOPolicy::new;
        Supplier<EnumerationPolicy> lifo = LIFOPolicy::new;
        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p0 > 0");

        for (Boolean includeAge : Arrays.asList(null, false, true)) {
            for (Boolean markRegenerations : Arrays.asList(null, false, true)) {
                for (Boolean excludeZeroProb : Arrays.asList(null, false, true)) {
                    for (Supplier<EnumerationPolicy> policy : Arrays.asList(null, fifo, lifo)) {
                        for (Supplier<StopCriterion> stopOn : Arrays.asList(null, cond)) {

                            TimedAnalysis.Builder builder = TimedAnalysis.builder();

                            if (includeAge != null)
                                builder.includeAge(includeAge);
                            if (markRegenerations != null)
                                builder.markRegenerations(markRegenerations);
                            if (excludeZeroProb != null)
                                builder.excludeZeroProb(excludeZeroProb);
                            if (policy != null)
                                builder.policy(policy);
                            if (stopOn != null)
                                builder.stopOn(stopOn);

                            TimedAnalysis analysis = builder.build();

                            // check that parameters are set correctly
                            assertEquals(includeAge == null ? false : includeAge,
                                    analysis.includeAge());
                            assertEquals(markRegenerations == null ? false : markRegenerations,
                                    analysis.markRegenerations());
                            assertEquals(excludeZeroProb == null ? false : excludeZeroProb,
                                    analysis.excludeZeroProb());

                            if (policy == null)
                                assertEquals(FIFOPolicy.class, analysis.policy().get().getClass());
                            else if (policy.equals(fifo))
                                assertEquals(fifo, analysis.policy());
                            else
                                assertEquals(lifo, analysis.policy());

                            if (stopOn == null)
                                assertEquals(AlwaysFalseStopCriterion.class,
                                        analysis.stopOn().get().getClass());
                            else
                                assertEquals(stopOn, analysis.stopOn());

                            if (!analysis.canAnalyze(pn)) {
                                assertThrows(IllegalArgumentException.class, () ->
                                    analysis.compute(pn, marking));

                            } else {
                                SuccessionGraph graph = analysis.compute(pn, marking);

                                for (State s: graph.getStates()) {
                                    assertEquals(includeAge != null && includeAge,
                                            s.getFeature(TimedStateFeature.class).getDomain()
                                            .getVariables().contains(Variable.AGE));

                                    assertEquals(markRegenerations != null && markRegenerations,
                                            s.hasFeature(EnablingSyncsFeature.class));
                                }

                                if (stopOn != null) {
                                    assertEquals(1, graph.getStates().size());
                                    assertEquals(0, graph.getSuccessions().size());

                                } else if (excludeZeroProb != null && excludeZeroProb) {
                                    assertEquals(3, graph.getStates().size());
                                    assertEquals(2, graph.getSuccessions().size());

                                } else {
                                    assertEquals(4, graph.getStates().size());
                                    assertEquals(3, graph.getSuccessions().size());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
