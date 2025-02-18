package org.oristool.models.gspn.client;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.stpn.*;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.*;
import org.oristool.util.Pair;


import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("slow")
public class ImmRemovalTest {
    private Marking immAfterArrivalMarking(int out, int arrived, int inService, PetriNet pn) {
        Marking m = new Marking();
        m.setTokens(pn.getPlace("out"), out);
        m.setTokens(pn.getPlace("arrived"), arrived);
        m.setTokens(pn.getPlace("inService"), inService);
        return m;
    }

    @Test
    public void immAfterArrival() {
        PetriNet pn = new PetriNet();

        Place out = pn.addPlace("out");
        Place arrived = pn.addPlace("arrived");
        Place inService = pn.addPlace("inService");

        Transition accept = pn.addTransition("accept");
        Transition arrival = pn.addTransition("arrival");
        Transition service = pn.addTransition("service");

        pn.addPrecondition(out, arrival);
        pn.addPostcondition(arrival, arrived);
        pn.addPrecondition(arrived, accept);
        pn.addPostcondition(accept, inService);
        pn.addPrecondition(inService, service);
        pn.addPostcondition(service, out);

        accept.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ZERO));
        arrival.addFeature(StochasticTransitionFeature.newExponentialInstance("0.5"));
        service.addFeature(StochasticTransitionFeature.newExponentialInstance("2"));

        Marking m_3out = immAfterArrivalMarking(3, 0, 0, pn);
        Marking m_2out_1arrived = immAfterArrivalMarking(2, 1, 0, pn);
        Marking m_2out_1inService = immAfterArrivalMarking(2, 0, 1, pn);
        Marking m_1out_1arrived_1inService = immAfterArrivalMarking(1, 1, 1, pn);
        Marking m_1out_2inService = immAfterArrivalMarking(1, 0, 2, pn);
        Marking m_1arrived_2inService = immAfterArrivalMarking(0, 1, 2, pn);
        Marking m_3inService = immAfterArrivalMarking(0, 0, 3, pn);

        Map<Marking, Double> expected = Map.of(
            m_3out, 0.7529411764705883,
            m_2out_1arrived, 0.0,
            m_2out_1inService, 0.18823529411764706,
            m_1out_1arrived_1inService, 0.0,
            m_1out_2inService, 0.04705882352941172,
            m_1arrived_2inService, 0.0,
            m_3inService, 0.0117647058823529
        );

        Map<Marking, Double> dist = GSPNSteadyState.builder().build().compute(pn, m_3out);

        assertEquals(expected.values().stream().filter(x -> x > 0.0).count(), dist.size());
        for (Marking m : dist.keySet()) {
            assertEquals(expected.get(m), dist.get(m), 1e-9);
        }
    }

    private Transition addTransition(int x1, int x2, PetriNet pn) {
        char[] letters = "abcdefghijklmnopqrstuvwxyz".toCharArray();;
        String name = String.format("t_%d_%d", x1, x2);
        int i = 1;
        while (pn.getTransition(name) != null) {
            name = String.format("t_%d_%d_%c", x1, x2, letters[i++]);
        }
        return pn.addTransition(name);
    }

    private void addExp(int x1, int x2, String rate, PetriNet pn) {
        Transition t = addTransition(x1, x2, pn);
        t.addFeature(StochasticTransitionFeature.newExponentialInstance(rate));
        t.addFeature(new EnablingFunction(String.format("x == %d", x1)));
        t.addFeature(new PostUpdater(String.format("x = %d", x2), pn));
    }

    private void addImm(int x1, int x2, String prob, PetriNet pn) {
        Transition t = addTransition(x1, x2, pn);
        t.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ZERO, MarkingExpr.from(prob, pn)));
        t.addFeature(new EnablingFunction(String.format("x == %d", x1)));
        t.addFeature(new PostUpdater(String.format("x = %d", x2), pn));
    }

    @Test
    public void complexImmRemoval() {
        // a complex example with:
        // - multiple transitions between states
        // - tangible states connected to different groups of vanishing states
        // - tangible states that are entry and exit points at the same time
        // - direct transitions between entry and exit tangible states
        // illustration: https://imgur.com/a/vkbW9SX

        PetriNet pn = new PetriNet();
        Place x = pn.addPlace("x");

        addExp(9, 10, "1.0", pn);
        addImm(10, 1, "1.0", pn);
        addExp(12, 3, "0.5", pn);

        addExp(13, 3, "3.0", pn);
        addImm(15, 13, "0.7", pn);
        addImm(15, 14, "0.3", pn);
        addImm(14, 15, "1.0", pn);
        addExp(8, 14, "4.0", pn);

        addExp(1, 10, "2.0", pn);
        addExp(1, 7, "0.5", pn);
        addExp(1, 4, "0.5", pn);
        addExp(1, 4, "2.5", pn);

        addExp(2, 11, "1.5", pn);
        addExp(2, 11, "3.5", pn);

        addExp(3, 13, "2.0", pn);
        addExp(3, 14, "2.0", pn);
        addExp(3, 6, "2.0", pn);
        addExp(3, 4, "2.0", pn);

        addImm(4, 1, "0.5", pn);
        addImm(4, 7, "0.3", pn);
        addImm(4, 5, "0.2", pn);

        addImm(5, 4, "0.2", pn);
        addImm(5, 4, "0.2", pn);
        addImm(5, 8, "0.3", pn);
        addImm(5, 8, "0.3", pn);

        addImm(6, 5, "0.9", pn);
        addImm(6, 5, "0.1", pn);

        Marking m = new Marking();
        m.setTokens(x, 12);

        // TRANSIENT
        BigDecimal timeBound = new BigDecimal("20");
        BigDecimal timeStep = new BigDecimal("0.01");
        BigDecimal error = new BigDecimal("0.00001");
        RegTransient analysis = RegTransient.builder()
                .timeBound(timeBound)
                .timeStep(timeStep)
                .greedyPolicy(timeBound, error)
                .build();

        TransientSolution<DeterministicEnablingState, Marking> regTransient =
                analysis.compute(pn, m);

        double step = 0.01;
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
                .timePoints(0.0, 20.0, step)
                .error(1e-9)
                .build().compute(pn, m);

        Map<Marking, Integer> stateIndex = result.first();
        double[][] probs = result.second();

        int regStartIdx = regTransient.getRegenerations().indexOf(regTransient.getInitialRegeneration());
        for (int t = 0; t < probs.length; t++) {
            double time = t * step;
            for (Marking i : stateIndex.keySet()) {
                int regMarkingIdx = regTransient.getColumnStates().indexOf(i);
                assertEquals(regTransient.getSolution()[t][regStartIdx][regMarkingIdx],
                        probs[t][stateIndex.get(i)], 1e-2);
            }
        }

        // STEADY STATE
        Map<Marking, Double> dist = GSPNSteadyState.builder().build().compute(pn, m);
        Marking x7 = new Marking();
        x7.setTokens(x, 7);
        assertEquals(Map.of(x7, 1.0), dist);
    }

    @Test
    public void complexImmRemovalVanishingInitial() {
        // same example but starting from a vanishing state

        PetriNet pn = new PetriNet();
        Place x = pn.addPlace("x");

        addExp(9, 10, "1.0", pn);
        addImm(10, 1, "1.0", pn);
        addExp(12, 3, "0.5", pn);

        addExp(13, 3, "3.0", pn);
        addImm(15, 13, "0.7", pn);
        addImm(15, 14, "0.3", pn);
        addImm(14, 15, "1.0", pn);
        addExp(8, 14, "4.0", pn);

        addExp(1, 10, "2.0", pn);
        addExp(1, 7, "0.5", pn);
        addExp(1, 4, "0.5", pn);
        addExp(1, 4, "2.5", pn);

        addExp(2, 11, "1.5", pn);
        addExp(2, 11, "3.5", pn);

        addExp(3, 13, "2.0", pn);
        addExp(3, 14, "2.0", pn);
        addExp(3, 6, "2.0", pn);
        addExp(3, 4, "2.0", pn);

        addImm(4, 1, "0.5", pn);
        addImm(4, 7, "0.3", pn);
        addImm(4, 5, "0.2", pn);

        addImm(5, 4, "0.2", pn);
        addImm(5, 4, "0.2", pn);
        addImm(5, 8, "0.3", pn);
        addImm(5, 8, "0.3", pn);

        addImm(6, 5, "0.9", pn);
        addImm(6, 5, "0.1", pn);

        Marking m = new Marking();
        m.setTokens(x, 5);

        // STEADY STATE
        Map<Marking, Double> dist = GSPNSteadyState.builder().build().compute(pn, m);
        Marking x7 = new Marking();
        x7.setTokens(x, 7);
        assertEquals(Map.of(x7, 1.0), dist);
    }
}
