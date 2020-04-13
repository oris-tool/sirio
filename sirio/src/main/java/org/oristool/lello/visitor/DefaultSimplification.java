/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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

package org.oristool.lello.visitor;

import java.util.ArrayList;
import java.util.List;

import org.oristool.lello.Bindings;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.ValueOperations;
import org.oristool.lello.ast.BinaryExpression;
import org.oristool.lello.ast.Brackets;
import org.oristool.lello.ast.Constant;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.ast.FunctionCall;
import org.oristool.lello.ast.UnaryExpression;
import org.oristool.lello.ast.UnaryExpression.UnaryOperationType;
import org.oristool.lello.ast.Variable;
import org.oristool.lello.exception.EvalException;

/**
 * Simplifies a Lello expression by using some elementary algebraic properties.
 * This is an implementation of the SymbolicVisitor interface.
 */
public class DefaultSimplification implements SymbolicVisitor {

    @Override
    public Expression visitBinaryExpression(Bindings bindings,
            BinaryExpression e) {
        Expression esimp = new BinaryExpression(e.getLeft().simplify(bindings,
                this), e.getOp(), e.getRight().simplify(bindings, this));

        try {
            Value v = esimp.eval(bindings);
            return new Constant(v);
        } catch (EvalException ex) {
            BinaryExpression beSimp = (BinaryExpression) esimp;

            Expression ls = beSimp.getLeft();
            Expression rs = beSimp.getRight();

            if (beSimp.getOp().equals("+")) {
                if (ls.isZero().equals(Truth.YES))
                    return rs.copy();
                else if (rs.isZero().equals(Truth.YES))
                    return ls.copy();
            } else if (beSimp.getOp().equals("-")) {
                if (ls.isZero().equals(Truth.YES))
                    return new BinaryExpression(new UnaryExpression(
                            UnaryOperationType.PREFIX, "-", new Constant(
                                    new Value(1))), "*", rs);
                else if (rs.isZero().equals(Truth.YES))
                    return ls.copy();
                else if (ls.format(new DefaultFormatter()).equals(
                        rs.format(new DefaultFormatter())))
                    return new Constant(new Value(0));
            } else if (beSimp.getOp().equals("*")) {
                if (ls.isZero().equals(Truth.YES)
                        || rs.isZero().equals(Truth.YES))
                    return new Constant(new Value(0));
                else if (ls.isOne().equals(Truth.YES))
                    return rs.copy();
                else if (rs.isOne().equals(Truth.YES))
                    return ls.copy();
            } else if (beSimp.getOp().equals("^")) {
                if (rs.isZero().equals(Truth.YES))
                    return new Constant(new Value(1));
            } else if (beSimp.getOp().equals("/")) {
                if (ls.format(new DefaultFormatter()).equals(
                        rs.format(new DefaultFormatter())))
                    return new Constant(new Value(1));
                else if (ls.isZero().equals(Truth.YES))
                    return new Constant(new Value(0));
            }

            return esimp;
        }
    }

    @Override
    public Expression visitBrackets(Bindings bindings, Brackets e) {
        Expression innerSimp = e.getExpr().simplify(bindings, this);

        if (innerSimp instanceof Constant) {
            return innerSimp;
        } else if (innerSimp instanceof Variable) {
            return innerSimp;
        } else if (innerSimp instanceof Brackets) {
            return innerSimp;
        } else if (innerSimp instanceof Constant) {
            return innerSimp;
        } else {
            return new Brackets(innerSimp);
        }
    }

    @Override
    public Expression visitConstant(Bindings bindings, Constant e) {
        return e.copy();
    }

    @Override
    public Expression visitFunctionCall(Bindings bindings, FunctionCall e) {
        List<Expression> simplifiedParams = new ArrayList<Expression>();

        boolean foundConstant = false;
        Value minConstant = new Value(0);
        Value maxConstant = new Value(0);

        for (Expression param : e.getParameters()) {
            Expression sparam = param.simplify(bindings, this);

            if (sparam instanceof Constant
                    && ((Constant) sparam).getValue().isNumeric()) {
                Value v = ((Constant) sparam).getValue();

                if (!foundConstant) {
                    minConstant = v;
                    maxConstant = v;
                    foundConstant = true;
                } else {
                    minConstant = ValueOperations.lt(v, minConstant)
                            .getBooleanValue() ? v : minConstant;
                    maxConstant = ValueOperations.gt(v, maxConstant)
                            .getBooleanValue() ? v : maxConstant;
                }
            }

            if ((!(sparam instanceof Constant))
                    || (!e.getName().equals("max") && !e.getName()
                            .equals("min")))
                simplifiedParams.add(sparam);
        }

        if (e.getName().equals("max") && foundConstant)
            simplifiedParams.add(new Constant(maxConstant));

        if (e.getName().equals("min") && foundConstant)
            simplifiedParams.add(new Constant(minConstant));

        Expression simplifiedFC = null;

        if (e.getName().equals("max") && simplifiedParams.size() == 1)
            simplifiedFC = new Constant(maxConstant);
        else if (e.getName().equals("min") && simplifiedParams.size() == 1)
            simplifiedFC = new Constant(minConstant);
        else
            simplifiedFC = new FunctionCall(e.getName(), simplifiedParams);

        try {
            Value v = simplifiedFC.eval(bindings);
            return new Constant(v);
        } catch (EvalException ex) {
            return simplifiedFC;
        }
    }

    @Override
    public Expression visitUnaryExpression(Bindings bindings, UnaryExpression e) {
        Expression esimp = new UnaryExpression(e.getType(), e.getOp(), e
                .getExpr().simplify(bindings, this));

        try {
            Value v = esimp.eval(bindings);
            return new Constant(v);
        } catch (EvalException ex) {
            return esimp;
        }
    }

    @Override
    public Expression visitVariable(Bindings bindings, Variable e) {
        try {
            Value v = e.eval(bindings);
            return new Constant(v);
        } catch (EvalException ex) {
            return e.copy();
        }
    }
}
