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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Ticks {

    private static final MathContext mathCtxt = MathContext.DECIMAL128;

    private final List<BigDecimal> integralTicks;

    private final BigDecimal integralTickStep;
    private final int integralTicksPerKernelTick;
    private final int kernelTicks;

    public Ticks(BigDecimal end, int kernelTicks, int integralTicksPerKernelTick) {

        if (kernelTicks < 1) {
            throw new IllegalArgumentException("At least 1 kernel tick is needed");
        }
        if (integralTicksPerKernelTick < 1) {
            throw new IllegalArgumentException(
                    "At least 1 integral tick per kernel tick is needed");
        }

        this.kernelTicks = kernelTicks;
        this.integralTicksPerKernelTick = integralTicksPerKernelTick;
        this.integralTickStep = end.divide(new BigDecimal(kernelTicks - 1), mathCtxt)
                .divide(new BigDecimal(integralTicksPerKernelTick), mathCtxt);

        List<BigDecimal> it = new ArrayList<BigDecimal>();
        for (BigDecimal tick = BigDecimal.ZERO; tick.compareTo(end) <= 0; tick = tick
                .add(integralTickStep)) {
            it.add(tick);
        }
        assert (it.size() == (kernelTicks - 1) * integralTicksPerKernelTick + 1);
        integralTicks = Collections.unmodifiableList(it);

    }

    public boolean isKernelTick(int index) {
        return (index) % integralTicksPerKernelTick == 0;
    }

    public BigDecimal getIntegralTick(int index) {
        return integralTicks.get(index);
    }

    public BigDecimal getKernelTick(int index) {
        int integralIndex = index * integralTicksPerKernelTick - 1;
        return integralTicks.get(integralIndex);
    }

    public List<BigDecimal> getIntegralTicks() {
        return integralTicks;
    }

    public BigDecimal getIntegralTickStep() {
        return integralTickStep;
    }

    public int getNumIntegralTicksPerKernelTick() {
        return integralTicksPerKernelTick;
    }

    public int getNumKernelTicks() {
        return kernelTicks;
    }
}
