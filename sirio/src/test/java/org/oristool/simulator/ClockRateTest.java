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

package org.oristool.simulator;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.rewards.ContinuousRewardTime;
import org.oristool.simulator.rewards.RewardEvaluator;
import org.oristool.simulator.stpn.STPNSimulatorComponentsFactory;
import org.oristool.simulator.stpn.TransientMarkingConditionProbability;

/**
 * Test STPNs with multiple rates.
 */
class ClockRateTest {

    @Test
    void detUnif() {
        PetriNet pn = new PetriNet();
        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        
        Transition t0 = pn.addTransition("t0");
        Transition t1 = pn.addTransition("t1");
        
        t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ONE));
        t1.addFeature(StochasticTransitionFeature.newUniformInstance(
                new BigDecimal(2), new BigDecimal(4), MarkingExpr.of(2)));
        
        pn.addPrecondition(p0, t0);
        pn.addPrecondition(p1, t1);
        
        Marking m = new Marking();
        m.addTokens(p0, 1);
        m.addTokens(p1, 1);

        BigDecimal timeLimit = new BigDecimal(4);
        BigDecimal timeStep = new BigDecimal("0.1");
        int timePoints = (timeLimit.divide(timeStep)).intValue() + 1;
        long runs = 50000;

        Sequencer s = new Sequencer(pn, m, new STPNSimulatorComponentsFactory(), NoOpLogger.INSTANCE);
        
        TransientMarkingConditionProbability r1 = 
                new TransientMarkingConditionProbability(s, 
                        new ContinuousRewardTime(timeStep), timePoints, 
                        MarkingCondition.fromString("p1"));
        RewardEvaluator re1 = new RewardEvaluator(r1, runs);    
        
        s.simulate();
        TimeSeriesRewardResult result = (TimeSeriesRewardResult) re1.getResult();
        
        m.removeTokens(p0, 1);
        int a = 10;
        int b = 20;
        for (int t = 0; t < timePoints; t++) {
            double expected = (t < a || t > b ? 0.0 : 1-(1.0*t-a)/(b-a));
            assertEquals(expected, result.getTimeSeries(m)[t].doubleValue(), 0.01);
        }            
    }

}
