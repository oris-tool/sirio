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
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.policy.FIFOPolicy;
import org.oristool.analyzer.policy.LIFOPolicy;
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
 * A simple STPN test analyzing models with multiple uniform transitions in
 * parallel.
 */
class ParallelTransitionsTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        final Place p0 = pn.addPlace("p0");
        final Place p1 = pn.addPlace("p1");
        final Place p2 = pn.addPlace("p2");
        final Place p3 = pn.addPlace("p3");

        final Transition t0 = pn.addTransition("t0");
        final Transition t1 = pn.addTransition("t1");
        final Transition t2 = pn.addTransition("t2");

        t0.addFeature(StochasticTransitionFeature.newUniformInstance("0.5", "1.5"));
        t1.addFeature(StochasticTransitionFeature.newUniformInstance("1.0", "2.0"));
        t2.addFeature(StochasticTransitionFeature.newUniformInstance("1.8", "2.8"));

        pn.addPrecondition(p0, t0);
        pn.addPrecondition(p1, t1);
        pn.addPrecondition(p2, t2);
        pn.addPostcondition(t0, p3);
        pn.addPostcondition(t1, p3);
        pn.addPostcondition(t2, p3);

        marking = new Marking();
        marking.addTokens(p0, 1);
        marking.addTokens(p1, 1);
        marking.addTokens(p2, 1);
    }

    @Test
    void transientAnalysisVariants() {

        List<BigDecimal> timeBounds = Arrays.asList(null, BigDecimal.ONE,
                BigDecimal.TEN, new BigDecimal("10.5"));

        List<BigDecimal> timeSteps = Arrays.asList(null, new BigDecimal("0.1"),
                BigDecimal.ONE, new BigDecimal("2"));

        Supplier<EnumerationPolicy> fifo = FIFOPolicy::new;
        Supplier<EnumerationPolicy> lifo = LIFOPolicy::new;

        BigDecimal greedyError = new BigDecimal("0.1");
        BigDecimal greedyBound = new BigDecimal("2.8");
        Supplier<EnumerationPolicy> greedy = () -> new TruncationPolicy(greedyError,
                new OmegaBigDecimal(greedyBound));
        List<Supplier<EnumerationPolicy>> policies = Arrays.asList(null, fifo, lifo, greedy);

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p3 > 1");
        List<Supplier<StopCriterion>> conditions = Arrays.asList(null, cond);

        for (final BigDecimal timeBound : timeBounds) {
            for (final BigDecimal timeStep : timeSteps) {
                for (final Supplier<EnumerationPolicy> policy : policies) {
                    for (Supplier<StopCriterion> stopOn : conditions) {

                        TreeTransient.Builder builder = TreeTransient.builder();

                        if (timeBound != null)
                            builder.timeBound(timeBound);

                        if (timeStep != null)
                            builder.timeStep(timeStep);

                        if (policy != null) {
                            if (policy == greedy)
                                builder.greedyPolicy(greedyBound, greedyError);
                            else
                                builder.policy(policy);

                        }

                        if (stopOn != null)
                            builder.stopOn(stopOn);

                        if ((timeBound == null && policy != greedy) || timeStep == null) {
                            assertThrows(IllegalStateException.class, () -> builder.build());
                            continue;
                        }

                        TreeTransient analysis = builder.build();

                        // check that parameters are set correctly
                        assertEquals(policy != greedy ? timeBound : greedyBound,
                                analysis.timeBound());
                        assertEquals(timeStep, analysis.timeStep());

                        if (policy == null) {
                            assertEquals(FIFOPolicy.class, analysis.policy().get().getClass());
                        } else if (policy.equals(fifo)) {
                            assertEquals(fifo, analysis.policy());
                        } else if (policy.equals(lifo)) {
                            assertEquals(lifo, analysis.policy());
                        } else {
                            TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
                            assertEquals(p.getEpsilon(), greedyError);
                            assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(greedyBound));
                        }

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

                        int states = 6;
                        if (stopOn != null)
                            states--;
                        if (policy == greedy)
                            states--;

                        assertEquals(states, result.getColumnStates().size());

                        double maxError = policy != greedy
                                ? 0.000001 : greedyError.doubleValue();
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
    }

    @Test
    void regenerativeAnalysisVariants() {

        List<BigDecimal> timeBounds = Arrays.asList(null, BigDecimal.ONE,
                BigDecimal.TEN, new BigDecimal("10.5"));

        List<BigDecimal> timeSteps = Arrays.asList(null, new BigDecimal("0.1"),
                BigDecimal.ONE, new BigDecimal("2"));

        Supplier<EnumerationPolicy> fifo = FIFOPolicy::new;
        Supplier<EnumerationPolicy> lifo = LIFOPolicy::new;

        BigDecimal greedyError = new BigDecimal("0.1");
        BigDecimal greedyBound = new BigDecimal("2.8");
        Supplier<EnumerationPolicy> greedy = () -> new TruncationPolicy(greedyError,
                new OmegaBigDecimal(greedyBound));
        List<Supplier<EnumerationPolicy>> policies = Arrays.asList(null, fifo, lifo, greedy);

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p3 > 1");
        List<Supplier<StopCriterion>> conditions = Arrays.asList(null, cond);

        for (final BigDecimal timeBound : timeBounds) {
            for (final BigDecimal timeStep : timeSteps) {
                for (final Supplier<EnumerationPolicy> policy : policies) {
                    for (final Supplier<StopCriterion> stopOn : conditions) {

                        RegTransient.Builder builder = RegTransient.builder();

                        if (timeBound != null)
                            builder.timeBound(timeBound);

                        if (timeStep != null)
                            builder.timeStep(timeStep);

                        if (policy != null) {
                            if (policy == greedy)
                                builder.greedyPolicy(greedyBound, greedyError);
                            else
                                builder.policy(policy);

                        }

                        if (stopOn != null)
                            builder.stopOn(stopOn);

                        if ((timeBound == null && policy != greedy) || timeStep == null) {
                            assertThrows(IllegalStateException.class, () -> builder.build());
                            continue;
                        }

                        RegTransient analysis = builder.build();

                        // check that parameters are set correctly
                        assertEquals(policy != greedy ? timeBound : greedyBound,
                                analysis.timeBound());
                        assertEquals(timeStep, analysis.timeStep());

                        if (policy == null) {
                            assertEquals(FIFOPolicy.class, analysis.policy().get().getClass());
                        } else if (policy.equals(fifo)) {
                            assertEquals(fifo, analysis.policy());
                        } else if (policy.equals(lifo)) {
                            assertEquals(lifo, analysis.policy());
                        } else {
                            TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
                            assertEquals(p.getEpsilon(), greedyError);
                            assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(greedyBound));
                        }

                        if (stopOn == null)
                            assertEquals(AlwaysFalseStopCriterion.class,
                                    analysis.stopOn().get().getClass());
                        else
                            assertEquals(stopOn, analysis.stopOn());


                        TransientSolution<DeterministicEnablingState, Marking> result =
                                analysis.compute(pn, marking);

                        assertEquals(analysis.timeBound().divide(timeStep).intValue() + 1,
                                result.getSamplesNumber());

                        assertEquals(stopOn == null ? 2 : 1,
                                result.getRegenerations().size());

                        DeterministicEnablingState initialReg =
                                new DeterministicEnablingState(marking, pn);
                        assertEquals(initialReg, result.getRegenerations().get(0));

                        int states = 6;
                        if (stopOn != null)
                            states--;
                        if (policy == greedy)
                            states--;

                        assertEquals(states, result.getColumnStates().size());

                        double maxError = policy != greedy
                                ? 0.000001 : greedyError.doubleValue();

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
    }

    @Test
    void steadyStateAnalysisVariants() {

        Supplier<StopCriterion> cond = () -> new MarkingConditionStopCriterion("p3 > 1");
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

            assertEquals(stopOn == null ? 6 : 5,
                    result.getSteadyState().keySet().size());

            double maxError = 0.000001;
            double sum = result.getSteadyState().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
            assertTrue(Math.abs(1 - sum) < maxError);
        }
    }
}
