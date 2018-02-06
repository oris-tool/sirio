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

package org.oristool.math.expression;

import java.math.BigDecimal;

import org.oristool.math.OmegaBigDecimal;

/**
 * An exponential atomic term {@code Exp[-lambda * x]}, where {@code x} is a
 * {@link Variable}.
 */
public class ExponentialTerm implements AtomicTerm {

    private Variable variable;
    private BigDecimal lambda;

    /**
     * Class constructor specifying the {@link variable} of the exponential term
     * and its rate
     *
     * @param variable
     *            of the exponential term
     * @param lambda
     *            rate of the exponential term
     */
    public ExponentialTerm(Variable variable, BigDecimal lambda) {
        this.variable = variable;
        this.lambda = lambda;
    }

    /**
     * Returns the {@link Variable} of the exponential term
     *
     * @return Variable of the term
     */
    @Override
    public Variable getVariable() {
        return variable;
    }

    /**
     * Sets the {@link Variable} of the exponential term
     *
     * @param Variable
     *            of the term
     */
    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    /**
     * Returns the rate of the exponential term
     *
     * @return rate lambda of the term
     */
    public BigDecimal getLambda() {
        return lambda;
    }

    /**
     * Sets the rate of the exponential term
     *
     * @param lambda
     *            rate of the term
     */
    public void setLambda(BigDecimal lambda) {
        this.lambda = lambda;
    }

    @Override
    public void multiply(AtomicTerm term) {

        if (!(term instanceof ExponentialTerm))
            throw new IllegalArgumentException(
                    "Only exponential terms of the same kind can be multiplied");

        ExponentialTerm o = (ExponentialTerm) term;

        if (!o.getVariable().equals(this.getVariable()))
            throw new IllegalArgumentException(
                    "Only exponentials of the same variable can be multiplied");

        this.lambda = this.lambda.add(o.lambda);

    }

    @Override
    public AtomicTerm duplicate() {
        return new ExponentialTerm(variable, lambda);
    }

    @Override
    public boolean isOne() {
        if (lambda.compareTo(BigDecimal.ZERO) == 0)
            return true;
        else
            return false;
    }

    @Override
    public OmegaBigDecimal evaluate(OmegaBigDecimal value) {

        OmegaBigDecimal res = value.multiply(new OmegaBigDecimal(lambda)).negate();

        if (res.equals(OmegaBigDecimal.POSITIVE_INFINITY))
            return res;
        else if (res.equals(OmegaBigDecimal.NEGATIVE_INFINITY))
            return OmegaBigDecimal.ZERO;
        else if (Math.exp(new OmegaBigDecimal(lambda).negate().multiply(value).doubleValue()) == Double.POSITIVE_INFINITY)
            return OmegaBigDecimal.POSITIVE_INFINITY;
        else
            return new OmegaBigDecimal(BigDecimal.valueOf(Math.exp(new OmegaBigDecimal(lambda).negate().multiply(value).doubleValue())));

//         return new OmegaBigDecimal(Util.exp(new OmegaBigDecimal(lambda).multiply(value).negate().bigDecimalValue(), 100));
    }

    @Override
    public void substitute(Variable oldVar, Variable newVar) {

        if (this.variable.equals(oldVar))
            this.variable = newVar;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof ExponentialTerm))
            return false;

        ExponentialTerm o = (ExponentialTerm) obj;
        return this.variable.equals(o.variable)
                && this.lambda.compareTo(o.lambda) == 0;
    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + this.variable.hashCode();
        result = 31 * result + this.lambda.hashCode();

        return result;
    }

    /**
     * toString function of the exponential term
     */
    @Override
    public String toString() {
        if (lambda.compareTo(BigDecimal.ZERO) > 0)
            return "Exp[-" + this.lambda.toPlainString() + " "
                    + this.getVariable() + "]";
        else
            return "Exp[" + this.lambda.negate().toPlainString() + " "
                    + this.getVariable() + "]";
    }
}
