package org.oristool.models.stpn.client;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oristool.analyzer.stop.AlwaysFalseStopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.TransientSolutionViewer;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.stpn.trees.TruncationPolicy;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

class RejuvenationTest {

    private PetriNet pn;
    private Marking marking;

    @BeforeEach
    void buildModel() {
        pn = new PetriNet();
        
        Place Clock = pn.addPlace("Clock");
        Place Detected = pn.addPlace("Detected");
        Place Err = pn.addPlace("Err");
        Place Ko = pn.addPlace("Ko");
        Place Ok = pn.addPlace("Ok");
        Place Rej = pn.addPlace("Rej");
        
        Transition detect = pn.addTransition("detect");
        Transition error = pn.addTransition("error");
        Transition fail = pn.addTransition("fail");
        Transition rejFromErr = pn.addTransition("rejFromErr");
        Transition rejFromOk = pn.addTransition("rejFromOk");
        Transition repair = pn.addTransition("repair");
        Transition waitClock = pn.addTransition("waitClock");
        
        pn.addInhibitorArc(Rej, fail);
        pn.addInhibitorArc(Detected, waitClock);
        pn.addInhibitorArc(Rej, error);
        pn.addPrecondition(Rej, rejFromErr);
        pn.addPostcondition(error, Err);
        pn.addPrecondition(Err, fail);
        pn.addPrecondition(Err, rejFromErr);
        pn.addPostcondition(rejFromOk, Clock);
        pn.addPostcondition(rejFromOk, Ok);
        pn.addPrecondition(Clock, waitClock);
        pn.addPostcondition(repair, Ok);
        pn.addPrecondition(Detected, repair);
        pn.addPostcondition(waitClock, Rej);
        pn.addPrecondition(Ok, rejFromOk);
        pn.addPrecondition(Ko, detect);
        pn.addPostcondition(rejFromErr, Ok);
        pn.addPrecondition(Ok, error);
        pn.addPostcondition(rejFromErr, Clock);
        pn.addPostcondition(detect, Detected);
        pn.addPostcondition(fail, Ko);
        pn.addPrecondition(Rej, rejFromOk);
       
        detect.addFeature(StochasticTransitionFeature.newUniformInstance("0","5"));
        error.addFeature(StochasticTransitionFeature.newErlangInstance(2, new BigDecimal("0.02")));
        fail.addFeature(StochasticTransitionFeature.newErlangInstance(2, new BigDecimal("0.01")));
        rejFromErr.addFeature(StochasticTransitionFeature.newUniformInstance("0", "5"));
        rejFromOk.addFeature(StochasticTransitionFeature.newUniformInstance("0", "5"));
        repair.addFeature(new PostUpdater("Clock=1, Rej=0", pn));
        repair.addFeature(StochasticTransitionFeature.newUniformInstance("0", "25"));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance("150"));

        marking = new Marking();
        marking.setTokens(Clock, 1);
        marking.setTokens(Ok, 1);
    }

    @Test
    void regTransient() {
        BigDecimal timeBound = new BigDecimal("1500");
        BigDecimal timeStep = new BigDecimal("1");
        BigDecimal error = new BigDecimal("0");

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

        assertTrue(result.sumsToOne(1e-8));

        // result.writeCSV("src/test/resources/Rejuvenation_regTransient.csv", 9);
        TransientSolution<String,String> expected = 
                TransientSolution.readCSV("src/test/resources/Rejuvenation_regTransient.csv");
        assertTrue(result.isClose(expected, 1e-9));
        
        // try { new TransientSolutionViewer(result); Thread.sleep(100000); } catch (InterruptedException e) { }
	}

    @Test
    void regTransientFilter() {
    	RewardRate[] rewardRates = TransientSolution.rewardRates("Ko; If(Ko+Detected>0 || Rej>0,1,0); If(Ok>0 && Rej>0,1,0)");
    	
        BigDecimal timeBound = new BigDecimal("1500");
        BigDecimal timeStep = new BigDecimal("1");
        BigDecimal error = new BigDecimal("0");

        RegTransient.Builder builder = RegTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);
        builder.markingFilter(RewardRate.nonZero(0.0, rewardRates));
        RegTransient analysis = builder.build();

        // check that parameters are set correctly
        assertEquals(timeBound, analysis.timeBound());
        assertEquals(timeStep, analysis.timeStep());
        TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
        assertEquals(p.getEpsilon(), error);
        assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));
        assertEquals(AlwaysFalseStopCriterion.class, analysis.stopOn().get().getClass());

        // compute result
        TransientSolution<DeterministicEnablingState, Marking> result =
                analysis.compute(pn, marking);

        // result.writeCSV("src/test/resources/Rejuvenation_regTransientFilter.csv", 9);
        TransientSolution<String,String> expected = 
                TransientSolution.readCSV("src/test/resources/Rejuvenation_regTransientFilter.csv");
        assertTrue(result.isClose(expected, 1e-9));
        
        TransientSolution<DeterministicEnablingState, RewardRate> rewards = 
        		TransientSolution.computeRewards(false, result, rewardRates);

        // rewards.writeCSV("src/test/resources/Rejuvenation_regTransientFilterRewards.csv", 9);
        TransientSolution<String,String> expectedRewards = 
                TransientSolution.readCSV("src/test/resources/Rejuvenation_regTransientFilterRewards.csv");
        assertTrue(rewards.isClose(expectedRewards, 1e-9));

        // try { new TransientSolutionViewer(rewards); Thread.sleep(100000); } catch (InterruptedException e) { }
	}

    @Test
    void regTransientLocalPeriod() {
        BigDecimal timeBound = new BigDecimal("5000");
        BigDecimal timeStep = new BigDecimal("1");
        BigDecimal error = new BigDecimal("0");
        int localEvaluationPeriod = 10;
        int globalEvaluationPeriod = 1;

        RegTransient.Builder builder = RegTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);
        builder.localEvaluationPeriod(localEvaluationPeriod);
        builder.globalEvaluationPeriod(globalEvaluationPeriod);

        RegTransient analysis = builder.build();

        // check that parameters are set correctly
        assertEquals(timeBound, analysis.timeBound());
        assertEquals(timeStep, analysis.timeStep());
        TruncationPolicy p = (TruncationPolicy) analysis.policy().get();
        assertEquals(p.getEpsilon(), error);
        assertEquals(p.getTauAgeLimit(), new OmegaBigDecimal(timeBound));
        assertEquals(AlwaysFalseStopCriterion.class, analysis.stopOn().get().getClass());
        assertEquals(localEvaluationPeriod, analysis.localEvaluationPeriod());
        assertEquals(globalEvaluationPeriod, analysis.globalEvaluationPeriod());

        // compute and check result
        TransientSolution<DeterministicEnablingState, Marking> result =
                analysis.compute(pn, marking);

        // assertTrue(result.sumsToOne(0.5));

        // result.writeCSV("src/test/resources/Rejuvenation_regTransientLocalPeriod.csv", 9);
        TransientSolution<String,String> expected =
                TransientSolution.readCSV("src/test/resources/Rejuvenation_regTransientLocalPeriod.csv");
        assertTrue(result.isClose(expected, 1e-9));

        // try { new TransientSolutionViewer(result); Thread.sleep(100000); } catch (InterruptedException e) { }
	}
}
