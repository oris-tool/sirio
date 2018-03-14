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

package org.oristool.models.tpn;

import java.util.HashSet;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Variable;
import org.oristool.models.pn.MarkingUpdater;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.PetriSuccessionEvaluator;
import org.oristool.models.pn.Priority;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Builder of successor states for time Petri nets.
 */
public final class TimedSuccessionEvaluator implements
        SuccessionEvaluator<PetriNet, Transition> {

    private PetriSuccessionEvaluator petriSuccessionEvaluator;
    private boolean excludeZeroProb;

    /**
     * Builds a succession evaluator with a given token remover and adder.
     *
     * <p>If {@code checkNewlyEnabled} is true, states with the same marking are
     * considered different if they have a different set of newly-enabled
     * transitions.
     *
     * <p>If {@code excludeZeroProb} is true, transition firings with zero
     * probability are excluded.
     *
     * @param tokensRemover the object used to add tokens after a firing
     * @param tokensAdder the object used to remove tokens after a firing
     * @param checkNewlyEnabled whether to compare the sets of newly-enabled
     *        transitions of states
     * @param excludeZeroProb whether to exclude transition firings with zero
     *        probability
     */
    public TimedSuccessionEvaluator(MarkingUpdater tokensRemover,
            MarkingUpdater tokensAdder,
            boolean checkNewlyEnabled, boolean excludeZeroProb) {

        petriSuccessionEvaluator = new PetriSuccessionEvaluator(tokensRemover,
                tokensAdder, checkNewlyEnabled);

        this.excludeZeroProb = excludeZeroProb;
    }

    @Override
    public Succession computeSuccession(PetriNet petriNet, State state,
            Transition fired) {

        final Succession succession = petriSuccessionEvaluator.computeSuccession(
                petriNet, state, fired);
        final PetriStateFeature prevPetriStateFeature = succession.getParent()
                .getFeature(PetriStateFeature.class);
        final PetriStateFeature nextPetriStateFeature = succession.getChild()
                .getFeature(PetriStateFeature.class);
        TimedStateFeature prevTimedStateFeature = succession.getParent()
                .getFeature(TimedStateFeature.class);

        final Set<Variable> enabled = Transition
                .newVariableSetInstance(prevPetriStateFeature.getEnabled());

        Variable firedVar = new Variable(fired.toString());
        DBMZone prevDomain = prevTimedStateFeature.getDomain();

        // impose a priority-based order on the execution of immediate timers:
        // note that firedVar can have null-delay vars even if fired is
        // not deterministic (e.g. two [0,1] after the firing of [1,1])
        Set<Variable> nullDelayVariables = prevDomain.getNullDelayVariables(firedVar);
        nullDelayVariables.remove(Variable.TSTAR);
        nullDelayVariables.remove(Variable.AGE);

        if (nullDelayVariables.size() > 0) {
            Set<Transition> nullDelayTransitions = new HashSet<>();
            nullDelayTransitions.add(fired);
            for (Variable v : nullDelayVariables)
                nullDelayTransitions.add(petriNet.getTransition(v.toString()));

            Set<Transition> maxPriority = Priority.maxPriority(nullDelayTransitions);
            if (!maxPriority.contains(fired))
                return null;
        }

        if (excludeZeroProb) {
            for (Variable other : prevDomain.getVariables()) {
                if (!other.equals(firedVar)              // i != 0
                        && !other.equals(Variable.TSTAR) // i != *
                        && !other.equals(Variable.AGE)   // i != age
                        && prevDomain.getBound(other, firedVar)
                            .compareTo(OmegaBigDecimal.ZERO) == 0  // now IMM
                        && prevDomain.getBound(firedVar, other)
                            .compareTo(OmegaBigDecimal.ZERO) > 0   // not before
                        // either i or 0 was distributed
                        && (prevDomain.getBound(firedVar, Variable.TSTAR)
                                .add(prevDomain.getBound(Variable.TSTAR, firedVar))
                                .compareTo(OmegaBigDecimal.ZERO) > 0
                         || prevDomain.getBound(other, Variable.TSTAR)
                                .add(prevDomain.getBound(Variable.TSTAR, other))
                                .compareTo(OmegaBigDecimal.ZERO) > 0)) {

                    return null;
                }
            }
        }

        // conditioning
        DBMZone newDomain = new DBMZone(prevDomain);
        newDomain.imposeVarLower(firedVar, enabled);
        if (newDomain.isEmpty())
            return null;

        // shift and project
        newDomain.setNewGround(firedVar);

        // disabling
        newDomain.projectVariables(Transition
                .newVariableSetInstance(nextPetriStateFeature.getDisabled()));

        // newly enabling
        newDomain.addVariables(
                Transition.newVariableSetInstance(nextPetriStateFeature.getNewlyEnabled()));

        for (Transition t : nextPetriStateFeature.getNewlyEnabled()) {
            TimedTransitionFeature ttf = t.getFeature(TimedTransitionFeature.class);
            newDomain.setCoefficient(t.newVariableInstance(), Variable.TSTAR,
                    ttf.getLFT());
            newDomain.setCoefficient(Variable.TSTAR, t.newVariableInstance(),
                    ttf.getEFT().negate());
        }

        TimedStateFeature nextTimedStateFeature = new TimedStateFeature();
        nextTimedStateFeature.setDomain(newDomain);
        succession.getChild().addFeature(nextTimedStateFeature);

        return succession;
    }
}
