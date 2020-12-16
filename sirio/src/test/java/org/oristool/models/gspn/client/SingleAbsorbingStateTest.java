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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.gspn.chains.DTMC;
import org.oristool.models.gspn.reachability.GSPNReachability;
import org.oristool.models.gspn.reachability.SPNState;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * A trivial CTMC with just one state.
 */
class SingleAbsorbingStateTest {

    private PetriNet pn;
    private Marking marking;
    private SPNState initialState;

    void buildModel(boolean selfEXP, boolean selfIMM) {
        pn = new PetriNet();
        marking = new Marking();

        Place p0 = pn.addPlace("p0");
        marking.addTokens(p0, 1);
        initialState = new SPNState(marking, 0.0);

        if (selfEXP) {
            Transition exp = pn.addTransition("exp");
            exp.addFeature(StochasticTransitionFeature.newExponentialInstance(
                    new BigDecimal("0.5"), MarkingExpr.from("p0 * 2", pn)));  // rate is 1
            pn.addPrecondition(p0, exp);
            pn.addPostcondition(exp, p0);
            initialState = new SPNState(marking, 1.0);
        }

        if (selfIMM) {
            Transition imm = pn.addTransition("imm");
            imm.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
            pn.addPrecondition(p0, imm);
            pn.addPostcondition(imm, p0);
            initialState = new SPNState(marking, 0.0);
        }
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
                    assertEquals(List.of(initialState), result.initialStates());

                    int initialStateIndex = result.initialStates().indexOf(initialState);
                    assertEquals(initialState.exitRate(),
                            result.initialStates().get(initialStateIndex).exitRate(), 1e-15);
                    assertEquals(1.0,
                            result.initialProbs().get(initialStateIndex), 1e-15);
                    assertEquals(Set.of(initialState), result.probsGraph().nodes());
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
                    assertEquals(1, dist.size());
                    assertTrue(dist.containsKey(marking));
                    assertEquals(1.0, dist.get(marking), 1e-12);
                }
            }
        }
    }

    @Test
    public void transientProbs() {

        for (boolean selfEXP : List.of(false, true)) {
            for (boolean selfIMM : List.of(false, true)) {
                buildModel(selfEXP, selfIMM);

                if (selfIMM) {  // time lock
                    assertThrows(IllegalStateException.class, () ->
                        GSPNSteadyState.builder().build().compute(pn, marking));
                } else {
                    double step = 0.1;
                    Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                            .timePoints(0.0, 10.0, step)
                            .error(1e-9)
                            .build().compute(pn, marking);

                    Map<Marking, Integer> statePos = result.first();
                    double[][] probs = result.second();

                    assertEquals(Set.of(marking), statePos.keySet());

                    for (int t = 0; t < probs.length; t++) {
                        assertEquals(1.0, probs[t][0], 1e-9);
                    }
                }
            }
        }
    }
}
