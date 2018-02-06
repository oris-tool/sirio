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

package org.oristool.models.stpn;

import java.math.BigDecimal;

import org.oristool.analyzer.SuccessionFeature;

/**
 * Succession feature encoding the firing probability.
 */
public class StochasticSuccessionFeature implements SuccessionFeature {

    private BigDecimal probability;

    public StochasticSuccessionFeature(BigDecimal probability) {
        this.probability = probability;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(BigDecimal probability) {
        this.probability = probability;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof StochasticSuccessionFeature))
            return false;

        StochasticSuccessionFeature o = (StochasticSuccessionFeature) obj;

        return this.probability.equals(o.probability);
    }

    @Override
    public int hashCode() {
        return probability.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Succession probability: ");
        b.append(probability);
        b.append("\n");
        return b.toString();
    }
}
