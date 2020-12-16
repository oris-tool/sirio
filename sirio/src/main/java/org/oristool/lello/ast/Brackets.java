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

package org.oristool.lello.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.oristool.lello.Bindings;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.exception.EvalException;
import org.oristool.lello.visitor.DefaultFormatter;
import org.oristool.lello.visitor.FormatVisitor;
import org.oristool.lello.visitor.SymbolicVisitor;

/**
 * Represents an expression enclosed in brackets. It does not have any
 * particular behavior but it allows to determine how an expression was
 * parenthesized before being parsed; also, it allows to add redundant
 * parenthesis to an expression which is being formatted in order to enforce a
 * certain evaluation order in a program which will take as input that formatted
 * expression.
 */
public class Brackets extends Expression {

    /**
     * Due to some technical issues in the Lello parser implementation it was
     * not possible to implement a production rule corresponding to:
     * <pre>
     * '(' expr ')'
     * </pre>
     * because it would conflict with the argument list of function calls, which
     * instead allows several comma-separated expression to be listed between
     * the brackets. It would have been possible to artificially constrain the
     * number of expression in such a list to be exactly one, however it was
     * observed that doing so would have meant to renounce to a useful feature,
     * which is basically a form of vector notation.
     * <pre>
     * '(' expr1 ',' expr2 ',' ... ',' exprN ')'
     * </pre>
     * For this reason this class actually represents a vector, but all the
     * supported operations assume only one expression inside. This is the
     * reason why instead of a single expression this class stores a list of
     * expressions.
     */
    private List<Expression> expressions;

    /**
     * Initializes a new expression in brackets.
     *
     * @param enclosed
     *            The enclosed expression.
     */
    public Brackets(Expression enclosed) {
        if (enclosed == null)
            throw new NullPointerException("Argument enclosed can not be null.");

        this.expressions = new ArrayList<Expression>();
        this.expressions.add(enclosed);
    }

    /**
     * Initializes a new vector, which is a list of expressions enclosed in
     * brackets.
     *
     * @param expressions The enclosed expressions (vector components).
     */
    public Brackets(List<Expression> expressions) {
        if (expressions == null)
            throw new NullPointerException(
                    "Argument expressions can not be null.");

        this.expressions = expressions;
    }

    /**
     * Retrieves the enclosed expression. If this object actually represents a
     * vector an exception will be thrown.
     *
     * @return The enclosed expression.
     */
    public Expression getExpr() {
        if (expressions.size() != 1)
            throw new EvalException(
                    "This is a list of expressions, not a bracket expression.");

        return expressions.get(0);
    }

    /**
     * Retrieves the enclosed expressions, which are the components of the
     * vector. This method will never throw an exception, however it should be
     * used only when this object is being considered with the semantics of a
     * vector.
     *
     * @return The enclosed expressions (vector components).
     */
    public List<Expression> getExpressions() {
        return Collections.unmodifiableList(expressions);
    }

    @Override
    public Value eval(Bindings bindings) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        if (expressions.size() != 1)
            throw new EvalException(
                    "This is a list of expressions, not a bracket expression.");

        return expressions.get(0).eval(bindings);
    }

    @Override
    public Expression simplify(Bindings bindings, SymbolicVisitor visitor) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitBrackets(bindings, this);
    }

    @Override
    public Truth isZero() {
        if (expressions.size() != 1)
            throw new EvalException(
                    "This is a list of expressions, not a bracket expression.");

        return expressions.get(0).isZero();
    }

    @Override
    public Truth isOne() {
        if (expressions.size() != 1)
            throw new EvalException(
                    "This is a list of expressions, not a bracket expression.");

        return expressions.get(0).isOne();
    }

    @Override
    public Set<String> variables() {
        if (expressions.size() != 1)
            throw new EvalException(
                    "This is a list of expressions, not a bracket expression.");

        return expressions.get(0).variables();
    }

    /**
     * Creates a copy of this object.
     */
    @Override
    public Expression copy() {
        List<Expression> copied = new ArrayList<Expression>();

        for (Expression e : expressions) {
            copied.add(e.copy());
        }

        return new Brackets(copied);
    }

    @Override
    public String format(FormatVisitor visitor) {
        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitBrackets(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
