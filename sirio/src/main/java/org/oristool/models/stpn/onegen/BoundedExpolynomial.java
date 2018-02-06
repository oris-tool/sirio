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

import java.util.HashMap;
import java.util.Map;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;

class BoundedExpolynomial {
    private OmegaBigDecimal lowerBound;
    private OmegaBigDecimal upperBound;
    private OmegaBigDecimal minValue;
    private OmegaBigDecimal maxValue;
    private Expolynomial unboundedFunction;
    private Variable var;
    boolean upperBoundGEQ;
    boolean shiftedLowerBound;

    public BoundedExpolynomial(Expolynomial unboundedFunction, Variable var,
            OmegaBigDecimal lowerBound, OmegaBigDecimal minValue, OmegaBigDecimal upperBound,
            OmegaBigDecimal maxValue, boolean upperBoundGEQ, boolean shiftedLowerBound) {

        if (lowerBound.compareTo(upperBound) > 0) {
            throw new IllegalArgumentException("Lower bound greater than upper bound");
        }

        this.var = var;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unboundedFunction = unboundedFunction;
        this.upperBoundGEQ = upperBoundGEQ;
        this.shiftedLowerBound = shiftedLowerBound;
    }

    public OmegaBigDecimal evaluate(OmegaBigDecimal value) {
        if (value.compareTo(lowerBound) < 0) {
            return minValue;
        } else if (value.compareTo(upperBound) > 0
                || (upperBoundGEQ && value.compareTo(upperBound) == 0)) {
            return maxValue;
        } else {
            Map<Variable, OmegaBigDecimal> assignments = new HashMap<>();
            assignments.put(var, shiftedLowerBound ? value.subtract(lowerBound) : value);
            return unboundedFunction.evaluate(assignments);
        }
    }

    public OmegaBigDecimal getLowerBound() {
        return lowerBound;
    }

    public OmegaBigDecimal getUpperBound() {
        return upperBound;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        String fString = unboundedFunction.toString();
        int fs = fString.length();
        s.append(String.format("%" + fs + "s%s\n", minValue, "  if " + var + " < " + lowerBound));
        String geq = upperBoundGEQ ? "= " : " ";
        s.append(String.format("%" + fs + "s%s\n", maxValue,
                "  if " + var + " >" + geq + upperBound));
        s.append(String.format("%" + fs + "s%s\n", fString, "  otherwise"));
        return s.toString();
    }
}
