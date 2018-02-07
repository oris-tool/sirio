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

package org.oristool.lello.ast;

import java.util.Set;

import org.oristool.lello.Bindings;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.visitor.DefaultFormatter;
import org.oristool.lello.visitor.FormatVisitor;
import org.oristool.lello.visitor.SymbolicVisitor;

/**
 * The base class for every node in Lello AST (Abstract Syntax Tree). This class
 * provides several services to the algorithms involved in manipulating the AST,
 * both in the form of static methods and instance methods, which can eventually
 * be overridden in subclasses.
 */
public abstract class Expression {

    /**
     * The tolerance beyond which two numeric values are considered the same.
     */
    public static final double EPSILON = 0.000001;

    /**
     * Checks whether a value is equal to 0 (with respect to the EPSILON
     * tolerance).
     *
     * @param x
     *            The value to be tested.
     * @return true if x is equal to 0, false otherwise.
     */
    public static final boolean isZeroNumber(Value x) {

        if (x == null)
            throw new NullPointerException("Argument x can not be null.");

        return x.isNumeric() && isZeroNumber(x.getNumericValueAsReal());
    }

    /**
     * Checks whether a double is equal to 0 (with respect to the EPSILON
     * tolerance). This method is internally called by isZeroNumber(Value) but
     * it is also exposed so that the same identical rule to address the
     * tolerance issue is available to programmers.
     *
     * @param x
     *            The double to be tested.
     * @return true if x is equal to 0, false otherwise.
     */
    public static final boolean isZeroNumber(double x) {

        if (Math.abs(x) >= EPSILON)
            return false;
        else
            return true;
    }

    /**
     * Checks whether a value is equal to 1 (with respect to the EPSILON
     * tolerance).
     *
     * @param x
     *            The value to be tested.
     * @return true if x is equal to 1, false otherwise.
     */
    public static final boolean isOneNumber(Value x) {

        if (x == null)
            throw new NullPointerException("Argument x can not be null.");

        return x.isNumeric() && isOneNumber(x.getNumericValueAsReal());
    }


    /**
     * Checks whether a double is equal to 1 (with respect to the EPSILON
     * tolerance). This method is internally called by isOneNumber(Value) but it
     * is also exposed so that the same identical rule to address the tolerance
     * issue is available to programmers.
     *
     * @param x
     *            The double to be tested.
     * @return true if x is equal to 1, false otherwise.
     */
    public static final boolean isOneNumber(double x) {

        return isZeroNumber(x - 1);
    }

    /**
     * Evaluates this expression using the specified variable bindings. Calling this
     * method does not alter this expression.
     *
     * <p>It is important to note that this method <i>will not</i> attempt any kind
     * of simplification on the expression, which means that all the variables
     * appearing in it must be either bound or correspond to Java static fields
     * defined somewhere; if this is not the case an exception will be thrown.
     *
     * @param bindings Variable bindings.
     * @return The value of the expression.
     */
    public abstract Value eval(Bindings bindings);

    /**
     * Tries to simplify this expression. Calling this method does not alter
     * this expression.
     *
     * <p>This method will not throw an exception if some variables appearing in
     * the expression are not bound. However it may still be able to cancel a
     * variable, if an appropriate rule is found.
     *
     * @param bindings
     *            Variable bindings.
     * @param visitor
     *            The visitor implementing the simplification rules.
     * @return A simplified expression if an appropriate rule was found, the
     *         same expression otherwise.
     */
    public abstract Expression simplify(Bindings bindings,
            SymbolicVisitor visitor);

    /**
     * Checks whether this expression is equal to 0 (with respect to the EPSILON
     * tolerance).
     *
     * <p>To decrease the probability that this method will return DONTKNOW,
     * simplify or eval should be preventively called on this expression.
     *
     * @return YES if this expression is equal to 0, NO if it is not, DONTKNOW
     *         if it is not known.
     */
    public abstract Truth isZero();

    /**
     * Checks whether this expression is equal to 1 (with respect to the EPSILON
     * tolerance).
     *
     * <p>To decrease the probability that this method will return DONTKNOW,
     * simplify or eval should be preventively called on this expression.
     *
     * @return YES if this expression is equal to 1, NO if it is not, DONTKNOW
     *         if it is not known.
     */
    public abstract Truth isOne();

    /**
     * Retrieves the names of all the variables appearing in this expression.
     *
     * @return set of variables
     */
    public abstract Set<String> variables();

    /**
     * Creates a copy of the object.
     *
     * @return a copy
     */
    public abstract Expression copy();

    /**
     * Formats this expression.
     *
     * @param visitor
     *            The visitor implementing the formatting rules.
     * @return The formatted string.
     */
    public String format(FormatVisitor visitor) {

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitExpression(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
