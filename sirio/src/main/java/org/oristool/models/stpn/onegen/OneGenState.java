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

package org.oristool.models.stpn.onegen;

import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.math.function.EXP;
import org.oristool.models.gspn.chains.CTMCState;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.Transition;

/**
 * CTMC state with reference to a node in the succession graph.
 */
public class OneGenState implements CTMCState<State> {
    private final State state;
    private final double exitRate;

    /**
     * Creates a new instance from a graph node.
     *
     * <p>The marking is obtained from the {@link PetriStateFeature} of the state,
     * while the exit rate is computed from the static features of enabled
     * transitions.
     *
     * @param state node in the succession graph
     * @throws IllegalArgumentException if more then one GEN transition or IMM
     *         transitions are enabled in the state.
     */
    public OneGenState(State state) {

        this.state = state;
        this.exitRate = exitRate(state,
                state.getFeature(PetriStateFeature.class).getEnabled());
    }

    static double rate(State s, Transition t) {
        Marking m = s.getFeature(PetriStateFeature.class).getMarking();
        StochasticTransitionFeature f =
                t.getFeature(StochasticTransitionFeature.class);
        double lambda = ((EXP)f.density()).getLambda().doubleValue();
        double scalingRate = f.rate().evaluate(m);
        return lambda * scalingRate;
    }

    static double exitRate(State s, Set<Transition> enabled) {
        double exitRate = 0.0;
        boolean genFound = false;

        for (Transition t : enabled) {
            StochasticTransitionFeature f = t.getFeature(StochasticTransitionFeature.class);

            if (f.isEXP()) {
                exitRate += rate(s, t);
            } else if (f.isIMM()) {
                throw new IllegalArgumentException("The state is vanishing");
            } else {
                if (!genFound)
                    genFound = true;
                else
                    throw new IllegalArgumentException("More than one GEN enabled");
            }
        }

        return exitRate;
    }

    @Override
    public double exitRate() {
        return exitRate;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (!(other instanceof OneGenState))
            return false;

        OneGenState o = (OneGenState) other;
        return this.state.equals(o.state);
    }

    @Override
    public int hashCode() {
        return this.state.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("State: ");
        b.append(state);
        b.append("\n");
        b.append("Exit rate: ");
        b.append(exitRate < Double.POSITIVE_INFINITY
                ? String.format("%.3f\n", exitRate) : "IMM\n");
        return b.toString();
    }
}
