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

package org.oristool.models.stpn.onegen.client;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.onegen.OneGenTransient;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;


/**
 * A simple test for the analysis under enabling restriction.
 */
class OneGenTest {

    PetriNet pn;
    Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();
        marking = new Marking();

        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        Place p2 = pn.addPlace("p2");
        Place p3 = pn.addPlace("p3");

        Transition t0 = pn.addTransition("t0");
        Transition t1 = pn.addTransition("t1");
        Transition t2 = pn.addTransition("t2");

        pn.addPrecondition(p0, t0);
        pn.addPostcondition(t0, p1);

        pn.addPrecondition(p2, t1);
        pn.addPostcondition(t1, p3);

        pn.addPrecondition(p3, t2);
        pn.addPostcondition(t2, p2);

        marking.setTokens(p0, 1);
        marking.setTokens(p1, 0);
        marking.setTokens(p2, 1);
        marking.setTokens(p3, 0);

        t0.addFeature(StochasticTransitionFeature.newUniformInstance("0", "1"));
        t1.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
        t2.addFeature(StochasticTransitionFeature.newExponentialInstance("1"));
    }

    @Test
    void testResults() {
        BigDecimal error = new BigDecimal("0.0001");
        BigDecimal timeBound = new BigDecimal("5");
        BigDecimal timeStep = new BigDecimal("0.1");

        RegTransient analysis = RegTransient.builder()
                  .greedyPolicy(timeBound, error)
                  .timeStep(timeStep)
                  .build();

        TransientSolution<DeterministicEnablingState, Marking> solutionRegenerative =
                  analysis.compute(pn, marking);

        OneGenTransient analysisOneGen = OneGenTransient.builder()
                  .timeBound(timeBound)
                  .timeStep(timeStep)
                  .error(error)
                  .build();

        TransientSolution<DeterministicEnablingState, Marking> solutionOneGen =
                analysisOneGen.compute(pn, marking);

        double[][][] solutionMatrixRegen = solutionRegenerative.getSolution();
        double[][][] orderedSolutionMatrixOnegen = OneGenUtils.order(solutionRegenerative,
                solutionOneGen, solutionRegenerative.getColumnStates(),
                solutionOneGen.getColumnStates(), solutionOneGen.getSolution());

        OneGenUtils.testMatrices(solutionMatrixRegen, orderedSolutionMatrixOnegen);
    }
}
