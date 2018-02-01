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
 * A generic atomic term in a simple product.
 */
public interface AtomicTerm {

    /**
     * Multiplies with another term.
     * 
     * @param term to be multiplied
     */
    void multiply(AtomicTerm term);

    /**
     * Duplicates an atomic term.
     * 
     * @return a copy of this term
     */
    AtomicTerm duplicate();

    /**
     * Checks if this term the constant 1.
     * 
     * @return true if the term is one, false otherwise
     */
    boolean isOne();

    /**
     * Evaluates the atomic term substituting value to its variable.
     * 
     * @param value the value to be substituted to {@code x}
     * @return the result of the evaluation
     */
    OmegaBigDecimal evaluate(OmegaBigDecimal value);

    /**
     * Substitutes oldVar with newVar in the atomic term.
     * 
     * @param oldVar variable to be substituted
     * @param newVar new variable
     */
    void substitute(Variable oldVar, Variable newVar);

    /**
     * Returns the variable associated with the atomic term.
     * 
     * @return the associated variable
     */
    Variable getVariable();
}
