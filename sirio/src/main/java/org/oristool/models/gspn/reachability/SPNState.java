/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2019 The ORIS Authors.
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

import org.oristool.analyzer.state.StateFeature;
import org.oristool.models.gspn.chains.CTMCState;
import org.oristool.petrinet.Marking;

/**
 * A GSPN state, including a sojourn rate and a marking.
 *
 * <p>Only the marking is used in object comparisons. As a consequence, when
 * this class is used as node of a graph, there exists at most one edge between
 * two markings.
 *
 * <p>A rate equal to {@code Double.POSITIVE_INFINITY} is used for vanishing
 * states (where immediate transitions are enabled).
 */
public final class SPNState implements StateFeature, CTMCState<Marking> {

    private final Marking marking;
    private final double exitRate;

    /**
     * Creates a new instance with a given marking and exit rate.
     *
     * @param marking marking of this state
     * @param exitRate total exit rate
     */
    public SPNState(Marking marking, double exitRate) {

        this.marking = marking;
        this.exitRate = exitRate;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (!(other instanceof SPNState))
            return false;

        SPNState o = (SPNState) other;
        return this.marking.equals(o.marking);
    }

    @Override
    public Marking state() {
        return marking;
    }

    @Override
    public double exitRate() {
        return exitRate;
    }

    @Override
    public int hashCode() {
        return this.marking.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Marking: ");
        b.append(marking);
        b.append("\n");
        b.append("Exit rate: ");
        b.append(exitRate < Double.POSITIVE_INFINITY
                ? String.format("%.3f\n", exitRate) : "IMM\n");
        return b.toString();
    }
}
