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

import org.oristool.math.OmegaBigDecimal;

/**
 * An exponential atomic term {@code x^alpha}, where x is a {@link Variable}.
 */
public class MonomialTerm implements AtomicTerm {

    private int alpha;
    private Variable variable;

    /**
     * Class constructor specifying the {@link variable} of the monomial term
     * and its exponent
     * 
     * @param variable
     *            of the monomial term
     * @param alpha
     *            exponent of the monomial term
     */
    public MonomialTerm(Variable variable, int alpha) {

        if (alpha < 0)
            throw new IllegalArgumentException(
                    "The exponent of an expolynomial should not be negative");

        this.variable = variable;
        this.alpha = alpha;
    }

    /**
     * Returns the {@link Variable} of the monomial term
     * 
     * @return Variable of the term
     */
    @Override
    public Variable getVariable() {
        return variable;
    }

    /**
     * Sets the {@link Variable} of the monomial term
     * 
     * @param Variable
     *            of the term
     */
    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    /**
     * Returns the exponent of the monomial term
     * 
     * @return exponent alpha of the term
     */
    public Integer getAlpha() {
        return alpha;
    }

    /**
     * Sets the exponent of the monomial term
     * 
     * @param alpha
     *            exponent of the term
     */
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void multiply(AtomicTerm term) {

        if (!(term instanceof MonomialTerm))
            throw new IllegalArgumentException(
                    "Only monomial terms of the same kind can be multiplied");

        MonomialTerm o = (MonomialTerm) term;

        if (!o.getVariable().equals(this.getVariable()))
            throw new IllegalArgumentException(
                    "Only monomials of the same variable can be multiplied");

        this.alpha += o.alpha;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof MonomialTerm))
            return false;

        MonomialTerm o = (MonomialTerm) obj;
        return this.variable.equals(o.variable) && this.alpha == o.alpha;
    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + this.variable.hashCode();
        result = 31 * result + this.alpha;

        return result;
    }

    @Override
    public AtomicTerm duplicate() {
        return new MonomialTerm(variable, alpha);
    }

    @Override
    public OmegaBigDecimal evaluate(OmegaBigDecimal value) {

        return value.pow(alpha);
    }

    @Override
    public void substitute(Variable oldVar, Variable newVar) {

        if (this.variable.equals(oldVar))
            this.variable = newVar;
        // FIXME lanciare errore se variable è diversa da oldVar?
    }

    @Override
    public boolean isOne() {

        if (alpha == 0)
            return true;
        else
            return false;
    }

    /**
     * toString function of the exponential term
     */
    @Override
    public String toString() {
        return this.getVariable() + "^" + this.alpha;
    }
}
