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
 * Postcondition of a Petri net.
 */
public class Postcondition extends Featurizable<PostconditionFeature> {

    private final Transition transition;
    private final Place place;
    private final int multiplicity;

    /**
     * Builds a postcondition with multiplicity 1.
     *
     * @param transition target transition
     * @param place output place
     */
    Postcondition(Transition t, Place p) {
        this(t, p, 1);
    }

    /**
     * Builds a postcondition with the given multiplicity.
     *
     * @param transition target transition
     * @param place output place
     * @param multiplicity postcondition multiplicity
     */
    Postcondition(Transition t, Place p, int multiplicity) {
        this.transition = t;
        this.place = p;
        this.multiplicity = multiplicity;
    }

    /**
     * Returns the target transition of this postcondition.
     *
     * @return target transition
     */
    public Transition getTransition() {
        return this.transition;
    }

    /**
     * Returns the output place of this postcondition.
     *
     * @return output place
     */
    public Place getPlace() {
        return this.place;
    }

    /**
     * Returns the multiplicity of this postcondition.
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
        b.append(this.transition);
        b.append(", ");
        b.append(this.place);
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
