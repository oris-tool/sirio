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

package org.oristool.models.stpn.onegen;

import org.oristool.analyzer.state.State;

class FiringGenFormula implements KernelFormula {

    private final State state;
    private final double delta;

    public FiringGenFormula(State state) {
        this(state, 1.0);
    }

    public FiringGenFormula(State state, double delta) {
        super();
        this.state = state;
        this.delta = delta;
    }

    @Override
    public double[] compute(PrecalculatedTickEvaluations evaluations) {
        TickExpression expression = t -> evaluations.getPdfAt(t)
                * evaluations.getTransientAt(state, t) * delta;
        return new TickIntegrator().evaluate(expression, evaluations.getTicks());
    }

    @Override
    public String toString() {
        return "GEN{" + Utils.getName(state) + "*" + String.format("%.3f", delta) + "}";
    }
}
