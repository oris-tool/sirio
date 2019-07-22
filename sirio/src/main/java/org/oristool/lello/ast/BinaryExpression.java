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

package org.oristool.lello.ast;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.oristool.lello.Bindings;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.ValueOperations;
import org.oristool.lello.exception.EvalException;
import org.oristool.lello.visitor.DefaultFormatter;
import org.oristool.lello.visitor.FormatVisitor;
import org.oristool.lello.visitor.SymbolicVisitor;

/**
 * Represents a binary expression. Any string may serve as operator.
 */
public class BinaryExpression extends Expression {

    /** The left hand side expression. */
    private Expression left;

    /** The binary operator. */
    private String op;

    /** The right hand side expression. */
    private Expression right;

    /**
     * Initializes a new binary expression.
     *
     * @param left
     *            The left hand side expression.
     * @param op
     *            The binary operator.
     * @param right
     *            The right hand side expression.
     */
    public BinaryExpression(Expression left, String op, Expression right) {
        if (left == null)
            throw new NullPointerException("Argument left can not be null.");

        if (op == null)
            throw new NullPointerException("Argument op can not be null.");

        if (right == null)
            throw new NullPointerException("Argument right can not be null.");

        this.left = left;
        this.op = op;
        this.right = right;
    }

    /**
     * Retrieves the left hand side expression.
     *
     * @return The left hand side expression.
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Retrieves the binary operator.
     *
     * @return The binary operator.
     */
    public String getOp() {
        return op;
    }

    /**
     * Retrieves the right hand side expression.
     *
     * @return The right hand side expression.
     */
    public Expression getRight() {
        return right;
    }

    @Override
    public Value eval(Bindings bindings) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        Value l = left.eval(bindings);
        Value r = right.eval(bindings);

        if (op.equals("+")) {
            return ValueOperations.add(l, r);
        } else if (op.equals("-")) {
            return ValueOperations.sub(l, r);
        } else if (op.equals("*")) {
            return ValueOperations.mul(l, r);
        } else if (op.equals("/")) {
            return ValueOperations.div(l, r);
        } else if (op.equals("%")) {
            return ValueOperations.mod(l, r);
        } else if (op.equals("^")) {
            return ValueOperations.raise(l, r);
        } else if (op.equals("<")) {
            return ValueOperations.lt(l, r);
        } else if (op.equals("<=")) {
            return ValueOperations.lte(l, r);
        } else if (op.equals(">")) {
            return ValueOperations.gt(l, r);
        } else if (op.equals(">=")) {
            return ValueOperations.gte(l, r);
        } else if (op.equals("==")) {
            return ValueOperations.eq(l, r);
        } else if (op.equals("!=")) {
            return ValueOperations.neq(l, r);
        } else if (op.equals("&&")) {
            return ValueOperations.and(l, r);
        } else if (op.equals("||")) {
            return ValueOperations.or(l, r);
        } else
            throw new EvalException("Unknown operator '" + op + "'.");
    }

    @Override
    public Expression simplify(Bindings bindings, SymbolicVisitor visitor) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitBinaryExpression(bindings, this);
    }

    @Override
    public Truth isZero() {

        return Truth.DONTKNOW;
    }

    @Override
    public Truth isOne() {

        return Truth.DONTKNOW;
    }

    @Override
    public Set<String> variables() {

        Set<String> s = new HashSet<String>(left.variables());
        s.addAll(right.variables());
        return Collections.unmodifiableSet(s);
    }

    @Override
    public BinaryExpression copy() {

        return new BinaryExpression(left.copy(), op, right.copy());
    }

    @Override
    public String format(FormatVisitor visitor) {

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitBinaryExpression(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
