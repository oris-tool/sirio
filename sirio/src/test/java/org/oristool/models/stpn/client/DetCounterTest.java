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

package org.oristool.models.stpn.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.stpn.trees.TruncationPolicy;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * A simple STPN test analyzing a model with deterministic accumulation.
 */
class DetCounterTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        final Place p1 = pn.addPlace("p1");
        final Transition t0 = pn.addTransition("t0");
        t0.addFeature(StochasticTransitionFeature.newDeterministicInstance("1"));

        pn.addPostcondition(t0, p1);

        marking = new Marking();
    }

    @Test
    void transientAnalysisVariants() {

        List<BigDecimal> timeBounds = Arrays.asList(BigDecimal.ONE,
                new BigDecimal("0.5"), BigDecimal.ONE, new BigDecimal("3"));

        List<BigDecimal> timeSteps = Arrays.asList(new BigDecimal("0.1"),
                new BigDecimal("0.3"), BigDecimal.ONE, new BigDecimal("2"));

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p1 >= 1");
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

                    if (stopOn == null) {
                        assertEquals(AlwaysFalseStopCriterion.class,
                                analysis.stopOn().get().getClass());
                    } else {
                        assertEquals(stopOn, analysis.stopOn());
                    }
                    
                    TransientSolution<Marking, Marking> result =
                            analysis.compute(pn, marking);

                    assertEquals(analysis.timeBound().divide(timeStep, MathContext.DECIMAL64).intValue() + 1,
                            result.getSamplesNumber());

                    assertEquals(1, result.getRegenerations().size());
                    assertEquals(marking, result.getRegenerations().get(0));

                    int states = stopOn == null ? analysis.timeBound().intValue() + 1 : 2;
                    if (timeBound.compareTo(BigDecimal.ONE) < 0)
                        states = 1;
                    assertEquals(states, result.getColumnStates().size());

                    double maxError = 0.000001;
                    for (int t = 0; t < result.getSamplesNumber(); t++) {
                        double sum = 0.0;

                        for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                            sum += result.getSolution()[t][0][j];
                        }

                        assertTrue(Math.abs(sum - 1) < maxError);  // sum to 1

                        int count = result.getStep().multiply(BigDecimal.valueOf(t)).intValue();
                        if (stopOn != null)
                            count = Integer.min(count, 1);

                        for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                            if (result.getColumnStates().get(j).getTokens("p1") == count) {
                                assertTrue(Math.abs(sum - 1) < maxError);  // close to 1
                            } else {
                                assertTrue(Math.abs(sum - 1) < maxError);  // close to 0
                            }
                        }
                    }
                }
            }
        }
    }

    /*
    @Test
    void regenerativeAnalysisVariants() {

        List<BigDecimal> timeBounds = Arrays.asList(BigDecimal.ONE,
                new BigDecimal("0.5"), BigDecimal.ONE, new BigDecimal("3"));

        List<BigDecimal> timeSteps = Arrays.asList(new BigDecimal("0.1"),
                new BigDecimal("0.3"), BigDecimal.ONE, new BigDecimal("2"));

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p1 >= 1");
        List<Supplier<StopCriterion>> conditions = Arrays.asList(null, cond); 

        for (final BigDecimal timeBound : timeBounds) {
            for (final BigDecimal timeStep : timeSteps) {
                for (Supplier<StopCriterion> stopOn : conditions) {

                    RegTransient.Builder builder = RegTransient.builder();

                    if (timeBound != null)
                        builder.timeBound(timeBound);

                    if (timeStep != null)
                        builder.timeStep(timeStep);
                    builder.logger(new PrintStreamLogger(System.out));
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

                    if (stopOn == null) {
                        assertEquals(AlwaysFalseStopCriterion.class,
                                analysis.stopOn().get().getClass());
                    } else {
                        assertEquals(stopOn, analysis.stopOn());
                    }
                    
                    TransientSolution<DeterministicEnablingState, Marking> result =
                            analysis.compute(pn, marking);

                    assertEquals(analysis.timeBound().divide(timeStep, MathContext.DECIMAL64).intValue() + 1,
                            result.getSamplesNumber());

                    assertEquals(1, result.getRegenerations().size());
                    assertEquals(marking, result.getRegenerations().get(0));

                    int states = stopOn == null ? analysis.timeBound().intValue() + 1 : 2;
                    if (timeBound.compareTo(BigDecimal.ONE) < 0)
                        states = 1;
                    assertEquals(states, result.getColumnStates().size());

                    double maxError = 0.000001;
                    for (int t = 0; t < result.getSamplesNumber(); t++) {
                        double sum = 0.0;

                        for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                            sum += result.getSolution()[t][0][j];
                        }

                        assertTrue(Math.abs(sum - 1) < maxError);  // sum to 1

                        int count = result.getStep().multiply(BigDecimal.valueOf(t)).intValue();
                        if (stopOn != null)
                            count = Integer.min(count, 1);

                        for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                            if (result.getColumnStates().get(j).getTokens("p1") == count) {
                                assertTrue(Math.abs(sum - 1) < maxError);  // close to 1
                            } else {
                                assertTrue(Math.abs(sum - 1) < maxError);  // close to 0
                            }
                        }
                    }
                }
            }
        }
    }
    */
}
