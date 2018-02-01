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

package org.oristool.math.function;

import java.math.BigDecimal;
import java.util.Collection;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Exmonomial;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.ExponentialTerm;
import org.oristool.math.expression.Variable;

public class EXP implements Function {

    private DBMZone domain;
    private Expolynomial density;

    /**
     * e^(-lambda x) function on [0, +infty) domain
     */
    public EXP(Variable x, BigDecimal lambda) {

        OmegaBigDecimal eft;
        OmegaBigDecimal lft;
        if (lambda.compareTo(BigDecimal.ZERO) > 0) {
            eft = OmegaBigDecimal.ZERO;
            lft = OmegaBigDecimal.POSITIVE_INFINITY;
        } else if (lambda.compareTo(BigDecimal.ZERO) < 0) {
            eft = OmegaBigDecimal.NEGATIVE_INFINITY;
            lft = OmegaBigDecimal.ZERO;
        } else
            throw new IllegalArgumentException(
                    "The lambda rate must different than zero");

        domain = new DBMZone(x);
        domain.setCoefficient(x, Variable.TSTAR, lft);
        domain.setCoefficient(Variable.TSTAR, x, eft.negate());

        density = new Expolynomial();
        Exmonomial exmon = new Exmonomial(new OmegaBigDecimal(lambda).abs());
        exmon.addAtomicTerm(new ExponentialTerm(x, lambda));
        density.addExmonomial(exmon);
    }

    public EXP(EXP e) {
        this(e.getVariable(), e.getLambda());
    }

    public BigDecimal getLambda() {
        return density.getExmonomials().get(0).getConstantTerm()
                .bigDecimalValue();
    }

    public Variable getVariable() {

        for (Variable v : domain.getVariables())
            if (!v.equals(Variable.TSTAR))
                return v;

        return null;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof EXP))
            return false;

        EXP other = (EXP) obj;

        return this.getVariable().equals(other.getVariable())
                && this.getLambda().compareTo(other.getLambda()) == 0;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.getLambda().hashCode();
        result = 31 * result + this.getVariable().hashCode();

        return result;
    }

    @Override
    public String toString() {
        // if(domain.getVariables().size().getConstraints().size()==0)
        // return "<empty function>\n";

        String result = "Domain\n";
        result = result + domain.toString();
        result = result + "Density\n";
        result = result + density.toString() + "\n";
        return result;
    }

    @Override
    public String toMathematicaString() {

        Collection<Variable> variables = domain.getVariables();
        variables.remove(Variable.TSTAR);

        String result = "f[";
        String prefix = "";
        for (Variable v : variables) {
            result += prefix + v;
            prefix = "_,";
        }
        result += "_";
        result = result + "] := ( " + density.toString() + " ) * "
                + domain.toUnitStepsString();

        return result;
    }

    @Override
    public Expolynomial getDensity() {
        return density;
    }

    @Override
    public DBMZone getDomain() {
        return domain;
    }
}
