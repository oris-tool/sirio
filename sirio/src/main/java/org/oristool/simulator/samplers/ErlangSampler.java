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

package org.oristool.simulator.samplers;

import java.math.BigDecimal;

import org.oristool.math.function.Erlang;

/**
 * Sampler for Erlang random variables.
 */
public final class ErlangSampler implements Sampler {

    private final BigDecimal rate;
    private final int shape;

    public ErlangSampler(Erlang f) {
        this.rate = f.getLambda();
        this.shape = f.getShape();
    }

    @Override
    public BigDecimal getSample() {

        double sample = 0.0;
        for (int i = 0; i < shape; ++i)
            sample += -Math.log(1 - Math.random()) / rate.doubleValue();

        return new BigDecimal(sample);
    }
}
