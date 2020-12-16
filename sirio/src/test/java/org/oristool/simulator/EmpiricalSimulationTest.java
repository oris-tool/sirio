package org.oristool.simulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.models.stpn.trees.EmpiricalTransitionFeature;
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

public class EmpiricalSimulationTest {
    @Test
    void simulateFromHistogram(){

        // Test Histograms
        ArrayList<BigDecimal> hCDF0 = new ArrayList<>(Arrays.asList(BigDecimal.valueOf(0.019117),
                BigDecimal.valueOf(0.268521), BigDecimal.valueOf(0.333222), BigDecimal.valueOf(0.333539),
                BigDecimal.valueOf(0.345689), BigDecimal.valueOf(0.466058), BigDecimal.valueOf(0.636494),
                BigDecimal.valueOf(0.685945), BigDecimal.valueOf(0.730779), BigDecimal.valueOf(0.808247),
                BigDecimal.valueOf(0.896465), BigDecimal.valueOf(0.959449), BigDecimal.valueOf(0.989),
                BigDecimal.valueOf(0.997867), BigDecimal.valueOf(0.999733), BigDecimal.valueOf(1.)));

        BigDecimal low0 = new BigDecimal("1.99664");
        BigDecimal upp0 = new BigDecimal("20.2561");


        ArrayList<BigDecimal> hCDF1 = new ArrayList<>(Arrays.asList(BigDecimal.valueOf(0.00623931),
                BigDecimal.valueOf(0.0187682), BigDecimal.valueOf(0.0454362), BigDecimal.valueOf(0.0876019),
                BigDecimal.valueOf(0.153718), BigDecimal.valueOf(0.24917), BigDecimal.valueOf(0.368522),
                BigDecimal.valueOf(0.502969), BigDecimal.valueOf(0.63485), BigDecimal.valueOf(0.751434),
                BigDecimal.valueOf(0.846986), BigDecimal.valueOf(0.913555), BigDecimal.valueOf(0.956526),
                BigDecimal.valueOf(0.98088), BigDecimal.valueOf(0.993912), BigDecimal.valueOf(1.)));

        BigDecimal low1 = new BigDecimal("2.66308");
        BigDecimal upp1 = new BigDecimal("5.18423");

        // Creating PetriNet
        PetriNet pn = new PetriNet();
        Place p0 = pn.addPlace("p0");
        Place p1 = pn.addPlace("p1");
        Place p2 = pn.addPlace("p2");

        Transition t0 = pn.addTransition("t0");
        Transition t1 = pn.addTransition("t1");

        t0.addFeature(EmpiricalTransitionFeature.newInstance(hCDF0, low0, upp0));
        t0.addFeature(StochasticTransitionFeature.newUniformInstance(low0, upp0));
        t1.addFeature(EmpiricalTransitionFeature.newInstance(hCDF1, low1, upp1));
        t1.addFeature(StochasticTransitionFeature.newUniformInstance(low1, upp1));


        pn.addPrecondition(p0, t0);
        pn.addPostcondition(t0, p1);
        pn.addPrecondition(p1, t1);
        pn.addPostcondition(t1, p2);

        Marking m = new Marking();
        m.addTokens(p0, 1);
        m.addTokens(p1, 0);
        m.addTokens(p2, 0);

        BigDecimal timeLimit = new BigDecimal(30);
        BigDecimal timeStep = new BigDecimal("0.1");
        int timePoints = (timeLimit.divide(timeStep)).intValue() + 1;
        long runs = 50000;

        Sequencer s = new Sequencer(pn, m,
                new STPNSimulatorComponentsFactory(), NoOpLogger.INSTANCE);

        TransientMarkingConditionProbability r1 =
                new TransientMarkingConditionProbability(s,
                        new ContinuousRewardTime(timeStep), timePoints,
                        MarkingCondition.fromString("p2"));
        RewardEvaluator re1 = new RewardEvaluator(r1, runs);

        s.simulate();
        TimeSeriesRewardResult result = (TimeSeriesRewardResult) re1.getResult();
        Assert.assertTrue(result.isValid(BigDecimal.valueOf(3)));
    }
}
