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

package org.oristool.lello;

import org.oristool.lello.ast.Expression;
import org.oristool.lello.exception.ValueException;

/**
 * This class contains all the logic behind Lello unary and binary operators.
 * All the methods throw an exception if the arguments are not compatible with
 * the operation; compatible types are combined by first converting them to the
 * same type, always converting the less accurate to the most accurate.
 */
public class ValueOperations {

    /**
     * Implements addition.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Their sum.
     */
    public static Value add(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            if (lhs.isReal() || rhs.isReal()) {
                return new Value(lhs.getNumericValueAsReal()
                        + rhs.getNumericValueAsReal());
            } else {
                return new Value(lhs.getIntegerValue() + rhs.getIntegerValue());
            }
        } else if (lhs.isString()) {
            return new Value(lhs.getStringValue() + rhs.toString());
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements subtraction.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Their difference.
     */
    public static Value sub(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            if (lhs.isReal() || rhs.isReal()) {
                return new Value(lhs.getNumericValueAsReal()
                        - rhs.getNumericValueAsReal());
            } else {
                return new Value(lhs.getIntegerValue() - rhs.getIntegerValue());
            }
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements multiplication.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Their product.
     */
    public static Value mul(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            if (lhs.isReal() || rhs.isReal()) {
                return new Value(lhs.getNumericValueAsReal()
                        * rhs.getNumericValueAsReal());
            } else {
                return new Value(lhs.getIntegerValue() * rhs.getIntegerValue());
            }
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements division.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Their quotient.
     */
    public static Value div(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            if (Expression.isZeroNumber(rhs.getNumericValueAsReal()))
                throw new ArithmeticException(
                        "A division by zero occurred. Check the arguments of LelloParser.parse.");

            return new Value(lhs.getNumericValueAsReal()
                    / rhs.getNumericValueAsReal());
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements modulus (also floating point modulus).
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return The remainder of lhs / rhs.
     */
    public static Value mod(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            if (Expression.isZeroNumber(rhs.getNumericValueAsReal()))
                throw new ArithmeticException(
                        "A division by zero occurred. Check the arguments of LelloParser.parse.");

            if (lhs.isReal() || rhs.isReal()) {
                return new Value(Math.IEEEremainder(
                        lhs.getNumericValueAsReal(),
                        rhs.getNumericValueAsReal()));
            } else {
                return new Value(lhs.getIntegerValue() % rhs.getIntegerValue());
            }
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements power. The result is always REAL.
     *
     * @param lhs
     *            Base.
     * @param rhs
     *            Exponent.
     * @return lhs^rhs as REAL.
     */
    public static Value raise(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(Math.pow(lhs.getNumericValueAsReal(),
                    rhs.getNumericValueAsReal()));
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements less than comparison.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Whether it is true that {@code lhs < rhs} or not.
     */
    public static Value lt(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(
                    lhs.getNumericValueAsReal() < rhs.getNumericValueAsReal());
        } else if (lhs.isString() && rhs.isString()) {
            return new Value(lhs.getStringValue().compareTo(
                    rhs.getStringValue()) < 0);
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements less than or equal comparison.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Whether it is true that {@code lhs <= rhs} or not.
     */
    public static Value lte(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(
                    lhs.getNumericValueAsReal() <= rhs.getNumericValueAsReal());
        } else if (lhs.isString() && rhs.isString()) {
            return new Value(lhs.getStringValue().compareTo(
                    rhs.getStringValue()) <= 0);
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements greater than comparison.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Whether it is true that {@code lhs > rhs} or not.
     */
    public static Value gt(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(
                    lhs.getNumericValueAsReal() > rhs.getNumericValueAsReal());
        } else if (lhs.isString() && rhs.isString()) {
            return new Value(lhs.getStringValue().compareTo(
                    rhs.getStringValue()) > 0);
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements greater than or equal comparison.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Whether it is true that {@code lhs >= rhs} or not.
     */
    public static Value gte(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(
                    lhs.getNumericValueAsReal() >= rhs.getNumericValueAsReal());
        } else if (lhs.isString() && rhs.isString()) {
            return new Value(lhs.getStringValue().compareTo(
                    rhs.getStringValue()) >= 0);
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements equality comparison.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Whether it is true that lhs == rhs or not.
     */
    public static Value eq(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(
                    lhs.getNumericValueAsReal() == rhs.getNumericValueAsReal());
        } else if (lhs.isString() && rhs.isString()) {
            return new Value(lhs.getStringValue().compareTo(
                    rhs.getStringValue()) == 0);
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements inequality comparison.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return Whether it is true that lhs != rhs or not.
     */
    public static Value neq(Value lhs, Value rhs) {
        if (lhs.isNumeric() && rhs.isNumeric()) {
            return new Value(
                    lhs.getNumericValueAsReal() != rhs.getNumericValueAsReal());
        } else if (lhs.isString() && rhs.isString()) {
            return new Value(lhs.getStringValue().compareTo(
                    rhs.getStringValue()) != 0);
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements logical AND.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return lhs AND rhs.
     */
    public static Value and(Value lhs, Value rhs) {
        if (lhs.isBoolean() && rhs.isBoolean()) {
            return new Value(lhs.getBooleanValue() && rhs.getBooleanValue());
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements logical OR.
     *
     * @param lhs
     *            Left hand side operand.
     * @param rhs
     *            Right hand side operand.
     * @return lhs OR rhs.
     */
    public static Value or(Value lhs, Value rhs) {
        if (lhs.isBoolean() && rhs.isBoolean()) {
            return new Value(lhs.getBooleanValue() || rhs.getBooleanValue());
        } else
            throw new ValueException(
                    "This operation is not supported for types "
                            + lhs.getType() + " and " + rhs.getType() + ".");
    }

    /**
     * Implements unary plus.
     *
     * @param u
     *            The operand.
     * @return Value +u.
     */
    public static Value pos(Value u) {
        if (u.isNumeric()) {
            if (u.isReal()) {
                return new Value(u.getRealValue());
            } else {
                return new Value(u.getIntegerValue());
            }
        } else
            throw new ValueException(
                    "This operation is not supported for type " + u.getType()
                            + ".");
    }

    /**
     * Implements unary minus.
     *
     * @param u
     *            The operand.
     * @return Value -u.
     */
    public static Value neg(Value u) {
        if (u.isNumeric()) {
            if (u.isReal()) {
                return new Value(-u.getRealValue());
            } else {
                return new Value(-u.getIntegerValue());
            }
        } else
            throw new ValueException(
                    "This operation is not supported for type " + u.getType()
                            + ".");
    }

    /**
     * Implements logical NOT.
     *
     * @param u
     *            The operand.
     * @return Value NOT(u).
     */
    public static Value not(Value u) {
        if (u.isBoolean()) {
            return new Value(!u.getBooleanValue());
        } else
            throw new ValueException(
                    "This operation is not supported for type " + u.getType()
                            + ".");
    }
}
