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

package org.oristool.petrinet;

import org.oristool.util.Featurizable;

/**
 * Precondition of a Petri net.
 */
public final class Precondition extends Featurizable<PreconditionFeature> {

    private final Place place;
    private final Transition transition;
    private final int multiplicity;

    /**
     * Builds a precondition with multiplicity 1.
     *
     * @param place input place
     * @param transition target transition
     */
    Precondition(Place p, Transition t) {
        this(p, t, 1);
    }

    /**
     * Builds a precondition with the given multiplicity.
     *
     * @param place input place
     * @param transition target transition
     * @param multiplicity precondition multiplicity
     */
    Precondition(Place place, Transition transition, int multiplicity) {
        this.place = place;
        this.transition = transition;
        this.multiplicity = multiplicity;
    }

    /**
     * Returns the input place of this precondition.
     *
     * @return input place
     */
    public Place getPlace() {
        return this.place;
    }

    /**
     * Returns the target transition of this precondition.
     *
     * @return target transition
     */
    public Transition getTransition() {
        return this.transition;
    }

    /**
     * Returns the multiplicity of this precondition.
     *
     * @return multiplicity
     */
    public int getMultiplicity() {
        return multiplicity;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("(");
        b.append(this.place);
        b.append(", ");
        b.append(this.transition);
        b.append(", ");
        b.append(multiplicity);
        b.append(")");

        if (this.getFeatures().size() > 0) {
            b.append("\n");
            b.append(super.toString());
        }

        return b.toString();
    }
}
