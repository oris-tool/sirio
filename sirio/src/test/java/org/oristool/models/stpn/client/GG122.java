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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.stpn.trees.TruncationPolicy;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A simple STPN test analyzing a GG122 queue with preemption.
 */
class GG122 {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();

        Place p1 = pn.addPlace("p1");
        Place p2 = pn.addPlace("p2");
        Place p3 = pn.addPlace("p3");
        Place p4 = pn.addPlace("p4");
        
        
        Transition t1 = pn.addTransition("t1");
        Transition t2 = pn.addTransition("t2");
        Transition t3 = pn.addTransition("t3");
        Transition t4 = pn.addTransition("t4");
        
        t1.addFeature(StochasticTransitionFeature.newUniformInstance("1", "2"));
        t2.addFeature(StochasticTransitionFeature.newUniformInstance("1", "2"));
        t3.addFeature(StochasticTransitionFeature.newUniformInstance("1", "2"));
        t4.addFeature(StochasticTransitionFeature.newUniformInstance("1", "2"));

        pn.addPrecondition(p1, t1);
        pn.addPostcondition(t1, p2);
        pn.addPrecondition(p2, t2);
        pn.addPostcondition(t2, p1);

        pn.addPrecondition(p3, t3);
        pn.addPostcondition(t3, p4);
        pn.addPrecondition(p4, t4);
        pn.addPostcondition(t4, p3);

        pn.addInhibitorArc(p4, t2);
        
        marking = new Marking();
        marking.setTokens(p1, 1);
        marking.setTokens(p3, 1);
    }

    @Test
    void treeTransient() {

        BigDecimal timeBound = new BigDecimal("12");
        BigDecimal timeStep = new BigDecimal("0.1");
        BigDecimal error = new BigDecimal("0.001");

        TreeTransient.Builder builder = TreeTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);

        TreeTransient analysis = builder.build();

        // check that parameters are set correctly
        assertEquals(timeBound, analysis.timeBound());
        assertEquals(timeStep, analysis.timeStep());
        TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
        assertEquals(p.getEpsilon(), error);
        assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));
        assertEquals(AlwaysFalseStopCriterion.class, analysis.stopOn().get().getClass());

        // compute and check result
        TransientSolution<Marking, Marking> result =
                analysis.compute(pn, marking);

        assertTrue(result.sumsToOne(1e-3));

        // result.writeCSV("src/test/resources/GG122_treeTransient.csv", 9);
        TransientSolution<String,String> expected = 
                TransientSolution.readCSV("src/test/resources/GG122_treeTransient.csv");
        assertTrue(result.isClose(expected, 1e-9));
        
        // try { new TransientSolutionViewer(result); Thread.sleep(100000); } catch (InterruptedException e) { }
    }

    @Test
    void regTransient() {

        BigDecimal timeBound = new BigDecimal("12");
        BigDecimal timeStep = new BigDecimal("0.1");
        BigDecimal error = new BigDecimal("0.00001");

        RegTransient.Builder builder = RegTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);

        RegTransient analysis = builder.build();

        // check that parameters are set correctly
        assertEquals(timeBound, analysis.timeBound());
        assertEquals(timeStep, analysis.timeStep());
        TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
        assertEquals(p.getEpsilon(), error);
        assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));
        assertEquals(AlwaysFalseStopCriterion.class, analysis.stopOn().get().getClass());

        // compute and check result
        TransientSolution<DeterministicEnablingState, Marking> result =
                analysis.compute(pn, marking);

        assertTrue(result.sumsToOne(1e-5));

        // result.writeCSV("src/test/resources/GG122_regTransient.csv", 9);
        TransientSolution<String,String> expected = 
                TransientSolution.readCSV("src/test/resources/GG122_regTransient.csv");
        assertTrue(result.isClose(expected, 1e-9));
        
        // try { new TransientSolutionViewer(result); Thread.sleep(100000); } catch (InterruptedException e) { }
    }
    
    @Test
    void regTransient_kernelPeriod() {

        BigDecimal timeBound = new BigDecimal("6");
        BigDecimal timeStep = new BigDecimal("0.01");
        BigDecimal error = new BigDecimal("0.00001");

        RegTransient.Builder builder = RegTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);
        builder.localEvaluationPeriod(4);
        builder.globalEvaluationPeriod(4);
        
        RegTransient analysis = builder.build();

        // check that parameters are set correctly
        assertEquals(timeBound, analysis.timeBound());
        assertEquals(timeStep, analysis.timeStep());
        TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
        assertEquals(p.getEpsilon(), error);
        assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));
        assertEquals(AlwaysFalseStopCriterion.class, analysis.stopOn().get().getClass());

        // compute and check result
        TransientSolution<DeterministicEnablingState, Marking> result =
                analysis.compute(pn, marking);

        assertTrue(result.sumsToOne(1e-5));

        // result.writeCSV("src/test/resources/GG122_regTransient_kernelPeriod.csv", 9);
        TransientSolution<String,String> expected = 
                TransientSolution.readCSV("src/test/resources/GG122_regTransient_kernelPeriod.csv");
        assertTrue(result.isClose(expected, 1e-6));
        
        // try { new TransientSolutionViewer(result); Thread.sleep(100000); } catch (InterruptedException e) { }
    }
}
