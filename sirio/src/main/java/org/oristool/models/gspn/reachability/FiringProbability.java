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

import org.oristool.analyzer.SuccessionFeature;

/**
 * Succession feature encoding the firing probability.
 */
public final class FiringProbability implements SuccessionFeature {

    private double prob;

    public FiringProbability(double prob) {
        this.prob = prob;
    }

    public double value() {
        return prob;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof FiringProbability))
            return false;

        FiringProbability o = (FiringProbability) obj;
        return Double.compare(prob, o.prob) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(prob);
    }

    @Override
    public String toString() {
        return String.format("Firing probability: %.3f", prob);
    }
}