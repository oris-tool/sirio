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

package org.oristool.models.client;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.onegen.OneGenTransient;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * A simple STPN including marking-dependent clock rates for UNIF transitions.
 */
class GenClockRatesTest {

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
        t1.addFeature(StochasticTransitionFeature.newUniformInstance(
                BigDecimal.ZERO, BigDecimal.ONE, MarkingExpr.from("p1*2", pn)));
    }

    @Test
    public void stationaryGSPN() {
        GSPNSteadyState analysis = GSPNSteadyState.builder().build();
        assertFalse(analysis.canAnalyze(pn));
    }

    @Test
    public void stationaryReg() {
        RegSteadyState analysis = RegSteadyState.builder().build();
        assertFalse(analysis.canAnalyze(pn));
    }

    @Test
    public void transientGSPN() {
        GSPNTransient analysis = GSPNTransient.builder()
                .timePoints(0.0, 10.0, 0.1).error(1e-9).build();
        assertFalse(analysis.canAnalyze(pn));
    }

    @Test
    public void transientReg() {
        BigDecimal step = new BigDecimal("0.001");
        RegTransient analysis = RegTransient.builder()
                .timeBound(new BigDecimal("5"))
                .timeStep(step)
                .build();
        assertFalse(analysis.canAnalyze(pn));
    }

    @Test
    public void transientTree() {
        BigDecimal step = new BigDecimal("0.01");
        TreeTransient analysis = TreeTransient.builder()
                .greedyPolicy(new BigDecimal("5"), new BigDecimal("1e-6"))
                .timeStep(step)
                .build();
        assertFalse(analysis.canAnalyze(pn));
    }

    @Test
    public void transientOneGen() {
        BigDecimal step = new BigDecimal("0.01");
        OneGenTransient analysis = OneGenTransient.builder()
                .timeBound(new BigDecimal("5"))
                .timeStep(step)
                .error(new BigDecimal("0.001"))
                .build();
        assertFalse(analysis.canAnalyze(pn));
    }
}

