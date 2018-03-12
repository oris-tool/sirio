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

package org.oristool.models.stpn.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.stpn.trees.TruncationPolicy;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * A simple STPN test analyzing a model with deterministic cycles.
 */
class DetCyclesTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        final Place p0 = pn.addPlace("p0");
        final Place p1 = pn.addPlace("p1");
        final Place p2 = pn.addPlace("p2");

        final Transition t01 = pn.addTransition("t01");
        final Transition t02 = pn.addTransition("t02");
        final Transition t10 = pn.addTransition("t10");
        final Transition t20 = pn.addTransition("t20");

        t01.addFeature(StochasticTransitionFeature.newDeterministicInstance("1"));
        t02.addFeature(StochasticTransitionFeature.newDeterministicInstance("1"));
        t10.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
        t20.addFeature(StochasticTransitionFeature.newDeterministicInstance("1"));

        pn.addPrecondition(p0, t01);
        pn.addPostcondition(t01, p1);
        pn.addPrecondition(p0, t02);
        pn.addPostcondition(t02, p2);
        pn.addPrecondition(p1, t10);
        pn.addPostcondition(t10, p0);
        pn.addPrecondition(p2, t20);
        pn.addPostcondition(t20, p0);

        marking = new Marking();
        marking.addTokens(p0, 1);
    }

    @Test
    void transientAnalysisVariants() {

        List<BigDecimal> timeBounds = Arrays.asList(BigDecimal.ONE,
                new BigDecimal("3"), new BigDecimal("10"));

        List<BigDecimal> timeSteps = Arrays.asList(null, new BigDecimal("0.1"),
                BigDecimal.ONE, new BigDecimal("2"));

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p2 > 1");
        List<Supplier<StopCriterion>> conditions = Arrays.asList(null, cond);

        for (final BigDecimal timeBound : timeBounds) {
            for (final BigDecimal timeStep : timeSteps) {
                for (Supplier<StopCriterion> stopOn : conditions) {

                    TreeTransient.Builder builder = TreeTransient.builder();

                    if (timeBound != null)
                        builder.timeBound(timeBound);

                    if (timeStep != null)
                        builder.timeStep(timeStep);

                    builder.greedyPolicy(timeBound, BigDecimal.ZERO);

                    if (stopOn != null)
                        builder.stopOn(stopOn);

                    if (timeStep == null) {
                        assertThrows(IllegalStateException.class, () -> builder.build());
                        continue;
                    }

                    TreeTransient analysis = builder.build();

                    // check that parameters are set correctly
                    assertEquals(timeBound, analysis.timeBound());
                    assertEquals(timeStep, analysis.timeStep());

                    TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
                    assertEquals(p.getEpsilon(), BigDecimal.ZERO);
                    assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));

                    if (stopOn == null)
                        assertEquals(AlwaysFalseStopCriterion.class,
                                analysis.stopOn().get().getClass());
                    else
                        assertEquals(stopOn, analysis.stopOn());


                    TransientSolution<Marking, Marking> result =
                            analysis.compute(pn, marking);

                    assertEquals(analysis.timeBound().divide(timeStep).intValue() + 1,
                            result.getSamplesNumber());

                    assertEquals(1, result.getRegenerations().size());
                    assertEquals(marking, result.getRegenerations().get(0));

                    assertEquals(3, result.getColumnStates().size());

                    double maxError = 0.000001;
                    for (int t = 0; t < result.getSamplesNumber(); t++) {
                        double sum = 0.0;

                        for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                            sum += result.getSolution()[t][0][j];
                        }

                        assertTrue(Math.abs(sum - 1) < maxError);
                    }
                }
            }
        }
    }

    @Test
    void regenerativeAnalysisVariants() {

        List<BigDecimal> timeBounds = Arrays.asList(BigDecimal.ONE,
                new BigDecimal("3"));

        List<BigDecimal> timeSteps = Arrays.asList(null, new BigDecimal("0.1"),
                BigDecimal.ONE, new BigDecimal("2"));

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p2 > 1");
        List<Supplier<StopCriterion>> conditions = Arrays.asList(null, cond);

        for (final BigDecimal timeBound : timeBounds) {
            for (final BigDecimal timeStep : timeSteps) {
                for (Supplier<StopCriterion> stopOn : conditions) {

                    RegTransient.Builder builder = RegTransient.builder();

                    if (timeBound != null)
                        builder.timeBound(timeBound);

                    if (timeStep != null)
                        builder.timeStep(timeStep);

                    builder.greedyPolicy(timeBound, BigDecimal.ZERO);

                    if (stopOn != null)
                        builder.stopOn(stopOn);

                    if (timeStep == null) {
                        assertThrows(IllegalStateException.class, () -> builder.build());
                        continue;
                    }

                    RegTransient analysis = builder.build();

                    // check that parameters are set correctly
                    assertEquals(timeBound, analysis.timeBound());
                    assertEquals(timeStep, analysis.timeStep());

                    TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
                    assertEquals(p.getEpsilon(), BigDecimal.ZERO);
                    assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));

                    if (stopOn == null)
                        assertEquals(AlwaysFalseStopCriterion.class,
                                analysis.stopOn().get().getClass());
                    else
                        assertEquals(stopOn, analysis.stopOn());


                    TransientSolution<DeterministicEnablingState, Marking> result =
                            analysis.compute(pn, marking);

                    assertEquals(analysis.timeBound().divide(timeStep).intValue() + 1,
                            result.getSamplesNumber());

                    assertEquals(2, result.getRegenerations().size());

                    DeterministicEnablingState initialReg =
                            new DeterministicEnablingState(marking, pn);
                    assertEquals(initialReg, result.getRegenerations().get(0));

                    assertEquals(3, result.getColumnStates().size());

                    double maxError = 0.000001;
                    for (int t = 0; t < result.getSamplesNumber(); t++) {
                        double sum = 0.0;

                        for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                            sum += result.getSolution()[t][0][j];
                        }

                        assertTrue(Math.abs(sum - 1) < maxError);
                    }
                }
            }
        }
    }

    @Test
    void steadyStateAnalysisVariants() {

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p2 > 1");
        List<Supplier<StopCriterion>> conditions = Arrays.asList(null, cond);

        for (final Supplier<StopCriterion> stopOn : conditions) {

            RegSteadyState.Builder builder = RegSteadyState.builder();
            if (stopOn != null)
                builder.stopOn(stopOn);

            RegSteadyState analysis = builder.build();

            // check that parameters are set correctly
            if (stopOn == null)
                assertEquals(AlwaysFalseStopCriterion.class,
                        analysis.stopOn().get().getClass());
            else
                assertEquals(stopOn, analysis.stopOn());

            SteadyStateSolution<Marking> result =
                    analysis.compute(pn, marking);

            assertEquals(stopOn == null ? 3 : 3,
                    result.getSteadyState().keySet().size());

            double maxError = 0.000001;
            double sum = result.getSteadyState().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
            assertEquals(1, sum, maxError);
            assertEquals(0.66666666666666,
                    result.getSteadyState().get(marking).doubleValue(), 1e-12);
        }
    }
}
