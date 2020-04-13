/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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

import org.oristool.math.OmegaBigDecimal;
import org.oristool.petrinet.TransitionFeature;

/**
 * Transition feature with minimum/maximum required time to the firing.
 */
public final class TimedTransitionFeature implements TransitionFeature {
    private OmegaBigDecimal eft;
    private OmegaBigDecimal lft;

    /**
     * Builds a timed feature from minimum/maximum firing times.
     *
     * @param eft earliest firing time (minimum time to fire)
     * @param lft latest firing time (maximum time to fire)
     */
    public TimedTransitionFeature(OmegaBigDecimal eft, OmegaBigDecimal lft) {

        if (eft.compareTo(OmegaBigDecimal.ZERO) < 0
                || eft.compareTo(OmegaBigDecimal.POSITIVE_INFINITY) == 0)
            throw new IllegalArgumentException(
                    "The earliest firing time must be a finite non-negative number");

        if (lft.compareTo(eft) < 0)
            throw new IllegalArgumentException(
                    "Latest firing time must be a greater or equal to the earliest firing time");

        this.eft = eft;
        this.lft = lft;
    }

    /**
     * Builds a timed feature from minimum/maximum firing times specified as
     * strings. The strings must be valid inputs to the constructor of
     * {@link OmegaBigDecimal}.
     *
     * @param eft earliest firing time (minimum time to fire)
     * @param lft latest firing time (maximum time to fire)
     */
    public TimedTransitionFeature(String eft, String lft) {
        this(new OmegaBigDecimal(eft), new OmegaBigDecimal(lft));
    }

    public boolean isDeterministic() {
        return eft.equals(lft);
    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof TimedTransitionFeature))
            return false;

        TimedTransitionFeature o = (TimedTransitionFeature) other;

        return eft.equals(o.eft) && lft.equals(o.lft);
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.eft.hashCode();
        result = 31 * result + this.lft.hashCode();

        return result;
    }

    public OmegaBigDecimal getEFT() {
        return eft;
    }

    public OmegaBigDecimal getLFT() {
        return lft;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("-- TimedTransitionFeature --\n");
        b.append("[");
        b.append(this.eft);
        b.append(",");
        b.append(this.lft);
        b.append("]");
        return b.toString();
    }
}
