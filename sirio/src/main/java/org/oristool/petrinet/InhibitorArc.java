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

package org.oristool.petrinet;

import org.oristool.util.Featurizable;

/**
 * An inhibitor arc connecting a place to a transition.
 *
 * <p>The transition will be enabled only if the place contains fewer tokens
 * than the multiplicity of the inhibitor arc.
 *
 * <p>This class implements the interface {@link TranstionFeature} to be added
 * dynamically as feature to any {@link Transition}.
 */
public final class InhibitorArc extends Featurizable<InhibitorArcFeature> {

    private final Place place;
    private final Transition transition;
    private final int multiplicity;

    /**
     * Constructs an inhibitor arc with multiplicity 1.
     *
     * @param transition the target transition
     * @param place the inhibitor place
     */
    InhibitorArc(Place p, Transition t) {
        this(p, t, 1);
    }

    /**
     * Constructs an inhibitor arc with the given multiplicity.
     *
     * @param transition the target transition
     * @param place the inhibitor place
     * @param multiplicity minimum number of tokens in the place to inhibit the
     *        transition
     */
    InhibitorArc(Place place, Transition transition, int multiplicity) {
        this.place = place;
        this.transition = transition;
        this.multiplicity = multiplicity;
    }

    /**
     * Returns the inhibitor place.
     *
     * @return inhibitor place
     */
    public Place getPlace() {
        return this.place;
    }

    /**
     * Returns the target transition.
     *
     * @return target transition
     */
    public Transition getTransition() {
        return this.transition;
    }

    /**
     * Returns the multiplicity of the inhibitor arc.
     *
     * @return multiplicity
     */
    public int getMultiplicity() {
        return multiplicity;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("!(");
        b.append(this.place);
        b.append(", ");
        b.append(this.transition);
        b.append(", ");
        b.append(multiplicity);
        b.append(')');

        if (this.getFeatures().size() > 0) {
            b.append('\n');
            b.append(super.toString());
        }

        return b.toString();
    }

}
