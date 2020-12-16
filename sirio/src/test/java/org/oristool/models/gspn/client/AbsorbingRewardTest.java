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

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.onegen.OneGenTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 *  Instantaneous reward computed from the solution of a simple EXP cycle.
 */
public class AbsorbingRewardTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();
        marking = new Marking();

        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        Transition t0 = pn.addTransition("t0");
        Transition t1 = pn.addTransition("t1");

        pn.addPrecondition(p0, t0);
        pn.addPostcondition(t0, p1);
        pn.addPrecondition(p1, t1);
        pn.addPostcondition(t1, p0);

        marking.setTokens(p0, 1);
        marking.setTokens(p1, 0);
        t0.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
        t1.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
    }

    @Test
    void testTransientOneGen() {

        TransientSolution<DeterministicEnablingState, Marking> solution = OneGenTransient.builder()
                .error(new BigDecimal("0.000001"))
                .stopOn(MarkingCondition.fromString("p1 == 1"))
                .timeBound(new BigDecimal("5"))
                .timeStep(new BigDecimal("0.1"))
                .build()
                .compute(pn, marking);

        TransientSolution<DeterministicEnablingState, RewardRate> reward =
                TransientSolution.computeRewards(false, solution, "p1");

        double step = reward.getStep().doubleValue();
        for (int t = 0; t < reward.getSolution().length; t++) {
            assertEquals(1 - Math.exp(-t * step), reward.getSolution()[t][0][0], 1e-6);
        }
    }

    @Test
    void testTransientGSPN() {
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                .error(1e-6)
                .stopOn(MarkingCondition.fromString("p1 == 1"))
                .timePoints(0, 5, 0.1)
                .build()
                .compute(pn, marking);

        TransientSolution<Marking, Marking> solution =
                TransientSolution.fromArray(result.second(), 0.1, result.first(), marking);

        TransientSolution<Marking, RewardRate> reward =
                TransientSolution.computeRewards(false, solution, "p1");

        double step = reward.getStep().doubleValue();
        for (int t = 0; t < reward.getSolution().length; t++) {
            assertEquals(1 - Math.exp(-t * step), reward.getSolution()[t][0][0], 1e-6);
        }
    }
}
