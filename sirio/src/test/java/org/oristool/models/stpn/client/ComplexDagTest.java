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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.log.PrintStreamLogger;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.stpn.trees.TruncationPolicy;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * A complex DAG with a single absorbing marking (p15).
 */
class ComplexDagTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();
        
        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        Place p2 = pn.addPlace("p2");
        Place p3 = pn.addPlace("p3");
        Place p4 = pn.addPlace("p4");
        Place p5 = pn.addPlace("p5");
        Place p6 = pn.addPlace("p6");
        Place p7 = pn.addPlace("p7");
        Place p8 = pn.addPlace("p8");
        Place p9 = pn.addPlace("p9");
        Place p10 = pn.addPlace("p10");
        Place p11 = pn.addPlace("p11");
        Place p12 = pn.addPlace("p12");
        Place p13 = pn.addPlace("p13");
        Place p14 = pn.addPlace("p14");
        Place p15 = pn.addPlace("p15");

        Transition t0 = pn.addTransition("t0");
        Transition t1 = pn.addTransition("t1");
        Transition t2 = pn.addTransition("t2");
        Transition t3 = pn.addTransition("t3");
        Transition t4 = pn.addTransition("t4");
        Transition t5 = pn.addTransition("t5");
        Transition t6 = pn.addTransition("t6");
        Transition t7 = pn.addTransition("t7");
        Transition t8 = pn.addTransition("t8");
        Transition t9 = pn.addTransition("t9");
        Transition t10 = pn.addTransition("t10");

        pn.addPostcondition(t2, p7);
        pn.addPrecondition(p7, t1);
        pn.addPrecondition(p5, t2);
        pn.addPostcondition(t10, p14);
        pn.addPostcondition(t0, p2);
        pn.addPrecondition(p8, t3);
        pn.addPrecondition(p0, t0);
        pn.addPostcondition(t6, p5);
        pn.addPostcondition(t5, p4);
        pn.addPostcondition(t3, p13);
        pn.addPrecondition(p2, t6);
        pn.addPrecondition(p14, t4);
        pn.addPostcondition(t0, p3);
        pn.addPrecondition(p6, t3);
        pn.addPostcondition(t0, p1);
        pn.addPostcondition(t2, p8);
        pn.addPrecondition(p4, t1);
        pn.addPrecondition(p1, t5);
        pn.addPostcondition(t7, p6);
        pn.addPrecondition(p11, t9);
        pn.addPostcondition(t4, p15);
        pn.addPostcondition(t2, p11);
        pn.addPrecondition(p12, t4);
        pn.addPrecondition(p13, t10);
        pn.addPrecondition(p9, t8);
        pn.addPostcondition(t1, p9);
        pn.addPostcondition(t9, p12);
        pn.addPrecondition(p3, t7);
        pn.addPostcondition(t8, p10);
        pn.addPrecondition(p10, t4);

        StochasticTransitionFeature imm = StochasticTransitionFeature.newDeterministicInstance("0");
        StochasticTransitionFeature unif01 = StochasticTransitionFeature.newUniformInstance("0",
                "1");

        t0.addFeature(imm);
        t1.addFeature(imm);
        t2.addFeature(imm);
        t3.addFeature(imm);
        t4.addFeature(imm);

        t5.addFeature(unif01);
        t6.addFeature(unif01);
        t7.addFeature(unif01);
        t8.addFeature(unif01);
        t9.addFeature(unif01);
        t10.addFeature(unif01);

        t0.addFeature(new Priority(0));
        t1.addFeature(new Priority(1));
        t2.addFeature(new Priority(2));
        t3.addFeature(new Priority(3));
        t4.addFeature(new Priority(4));

        marking = new Marking();
        marking.setTokens(p0, 1);
    }

    @Test
    void treeTransient() {

        BigDecimal timeBound = new BigDecimal("3");
        BigDecimal timeStep = new BigDecimal("0.01");
        BigDecimal error = BigDecimal.ZERO;

        TreeTransient.Builder builder = TreeTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);
        builder.evaluateByClass(true);
        TreeTransient analysis = builder.build();

        // check that parameters are set correctly
        assertEquals(timeBound, analysis.timeBound());
        assertEquals(timeStep, analysis.timeStep());
        TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
        assertEquals(p.getEpsilon(), error);
        assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));
        assertEquals(AlwaysFalseStopCriterion.class, analysis.stopOn().get().getClass());

        // compute and check result
        TransientSolution<Marking, Marking> result = analysis.compute(pn, marking);
        assertTrue(result.sumsToOne(1e-7));

        // result.writeCSV("src/test/resources/ComplexDag_treeTransient.csv", 9);
        TransientSolution<String,String> expected =
                TransientSolution.readCSV("src/test/resources/ComplexDag_treeTransient.csv");
        assertTrue(result.isClose(expected, 1e-7));

         // try {
         //     new TransientSolutionViewer(result);
         //    Thread.sleep(100000);
         // } catch (InterruptedException e) {
         // }
    }

    @Test
    void treeTransientByClass() {

        //for (int i = 0; i < 10; i++) {
            BigDecimal timeBound = new BigDecimal("3");
            BigDecimal timeStep = new BigDecimal("0.01");
            BigDecimal error = BigDecimal.ZERO;
    
            TreeTransient.Builder builder = TreeTransient.builder();
            builder.timeBound(timeBound);
            builder.timeStep(timeStep);
            builder.greedyPolicy(timeBound, error);
            builder.markingFilter(MarkingCondition.fromString("p15==1"));
            // builder.evaluateByClass(true);
            TreeTransient analysis = builder.build();
    
            // compute and check result
            // long start = System.nanoTime();
            TransientSolution<Marking, Marking> result = analysis.compute(pn, marking);
            // System.out.printf("%.2f ms\n", (System.nanoTime()-start)/1000000.0);
    
            // result.writeCSV("src/test/resources/ComplexDag_treeTransientByClass.csv", 9);
            TransientSolution<String,String> expected =
                    TransientSolution.readCSV("src/test/resources/ComplexDag_treeTransientByClass.csv");
            assertTrue(result.isClose(expected, 1e-9));
    
             // try {
             //     new TransientSolutionViewer(result);
             //    Thread.sleep(100000);
             // } catch (InterruptedException e) {
             // }
        //}
    }
    
}
