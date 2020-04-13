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

import org.oristool.lello.Bindings;
import org.oristool.lello.ast.BinaryExpression;
import org.oristool.lello.ast.Brackets;
import org.oristool.lello.ast.Constant;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.ast.FunctionCall;
import org.oristool.lello.ast.UnaryExpression;
import org.oristool.lello.ast.Variable;

/**
 * A Visitor interface used to perform symbolic manipulations on expressions,
 * such as simplifications; a concrete expression calls the appropriate method
 * of the Visitor passing a reference to itself.
 */
public interface SymbolicVisitor {

    /**
     * Called by a binary expression.
     *
     * @param bindings The bindings.
     * @param e The calling expression.
     * @return The manipulated expression.
     */
    Expression visitBinaryExpression(Bindings bindings, BinaryExpression e);

    /**
     * Called by a bracket expression.
     *
     * @param bindings The bindings.
     * @param e The calling expression.
     * @return The manipulated expression.
     */
    Expression visitBrackets(Bindings bindings, Brackets e);

    /**
     * Called by a constant.
     *
     * @param bindings The bindings.
     * @param e The calling expression.
     * @return The manipulated expression.
     */
    Expression visitConstant(Bindings bindings, Constant e);

    /**
     * Called by a function call expression.
     *
     * @param bindings The bindings.
     * @param e The calling expression.
     * @return The manipulated expression.
     */
    Expression visitFunctionCall(Bindings bindings, FunctionCall e);

    /**
     * Called by an unary expression.
     *
     * @param bindings The bindings.
     * @param e The calling expression.
     * @return The manipulated expression.
     */
    Expression visitUnaryExpression(Bindings bindings, UnaryExpression e);

    /**
     * Called by a variable reference.
     *
     * @param bindings The bindings.
     * @param e The calling expression.
     * @return The manipulated expression.
     */
    Expression visitVariable(Bindings bindings, Variable e);
}
