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

package org.oristool.models.gspn.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.gspn.reachability.GSPNReachability;
import org.oristool.models.gspn.reachability.SPNState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * Trivial CTMC where the only successor of the initial state is absorbing.
 */
class TwoReducibleStatesTest {

    private PetriNet pn;
    private Marking marking;
    private SPNState s1;
    private SPNState s2;

    SPNState addState(String name, boolean selfEXP, boolean selfIMM) {

        Place p = pn.addPlace(name);

        Marking m = new Marking();
        m.setTokens(p, 1);
        SPNState s = new SPNState(m, 0.0);

        if (selfEXP) {
            Transition exp = pn.addTransition(name + "_exp");
            exp.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
            pn.addPrecondition(p, exp);
            pn.addPostcondition(exp, p);
            s = new SPNState(m, 1.0);
        }

        if (selfIMM) {
            Transition imm = pn.addTransition(name + "_imm");
            imm.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
            pn.addPrecondition(p, imm);
            pn.addPostcondition(imm, p);
            s = new SPNState(m, 0.0);
        }

        return s;
    }

    void buildModel(boolean selfEXP, boolean selfIMM) {
        pn = new PetriNet();

        s1 = addState("p1", selfEXP, selfIMM);
        s2 = addState("p2", selfEXP, selfIMM);

        Transition exp = pn.addTransition("move");
        exp.addFeature(StochasticTransitionFeature.newExponentialInstance("0.5"));
        pn.addPrecondition(pn.getPlace("p1"), exp);
        pn.addPostcondition(exp, pn.getPlace("p2"));
        s1 = new SPNState(s1.state(), s1.exitRate() + 0.5);

        marking = new Marking(s1.state());
    }

    @Test
    public void reachability() {

        for (boolean selfEXP : List.of(false, true)) {
            for (boolean selfIMM : List.of(false, true)) {
                buildModel(selfEXP, selfIMM);

                if (selfIMM) {  // time lock
                    assertThrows(IllegalStateException.class, () ->
                        GSPNReachability.builder().build().compute(pn, marking));
                } else {
                    DTMC<SPNState> result = GSPNReachability.builder().build().compute(pn, marking);
                    assertEquals(List.of(s1), result.initialStates());

                    int i1 = result.initialStates().indexOf(s1);
                    assertEquals(s1.exitRate(), result.initialStates().get(i1).exitRate(), 1e-15);
                    assertEquals(1.0, result.initialProbs().get(i1), 1e-15);

                    assertEquals(Set.of(s1, s2), result.probsGraph().nodes());
                    assertEquals(selfEXP ? 3 : 1, result.probsGraph().edges().size());
                    assertEquals(selfEXP, result.probsGraph().hasEdgeConnecting(s1, s1));
                    assertEquals(selfEXP, result.probsGraph().hasEdgeConnecting(s2, s2));
                    assertTrue(result.probsGraph().hasEdgeConnecting(s1, s2));
                }
            }
        }
    }

    @Test
    public void stationary() {

        for (boolean selfEXP : List.of(false, true)) {
            for (boolean selfIMM : List.of(false, true)) {
                buildModel(selfEXP, selfIMM);

                if (selfIMM) {  // time lock
                    assertThrows(IllegalStateException.class, () ->
                        GSPNSteadyState.builder().build().compute(pn, marking));
                } else {
                    Map<Marking, Double> dist =
                            GSPNSteadyState.builder().build().compute(pn, marking);

                    assertEquals(Set.of(s2.state()), dist.keySet());
                    assertEquals(1.0, dist.get(s2.state()), 1e-12);
                }
            }
        }
    }

    @Test
    public void transientProbs() {

        for (boolean selfEXP : List.of(false, true)) {
            for (boolean selfIMM : List.of(false, true)) {
                buildModel(selfEXP, selfIMM);
                double step = 0.1;

                if (selfIMM) {  // time lock
                    assertThrows(IllegalStateException.class, () -> GSPNTransient.builder()
                            .timePoints(0.0, 10.0, step).build().compute(pn, marking));
                } else {

                    Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                            .timePoints(0.0, 10.0, step)
                            .error(1e-9)
                            .build().compute(pn, marking);

                    Map<Marking, Integer> statePos = result.first();
                    double[][] probs = result.second();
                    assertEquals(Set.of(s1.state(), s2.state()), statePos.keySet());
                    int i1 = statePos.get(s1.state());
                    int i2 = statePos.get(s2.state());

                    for (int t = 0; t < probs.length; t++) {
                        double time = t * step;
                        double p1 = kolmogorovProb1(time, 1.0, 0.0, 0.5, 0.0);
                        assertEquals(p1, probs[t][i1], 1e-9);
                        assertEquals(1.0 - p1, probs[t][i2], 1e-9);
                    }
                }
            }
        }
    }

    private static double kolmogorovProb1(double time, double p1, double p2,
            double lambda12, double lambda21) {
        double lambdaSum = lambda12 + lambda21;
        double transientProb11 = (lambda21 + lambda12 * Math.exp(-lambdaSum * time)) / lambdaSum;
        double transientProb22 = (lambda12 + lambda21 * Math.exp(-lambdaSum * time)) / lambdaSum;
        return p1 * transientProb11 + p2 * (1 - transientProb22);
    }
}
