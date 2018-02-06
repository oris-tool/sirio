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

package org.oristool.lello;

import org.oristool.lello.exception.ValueException;

/**
 * Represent a value of one of the supported types in Lello. These are the
 * following:
 *
 * <ul>
 * <li>NIL</li>: contains the only value nil, which corresponds to an undefined
 * value;
 * <li>STRING</li>: Unicode string;
 * <li>INTEGER</li>: signed integer type;
 * <li>REAL</li>: floating point number;
 * <li>BOOLEAN</li>: either true or false.
 * </ul>
 *
 * <p>Constructors and factory methods are provided to construct values any of
 * these types from those in Java.
 */
public class Value {

    /**
     * Represents the value type.
     */
    public enum Type {

        NIL, STRING, INTEGER, REAL, BOOLEAN
    }

    /** Type of this value. */
    private Type type;

    /** Underlying Java field to represent Lello type STRING. */
    private String stringValue;

    /** Underlying Java field to represent Lello type INTEGER. */
    private int integerValue;

    /** Underlying Java field to represent Lello type REAL. */
    private double realValue;

    /** Underlying Java field to represent Lello type BOOLEAN. */
    private boolean booleanValue;

    /**
     * Initializes a value to nil. This is for internal use only, users should
     * call method nil.
     */
    private Value() {

        this.type = Type.NIL;
    }

    /**
     * Initializes a value to a given string.
     *
     * @param stringValue The initial value.
     */
    public Value(String stringValue) {

        if (stringValue == null)
            throw new NullPointerException(
                    "Argument stringValue can not be null.");

        this.type = Type.STRING;
        this.stringValue = stringValue;
    }

    /**
     * Initializes a value to a given integer.
     *
     * @param integerValue The initial value.
     */
    public Value(int integerValue) {

        this.type = Type.INTEGER;
        this.integerValue = integerValue;
    }

    /**
     * Initializes a value to a given real.
     *
     * @param realValue The initial value.
     */
    public Value(double realValue) {

        this.type = Type.REAL;
        this.realValue = realValue;
    }

    /**
     * Initializes a value to a given boolean.
     *
     * @param booleanValue The initial value.
     */
    public Value(boolean booleanValue) {

        this.type = Type.BOOLEAN;
        this.booleanValue = booleanValue;
    }

    /**
     * Creates a copy of this object.
     */
    public Value copy() {

        Value c = new Value();

        if (type.equals(Type.STRING))
            c.stringValue = this.stringValue;
        else if (type.equals(Type.INTEGER))
            c.integerValue = this.integerValue;
        else if (type.equals(Type.REAL))
            c.realValue = this.realValue;
        else if (type.equals(Type.BOOLEAN))
            c.booleanValue = this.booleanValue;

        c.type = this.type;
        return c;
    }

    /**
     * Returns whether this value is of type STRING or not.
     *
     * @return true if this value is a STRING, false otherwise.
     */
    public boolean isString() {

        return type.equals(Type.STRING);
    }

    /**
     * Returns whether this value is of type INTEGER or not.
     *
     * @return true if this value is a INTEGER, false otherwise.
     */
    public boolean isInteger() {

        return type.equals(Type.INTEGER);
    }

    /**
     * Returns whether this value is of type REAL or not.
     *
     * @return true if this value is a REAL, false otherwise.
     */
    public boolean isReal() {

        return type.equals(Type.REAL);
    }

    /**
     * Returns whether this value is of type BOOLEAN or not.
     *
     * @return true if this value is a BOOLEAN, false otherwise.
     */
    public boolean isBoolean() {

        return type.equals(Type.BOOLEAN);
    }

    /**
     * Returns whether this value is nil or not.
     *
     * @return true if this value is nil, false otherwise.
     */
    public boolean isNil() {

        return type.equals(Type.NIL);
    }

    /**
     * Returns whether this value is numeric, which means either INTEGER or
     * REAL.
     *
     * @return true if this value is either INTEGER or REAL, false otherwise.
     */
    public boolean isNumeric() {

        return type.equals(Type.INTEGER) || type.equals(Type.REAL);
    }

    /**
     * Casts the numeric value of this object to real.
     *
     * <p>This method requires this value to be numeric.
     *
     * @return The value as real.
     */
    public double getNumericValueAsReal() {

        if (type.equals(Type.REAL))
            return realValue;
        else if (type.equals(Type.INTEGER))
            return (double) integerValue;
        else
            throw new ValueException("Non numeric value.");
    }

    /**
     * Casts the numeric value of this object to integer.
     *
     * <p>This method requires this value to be numeric.
     *
     * @return The value as integer.
     */
    public int getNumericValueAsInteger() {

        if (type.equals(Type.REAL))
            return (int) realValue;
        else if (type.equals(Type.INTEGER))
            return integerValue;
        else
            throw new ValueException("Non numeric value.");
    }

    /**
     * Returns the nil value.
     *
     * @return The nil value.
     */
    public static Value nil() {

        return new Value();
    }

    /**
     * Creates a BOOLEAN value from a Java string.
     *
     * @param s
     *            The input Java string.
     * @return The parsed value.
     */
    public static Value parseBoolean(String s) {

        if (s == null)
            throw new NullPointerException("Argument s can not be null.");

        Value n = new Value();
        n.type = Type.BOOLEAN;
        n.booleanValue = Boolean.parseBoolean(s);
        return n;
    }

    /**
     * Creates an INTEGER value from a Java string.
     *
     * @param s
     *            The input Java string.
     * @return The parsed value.
     */
    public static Value parseInteger(String s) {

        if (s == null)
            throw new NullPointerException("Argument s can not be null.");

        Value n = new Value();
        n.type = Type.INTEGER;
        n.integerValue = Integer.parseInt(s);
        return n;
    }

    /**
     * Creates a REAL value from a Java string.
     *
     * @param s
     *            The input Java string.
     * @return The parsed value.
     */
    public static Value parseReal(String s) {

        if (s == null)
            throw new NullPointerException("Argument s can not be null.");

        Value n = new Value();
        n.type = Type.REAL;
        n.realValue = Double.parseDouble(s);
        return n;
    }

    /**
     * Creates a STRING value from a Java string.
     *
     * @param s
     *            The input Java string.
     * @return The parsed value.
     */
    public static Value parseString(String s) {

        if (s == null)
            throw new NullPointerException("Argument s can not be null.");

        Value n = new Value();
        n.type = Type.STRING;
        n.stringValue = s;
        return n;
    }

    /**
     * Returns the type of this value.
     *
     * @return The type of this value.
     */
    public Type getType() {

        return type;
    }

    /**
     * Retrieves the Java string contained in this STRING value; if this value
     * is not a STRING an exception is thrown.
     *
     * @return
     */
    public String getStringValue() {

        if (!type.equals(Type.STRING))
            throw new ValueException("This Value is not of type STRING.");

        return stringValue;
    }

    /**
     * Retrieves the Java string contained in this INTEGER value; if this value
     * is not a INTEGER an exception is thrown.
     *
     * @return
     */
    public int getIntegerValue() {

        if (!type.equals(Type.INTEGER))
            throw new ValueException("This Value is not of type INTEGER.");

        return integerValue;
    }

    /**
     * Retrieves the Java string contained in this REAL value; if this value is
     * not a REAL an exception is thrown.
     *
     * @return
     */
    public double getRealValue() {

        if (!type.equals(Type.REAL))
            throw new ValueException("This Value is not of type REAL.");

        return realValue;
    }

    /**
     * Retrieves the Java string contained in this BOOLEAN value; if this value
     * is not a BOOLEAN an exception is thrown.
     *
     * @return
     */
    public boolean getBooleanValue() {

        if (!type.equals(Type.BOOLEAN))
            throw new ValueException("This Value is not of type BOOLEAN.");

        return booleanValue;
    }

    @Override
    public String toString() {

        if (type.equals(Type.STRING))
            return stringValue;
        else if (type.equals(Type.INTEGER))
            return Integer.valueOf(integerValue).toString();
        else if (type.equals(Type.REAL))
            return Double.valueOf(realValue).toString();
        else if (type.equals(Type.BOOLEAN))
            return Boolean.valueOf(booleanValue).toString();
        else if (type.equals(Type.NIL))
            return "nil";

        throw new RuntimeException("You should never see this exception.");
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof Value))
            return false;

        Value rhs = (Value) o;

        if (!type.equals(rhs.type))
            return false;

        if (type.equals(Type.STRING))
            return stringValue.equals(rhs.stringValue);
        else if (type.equals(Type.INTEGER))
            return integerValue == rhs.integerValue;
        else if (type.equals(Type.REAL))
            return realValue == rhs.realValue;
        else if (type.equals(Type.BOOLEAN))
            return booleanValue == rhs.booleanValue;
        else if (type.equals(Type.NIL))
            return true; // nil is always equal to nil

        throw new RuntimeException("You should never see this exception.");
    }

    @Override
    public int hashCode() {

        if (type.equals(Type.STRING))
            return stringValue.hashCode();
        else if (type.equals(Type.INTEGER))
            return Integer.valueOf(integerValue).hashCode();
        else if (type.equals(Type.REAL))
            return Double.valueOf(realValue).hashCode();
        else if (type.equals(Type.BOOLEAN))
            return Boolean.valueOf(booleanValue).hashCode();
        else if (type.equals(Type.NIL))
            return 0;

        throw new RuntimeException("You should never see this exception.");
    }
}
