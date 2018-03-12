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

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.oristool.math.expression.Variable;
import org.oristool.math.function.EXP;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * A generalized form of regeneration including a marking and the deterministic
 * enabling times of non-exponential transitions.
 */
public final class DeterministicEnablingState {

    private final Marking marking;
    private final Map<Variable, BigDecimal> enablingTimes;

    /**
     * Builds an object encoding the deterministic enabling state of non-exponential
     * transitions. Initially, all non-exponential transitions are associated with
     * enabling time equal to zero.
     *
     * @param marking the marking of the STPN
     * @param enabledTransitions set of enabled transitions
     */
    public DeterministicEnablingState(Marking marking,
            Set<Transition> enabledTransitions) {

        this.marking = marking;
        this.enablingTimes = new LinkedHashMap<Variable, BigDecimal>();

        for (Transition t : enabledTransitions)
            if (!(t.getFeature(StochasticTransitionFeature.class)
                    .density() instanceof EXP))
                enablingTimes.put(new Variable(t.getName()), BigDecimal.ZERO);
    }

    /**
     * Builds an object encoding the deterministic enabling state of non-exponential
     * transitions. Initially, all non-exponential transitions are associated with
     * enabling time equal to zero.
     *
     * @param marking the marking of the STPN
     * @param petriNet Petri net
     */
    public DeterministicEnablingState(Marking marking, PetriNet petriNet) {

        this(marking, petriNet.getEnabledTransitions(marking));
    }

    /**
     * Builds an instance from the given marking and map of enabling times.
     *
     * <p>The input objects are assumed to be constant (no copies are made).
     *
     * @param marking the marking of the STPN
     * @param enablingTimes map of enabling times
     */

    public DeterministicEnablingState(Marking marking,
            Map<Variable, BigDecimal> enablingTimes) {

        this.marking = marking;
        this.enablingTimes = enablingTimes;
    }

    public Marking getMarking() {
        return marking;
    }

    public Map<Variable, BigDecimal> getEnablingTimes() {
        return enablingTimes;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof DeterministicEnablingState))
            return false;

        DeterministicEnablingState s = (DeterministicEnablingState) obj;

        return marking.equals(s.marking)
                && enablingTimes.equals(s.enablingTimes);
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.marking.hashCode();
        result = 31 * result + this.enablingTimes.hashCode();

        return result;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append(marking.toString());
        b.append("@ ");

        String joiner = "";
        for (Map.Entry<Variable, BigDecimal> e : enablingTimes.entrySet()) {
            b.append(joiner);
            joiner = " ";
            b.append(e.getKey());
            b.append("|");
            b.append(e.getValue());
        }

        return b.toString();
    }
}
