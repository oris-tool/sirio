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

package org.oristool.lello.visitor;

import org.oristool.lello.ast.BinaryExpression;
import org.oristool.lello.ast.Brackets;
import org.oristool.lello.ast.Constant;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.ast.FunctionCall;
import org.oristool.lello.ast.UnaryExpression;
import org.oristool.lello.ast.Variable;

/**
 * A Visitor interface used to format expressions; a concrete expression calls
 * the appropriate method of the Visitor passing a reference to itself.
 */
public interface FormatVisitor {

    /**
     * Called by an expression when no other method is compatible.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitExpression(Expression e);

    /**
     * Called by a binary expression.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitBinaryExpression(BinaryExpression e);

    /**
     * Called by a bracket expression.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitBrackets(Brackets e);

    /**
     * Called by a constant.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitConstant(Constant e);

    /**
     * Called by a function call expression.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitFunctionCall(FunctionCall e);

    /**
     * Called by an unary expression.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitUnaryExpression(UnaryExpression e);

    /**
     * Called by a variable reference.
     *
     * @param e The calling expression.
     * @return The formatted expression.
     */
    String visitVariable(Variable e);
}
