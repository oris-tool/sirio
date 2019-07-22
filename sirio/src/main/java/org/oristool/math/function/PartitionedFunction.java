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

package org.oristool.math.function;

import java.util.List;
import java.util.stream.Collectors;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;

/**
 * Generic interface of partitioned functions.
 */
public interface PartitionedFunction {

    List<? extends Function> getFunctions();

    /**
     * Returns the minimum value across all subdomains.
     *
     * @return minimum value
     */
    default OmegaBigDecimal getDomainsEFT() {

        if (getDomains().size() == 0)
            throw new IllegalStateException("At least a partition must be present");

        return getDomains().get(0).getBound(Variable.TSTAR, Variable.X).negate();
    }

    /**
     * Returns the maximum value across all subdomains.
     *
     * @return maximum value
     */
    default OmegaBigDecimal getDomainsLFT() {

        if (getDomains().size() == 0)
            throw new IllegalStateException("At least a partition must be present");

        return getDomains().get(getDomains().size() - 1).getBound(Variable.X, Variable.TSTAR);
    }

    default List<? extends DBMZone> getDomains() {
        return getFunctions().stream().map(f -> f.getDomain()).collect(Collectors.toList());
    }

    default List<? extends Expolynomial> getDensities() {
        return getFunctions().stream().map(f -> f.getDensity()).collect(Collectors.toList());
    }

    String toMathematicaString();
}
