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

class TickIntegrator implements TickEvaluator {

    @Override
    public double[] evaluate(TickExpression expression, Ticks ticks) {

        double integralTickStep = ticks.getIntegralTickStep().doubleValue();
        double[] integrals = new double[ticks.getNumKernelTicks()];

        double partialSum = 0.0;
        int kernelTick = 0;

        for (int t = 0; t < ticks.getIntegralTicks().size(); t++) {
            partialSum += expression.evaluate(t) * integralTickStep;
            if (ticks.isKernelTick(t)) {
                integrals[kernelTick] = partialSum;
                kernelTick++;
            }
        }

        return integrals;
    }
}
