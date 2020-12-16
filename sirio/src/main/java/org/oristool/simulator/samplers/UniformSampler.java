/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
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

package org.oristool.simulator.samplers;

import java.math.BigDecimal;

/**
 * Sampler for uniform random variables.
 */
public class UniformSampler implements Sampler {

    private BigDecimal min;
    private BigDecimal max;

    /**
     * Creates a uniform sampler between {@code min} and {@code max}.
     *
     * @param min minimum value (inclusive)
     * @param max maximum value (exclisive)
     */
    public UniformSampler(BigDecimal min, BigDecimal max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public BigDecimal getSample() {

        if (min.compareTo(max) == 0)
            return min;
        else
            return new BigDecimal(Math.random()).multiply(max.subtract(min)).add(min);

    }
}
