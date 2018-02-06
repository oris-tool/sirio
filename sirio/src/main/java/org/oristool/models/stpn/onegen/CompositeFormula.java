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

import java.util.ArrayList;
import java.util.List;

class CompositeFormula implements KernelFormula {
    private final List<KernelFormula> addends;

    public CompositeFormula(List<KernelFormula> addends) {
        this.addends = new ArrayList<>(addends);
    }

    @Override
    public double[] compute(PrecalculatedTickEvaluations evaluations) {
        List<double[]> addendsEvaluations = new ArrayList<>();
        for (KernelFormula addend : addends) {
            addendsEvaluations.add(addend.compute(evaluations));
        }

        // adds addends
        double[] results = new double[evaluations.getTicks().getNumKernelTicks()];
        for (int i = 0; i < results.length; i++) {
            for (double[] addendEval : addendsEvaluations) {
                results[i] += addendEval[i];
            }
        }

        return results;
    }

    @Override
    public String toString() {
        if (addends.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(addends.get(0).toString());
        for (int i = 1; i < addends.size(); i++) {
            sb.append(" + " + addends.get(i));
        }
        return sb.toString();
    }
}
