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

package org.oristool.lello.visitor;

import org.oristool.lello.ast.BinaryExpression;
import org.oristool.lello.ast.Brackets;
import org.oristool.lello.ast.Constant;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.ast.FunctionCall;
import org.oristool.lello.ast.UnaryExpression;
import org.oristool.lello.ast.UnaryExpression.UnaryOperationType;
import org.oristool.lello.ast.Variable;

/**
 * Formats a Lello expression according to a C-like syntax. This is an
 * implementation of the FormatVisitor interface.
 */
public class DefaultFormatter implements FormatVisitor {

    @Override
    public String visitExpression(Expression e) {
        return "<expr>";
    }

    @Override
    public String visitBinaryExpression(BinaryExpression e) {
        Expression left = e.getLeft();
        String op = e.getOp();
        Expression right = e.getRight();

        return left.format(this) + op + right.format(this);
    }

    @Override
    public String visitBrackets(Brackets e) {
        StringBuilder sb = new StringBuilder();

        sb.append('(');

        for (int i = 0; i < e.getExpressions().size(); ++i) {
            if (i > 0)
                sb.append(',');

            sb.append(e.getExpressions().get(i).toString());
        }

        sb.append(')');

        return sb.toString();
    }

    @Override
    public String visitConstant(Constant e) {
        if (e.getValue().isString())
            return "\"" + e.getValue().toString() + "\"";
        else
            return e.getValue().toString();
    }

    @Override
    public String visitFunctionCall(FunctionCall e) {
        StringBuilder sb = new StringBuilder();

        sb.append(e.getName()).append('(');

        for (int i = 0; i < e.getParameters().size(); ++i) {
            if (i > 0)
                sb.append(',');

            sb.append(e.getParameters().get(i).toString());
        }

        sb.append(')');

        return sb.toString();
    }

    @Override
    public String visitUnaryExpression(UnaryExpression e) {
        if (e.getType().equals(UnaryOperationType.PREFIX))
            return e.getOp() + e.getExpr().toString();
        else
            return e.getExpr().toString() + e.getOp();
    }

    @Override
    public String visitVariable(Variable e) {
        return e.getName();
    }
}
