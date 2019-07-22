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

package org.oristool.models.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.onegen.OneGenTransient;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * A simple GSPN including marking-dependent clock rates for EXP transitions.
 */
class ExpClockRatesTest {

    private PetriNet pn;
    private Marking m0;
    private Marking m1;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        Transition t0 = pn.addTransition("t0");
        Transition t1 = pn.addTransition("t1");

        pn.addPrecondition(p0, t0);
        pn.addPostcondition(t0, p1);
        pn.addPrecondition(p1, t1);
        pn.addPostcondition(t1, p0);

        m0 = new Marking();
        m0.setTokens(p0, 1);
        m0.setTokens(p1, 0);

        m1 = new Marking();
        m1.setTokens(p0, 0);
        m1.setTokens(p1, 1);

        t0.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
        t1.addFeature(StochasticTransitionFeature.newExponentialInstance(BigDecimal.ONE,
                MarkingExpr.from("p1*2", pn)));
    }

    @Test
    public void stationaryGSPN() {
        Map<Marking, Double> dist = GSPNSteadyState.builder().build().compute(pn, m0);
        Map<Marking, Double> expected = Map.of(m0, 2.0 / 3.0, m1, 1.0 / 3.0);
        assertEquals(expected.size(), dist.size());
        for (Marking m : dist.keySet())
            assertEquals(expected.get(m), dist.get(m), 1e-9);
    }

    @Test
    public void stationaryReg() {
        SteadyStateSolution<Marking> result = RegSteadyState.builder().build().compute(pn, m0);
        Map<Marking, Double> expected = Map.of(m0, 2.0 / 3.0, m1, 1.0 / 3.0);
        assertEquals(expected.size(), result.getSteadyState().size());

        double maxError = 0.000001;
        double sum = result.getSteadyState().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
        assertTrue(Math.abs(1 - sum) < maxError);
        for (Marking m : result.getSteadyState().keySet())
            assertEquals(expected.get(m), result.getSteadyState().get(m).doubleValue(), 1e-9);
    }

    @Test
    public void transientGSPN() {

        double step = 0.1;
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                .timePoints(0.0, 10.0, step).error(1e-9).build().compute(pn, m0);

        Map<Marking, Integer> statePos = result.first();
        double[][] probs = result.second();

        assertEquals(Set.of(m0, m1), statePos.keySet());

        for (int t = 0; t < probs.length; t++) {
            double time = t * step;
            double p = kolmogorovProb1(time, 1.0, 0.0, 1.0, 2.0);
            assertEquals(p, probs[t][statePos.get(m0)], 1e-9);
            assertEquals(1.0 - p, probs[t][statePos.get(m1)], 1e-9);
        }
    }

    @Test
    public void transientReg() {

        BigDecimal step = new BigDecimal("0.001");
        TransientSolution<DeterministicEnablingState, Marking> result = RegTransient.builder()
                .timeBound(new BigDecimal("5"))
                .timeStep(step)
                .build().compute(pn, m0);

        assertEquals(Set.of(m0, m1), new HashSet<>(result.getColumnStates()));
        int i0 = result.getColumnStates().indexOf(m0);
        int i1 = result.getColumnStates().indexOf(m1);
        int r = result.getRegenerations().indexOf(result.getInitialRegeneration());

        for (int t = 0; t < result.getSolution().length; t++) {
            double time = t * step.doubleValue();
            double p = kolmogorovProb1(time, 1.0, 0.0, 1.0, 2.0);
            assertEquals(p, result.getSolution()[t][r][i0], 1e-3);
            assertEquals(1.0 - p, result.getSolution()[t][r][i1], 1e-3);
        }
    }

    @Test
    public void transientTree() {

        BigDecimal step = new BigDecimal("0.01");
        TransientSolution<Marking, Marking> result = TreeTransient.builder()
                .greedyPolicy(new BigDecimal("5"), new BigDecimal("1e-6"))
                .timeStep(step)
                .build().compute(pn, m0);

        assertEquals(Set.of(m0, m1), new HashSet<>(result.getColumnStates()));
        int i0 = result.getColumnStates().indexOf(m0);
        int i1 = result.getColumnStates().indexOf(m1);
        int r = result.getRegenerations().indexOf(result.getInitialRegeneration());

        for (int t = 0; t < result.getSolution().length; t++) {
            double time = t * step.doubleValue();
            double p = kolmogorovProb1(time, 1.0, 0.0, 1.0, 2.0);
            assertEquals(p, result.getSolution()[t][r][i0], 1e-6);
            assertEquals(1.0 - p, result.getSolution()[t][r][i1], 1e-6);
        }
    }

    @Test
    public void transientOneGen() {

        BigDecimal step = new BigDecimal("0.01");
        TransientSolution<DeterministicEnablingState, Marking> result = OneGenTransient.builder()
                .timeBound(new BigDecimal("5"))
                .timeStep(step)
                .error(new BigDecimal("0.001"))
                .build().compute(pn, m0);

        assertEquals(Set.of(m0, m1), new HashSet<>(result.getColumnStates()));
        int i0 = result.getColumnStates().indexOf(m0);
        int i1 = result.getColumnStates().indexOf(m1);
        int r = result.getRegenerations().indexOf(result.getInitialRegeneration());

        for (int t = 0; t < result.getSolution().length; t++) {
            double time = t * step.doubleValue();
            double p = kolmogorovProb1(time, 1.0, 0.0, 1.0, 2.0);
            assertEquals(p, result.getSolution()[t][r][i0], 1e-2);
            assertEquals(1.0 - p, result.getSolution()[t][r][i1], 1e-2);
        }
    }

    private static double kolmogorovProb1(double time, double p1, double p2, double lambda12,
            double lambda21) {
        double lambdaSum = lambda12 + lambda21;
        double transientProb11 = (lambda21 + lambda12 * Math.exp(-lambdaSum * time)) / lambdaSum;
        double transientProb22 = (lambda12 + lambda21 * Math.exp(-lambdaSum * time)) / lambdaSum;
        return p1 * transientProb11 + p2 * (1 - transientProb22);
    }
}
