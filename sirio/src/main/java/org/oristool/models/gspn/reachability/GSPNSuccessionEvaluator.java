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

package org.oristool.models.gspn.reachability;

import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.state.State;
import org.oristool.math.function.EXP;
import org.oristool.models.pn.PetriTokensAdder;
import org.oristool.models.pn.PetriTokensRemover;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;

/**
 * Succession evaluator for GSPN states.
 *
 * <p>For vanishing states, the firing probability of an IMM transition is
 * evaluated using the values of weights in the current marking. Priorities
 * among IMM transitions are properly accounted for.
 *
 * <p>For tangible states, firing probabilities of EXP transitions are evaluated
 * using the their static rates, multiplied by the value of their execution rate
 * in the current marking.
 *
 * <p>Events with zero probability are not generated.
 */
class GSPNSuccessionEvaluator
        implements SuccessionEvaluator<PetriNet, Transition> {

    @Override
    public Succession computeSuccession(PetriNet pn, State s, Transition t) {
        SPNState f = s.getFeature(SPNState.class);
        Marking m = f.state();

        Pair<Set<Transition>, Set<Transition>> enabled = enabledTransitions(m, pn);
        Set<Transition> imm = enabled.first();
        Set<Transition> exp = enabled.second();

        double prob = 0.0;
        if (!imm.isEmpty()) {
            if (!imm.contains(t))
                return null;

            prob = weight(m, t) / exitWeight(m, imm);

        } else if (!exp.isEmpty()) {
            if (!exp.contains(t))
                return null;

            double exitRate = f.exitRate();
            assert exitRate < Double.POSITIVE_INFINITY;

            prob = rate(m, t) / exitRate;
        }

        if (prob == 0.0)
            return null;

        Marking nextMarking = new Marking(m);
        new PetriTokensRemover().update(nextMarking, pn, t);
        new PetriTokensAdder().update(nextMarking, pn, t);
        if (t.hasFeature(PostUpdater.class))
            t.getFeature(PostUpdater.class).update(nextMarking, pn, t);

        double nextExitRate = exitRate(nextMarking, pn);

        State nextState = new State();
        nextState.addFeature(new SPNState(nextMarking, nextExitRate));

        Succession succ = new Succession(s, t, nextState);
        succ.addFeature(new FiringProbability(prob));
        return succ;
    }

    static Pair<Set<Transition>, Set<Transition>>
            enabledTransitions(Marking m, PetriNet pn) {

        Set<Transition> enabled = pn.getEnabledTransitions(m);

        Set<Transition> imm = new LinkedHashSet<>();
        Set<Transition> exp = new LinkedHashSet<>();
        int requiredPriority = Integer.MIN_VALUE;

        for (Transition t : enabled) {
            StochasticTransitionFeature f = t.getFeature(StochasticTransitionFeature.class);

            if (f.isEXP()) {
                if (f.clockRate().evaluate(m) > 0.0)
                    exp.add(t);

            } else if (f.isIMM()) {
                if (f.weight().evaluate(m) > 0.0) {
                    Priority p = t.getFeature(Priority.class);
                    int priority = p == null ? Integer.MIN_VALUE : p.value();

                    if (priority > requiredPriority) {
                        imm.clear();
                        requiredPriority = priority;
                    }

                    if (priority == requiredPriority)
                        imm.add(t);
                }

            } else {
                throw new IllegalStateException(
                        "In GSPNs, transitions should either be EXP or IMM");
            }
        }

        return Pair.of(imm, exp);
    }

    static double exitRate(Marking m, PetriNet pn) {
        Pair<Set<Transition>, Set<Transition>> enabled = enabledTransitions(m, pn);
        Set<Transition> imm = enabled.first();
        Set<Transition> exp = enabled.second();

        if (!imm.isEmpty())
            return Double.POSITIVE_INFINITY;
        else
            return exitRate(m, exp);
    }

    private static double exitRate(Marking m, Set<Transition> exp) {

        double exitRate = 0.0;
        for (Transition t : exp) {
            exitRate += rate(m, t);
        }

        return exitRate;
    }

    private static double rate(Marking m, Transition t) {
        StochasticTransitionFeature f =
                t.getFeature(StochasticTransitionFeature.class);
        double lambda = ((EXP)f.density()).getLambda().doubleValue();
        double scalingRate = f.clockRate().evaluate(m);
        return lambda * scalingRate;
    }

    private static double exitWeight(Marking m, Set<Transition> imm) {

        double exitWeight = 0.0;
        for (Transition t : imm) {
            StochasticTransitionFeature f =
                    t.getFeature(StochasticTransitionFeature.class);
            double weight = f.weight().evaluate(m);
            exitWeight += weight;
        }

        return exitWeight;
    }

    private static double weight(Marking m, Transition t) {
        return t.getFeature(StochasticTransitionFeature.class).weight().evaluate(m);
    }
}
