/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * Test features of update functions.
 */
public class UpdateFunctionsTest {
    @Test
    void testMax0() {
        for (Integer value : List.of(0, 1, 2)) {
            PetriNet pn = new PetriNet();
            Place p0 = pn.addPlace("p0");
            Place p1 = pn.addPlace("p1");
            Transition t0 = pn.addTransition("t0");
            pn.addPrecondition(p0, t0);
            t0.addFeature(new PostUpdater("p1=max(0,p1-1);", pn));
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ONE));

            Marking m = new Marking();
            m.setTokens(p0, 1);
            m.setTokens(p1, value);

            RegTransient analysis = RegTransient.builder()
                    .timeBound(new BigDecimal(2))
                    .timeStep(new BigDecimal("0.1")).build();
            TransientSolution<DeterministicEnablingState, Marking> result =
                    analysis.compute(pn, m);

            assertEquals(2, result.getRegenerations().size());
            assertEquals(2, result.getColumnStates().size());

            DeterministicEnablingState initialReg =
                    new DeterministicEnablingState(m, pn);
            assertEquals(initialReg, result.getRegenerations().get(0));

            Marking q = new Marking();
            q.setTokens(p0, 0);
            q.setTokens(p1, Math.max(0, value - 1));
            DeterministicEnablingState finalReg =
                    new DeterministicEnablingState(q, pn);
            assertEquals(finalReg, result.getRegenerations().get(1));


            for (int t = 0; t < result.getSamplesNumber(); t++) {
                double sum = 0.0;

                for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                    sum += result.getSolution()[t][0][j];
                }

                assertTrue(Math.abs(sum - 1) < 1e-9);
            }
        }
    }

    @Test
    void testMaxMixed() {
        for (Integer value : List.of(0, 1, 2, 3)) {
            PetriNet pn = new PetriNet();
            Place p0 = pn.addPlace("p0");
            Place p1 = pn.addPlace("p1");
            Transition t0 = pn.addTransition("t0");
            pn.addPrecondition(p0, t0);
            t0.addFeature(new PostUpdater("p1=round(max(0,p1-1,1));", pn));
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ONE));

            Marking m = new Marking();
            m.setTokens(p0, 1);
            m.setTokens(p1, value);

            RegTransient analysis = RegTransient.builder()
                    .timeBound(new BigDecimal(2))
                    .timeStep(new BigDecimal("0.1")).build();
            TransientSolution<DeterministicEnablingState, Marking> result =
                    analysis.compute(pn, m);

            assertEquals(2, result.getRegenerations().size());
            assertEquals(2, result.getColumnStates().size());

            DeterministicEnablingState initialReg =
                    new DeterministicEnablingState(m, pn);
            assertEquals(initialReg, result.getRegenerations().get(0));

            Marking q = new Marking();
            q.setTokens(p0, 0);
            q.setTokens(p1, Math.max(Math.max(0, value - 1), 1));
            DeterministicEnablingState finalReg =
                    new DeterministicEnablingState(q, pn);
            assertEquals(finalReg, result.getRegenerations().get(1));


            for (int t = 0; t < result.getSamplesNumber(); t++) {
                double sum = 0.0;

                for (int j = 0; j < result.getSolution()[t][0].length; j++) {
                    sum += result.getSolution()[t][0][j];
                }

                assertTrue(Math.abs(sum - 1) < 1e-9);
            }
        }
    }
}
