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

package org.oristool.models.stpn.onegen;

import java.util.List;
import java.util.Map;

import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;

class PrecalculatedTickEvaluations {
    private final Map<State, double[]> transientsPerState;
    private final List<OmegaBigDecimal> pdfEvals;
    private final List<OmegaBigDecimal> cdfEvals;
    private final Ticks ticks;

    public PrecalculatedTickEvaluations(Ticks ticks, Map<State, double[]> transientsPerState,
            List<OmegaBigDecimal> pdfEvals, List<OmegaBigDecimal> cdfEvals) {

        if (transientsPerState.isEmpty()) {
            throw new IllegalArgumentException("Empty transient");
        }

        int n = ticks.getIntegralTicks().size();

        if (pdfEvals.size() != n || transientsPerState.values().iterator().next().length != n
                || cdfEvals.size() != n) {
            throw new IllegalArgumentException(
                    "pdfEvaluation, cdfEvaluation and trasientsEvaluation must have the same size");
        }

        this.transientsPerState = transientsPerState;
        this.pdfEvals = pdfEvals;
        this.cdfEvals = cdfEvals;
        this.ticks = ticks;
    }

    public Ticks getTicks() {
        return ticks;
    }

    public double getTransientAt(State destination, int tickIndex) {
        return transientsPerState.get(destination)[tickIndex];
    }

    public double getPdfAt(int tickIndex) {
        return pdfEvals.get(tickIndex).doubleValue();
    }

    public double getCdfAt(int tickIndex) {
        return cdfEvals.get(tickIndex).doubleValue();
    }
}
