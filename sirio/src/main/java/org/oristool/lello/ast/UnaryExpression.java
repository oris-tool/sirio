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
 * Represents a unary expression. Any string may serve as operator.
 */
public class UnaryExpression extends Expression {

    /**
     * Represents which version of the operator is being used, either prefix or
     * suffix.
     */
    public enum UnaryOperationType {

        /** Prefix operator. */
        PREFIX,

        /** Suffix operator. */
        SUFFIX
    }

    /** The operator version. */
    private UnaryOperationType type;

    /** The unary operator. */
    private String op;

    /** The operand expression. */
    private Expression expr;

    /**
     * Initializes a new unary expression.
     *
     * @param type
     *            The operator version.
     * @param op
     *            The unary operator.
     * @param expr
     *            The operand expression.
     */
    public UnaryExpression(UnaryOperationType type, String op, Expression expr) {
        if (type == null)
            throw new NullPointerException("Argument type can not be null.");

        if (op == null)
            throw new NullPointerException("Argument op can not be null.");

        if (expr == null)
            throw new NullPointerException("Argument expr can not be null.");

        this.type = type;
        this.op = op;
        this.expr = expr;
    }

    /**
     * Retrieves the operator version.
     *
     * @return The operator version.
     */
    public UnaryOperationType getType() {
        return type;
    }

    /**
     * Retrieves the unary operator.
     *
     * @return The unary operator.
     */
    public String getOp() {
        return op;
    }

    /**
     * Retrieves the operand expression.
     *
     * @return The operand expression.
     */
    public Expression getExpr() {
        return expr;
    }

    @Override
    public Value eval(Bindings bindings) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        Value u = expr.eval(bindings);

        if (op.equals("+")) {
            return ValueOperations.pos(u);
        } else if (op.equals("-")) {
            return ValueOperations.neg(u);
        } else if (op.equals("!")) {
            return ValueOperations.not(u);
        } else
            throw new EvalException("Unknown operator.");
    }

    @Override
    public Expression simplify(Bindings bindings, SymbolicVisitor visitor) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitUnaryExpression(bindings, this);
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

        return expr.variables();
    }

    /**
     * Creates a copy of this object.
     */
    @Override
    public UnaryExpression copy() {

        return new UnaryExpression(type, op, expr.copy());
    }

    @Override
    public String format(FormatVisitor visitor) {

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitUnaryExpression(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
