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

import java.io.PrintWriter;
import java.util.List;

import org.oristool.lello.exception.ValueException;

/**
 * This class is a container of static methods which can be called from within
 * the Lello interpreter with their qualified or unqualified Java name; it also
 * acts as a container of shared resources on which the methods may depend, such
 * as output streams and open files.
 * 
 * <p>This class is intended for programmers who can modify the Lello source
 * code and that want a fast and easy way to extend the language with new
 * predefined functions which they think will be useful also for someone else
 * later. All functions must be declared static.
 * 
 * <p>Programmers which can not modify the Lello source, or programmers that
 * have a very specific need and do not want to add clutter to this class, can
 * declare static functions anywhere else in their source code; as long as Lello
 * is given the qualified name it will be able to call them.
 */
public class ValueFuncs {

    /**
     * A method can write some output to this shared PrintWriter; if this field
     * is not set any output will be suppressed.
     */
    private static PrintWriter pw = null;

    /**
     * Sets the shared PrintWriter which methods can use to write output.
     * 
     * @param printWriter
     *            The PrintWriter to which output will be written to.
     */
    public static void setPrintWriter(PrintWriter printWriter) {

        pw = printWriter;
    }

    /**
     * Finds the maximum of a list of values.
     * 
     * @param params
     *            A list of numeric values.
     * @return The maximum of the list.
     */
    public static double max(List<Value> params) {

        boolean foundOne = false;
        double m = 0.0;

        for (Value v : params) {
            if (v.getType().equals(Value.Type.INTEGER) && (!foundOne || v.getIntegerValue() > m)) {
                m = v.getIntegerValue();
                foundOne = true;
            } else if (v.getType().equals(Value.Type.REAL) && (!foundOne || v.getRealValue() > m)) {
                m = v.getRealValue();
                foundOne = true;
            }
        }

        if (!foundOne) {
            throw new ValueException(
                    "Function max called with no numeric arguments.");
        }

        return m;
    }

    /**
     * Finds the minimum of a list of values.
     * 
     * @param params
     *            A list of numeric values.
     * @return The minimum of the list.
     */
    public static double min(List<Value> params) {

        boolean foundOne = false;
        double m = 0.0;

        for (Value v : params) {
            if (v.getType().equals(Value.Type.INTEGER) && (!foundOne || v.getIntegerValue() < m)) {
                m = v.getIntegerValue();
                foundOne = true;
            } else if (v.getType().equals(Value.Type.REAL) && (!foundOne || v.getRealValue() < m)) {
                m = v.getRealValue();
                foundOne = true;
            }
        }

        if (!foundOne) {
            throw new ValueException(
                    "Function min called with no numeric arguments.");
        }

        return m;
    }

    /**
     * Computes the mean of a list of values.
     * 
     * @param params
     *            A list of numeric values.
     * @return The mean.
     */
    public static double avg(List<Value> params) {

        double total = 0.0;
        int cnt = 0;

        for (Value v : params) {
            if (v.getType().equals(Value.Type.INTEGER)) {
                total += v.getIntegerValue();
                ++cnt;
            } else if (v.getType().equals(Value.Type.REAL)) {
                total += v.getRealValue();
                ++cnt;
            }
        }

        if (cnt == 0) {
            throw new ValueException(
                    "Function avg called with no numeric arguments.");
        }

        return total / cnt;
    }

    /**
     * Returns one of two values depending on a boolean condition.
     * 
     * @param condition
     *            The tested condition.
     * @param a
     *            First value.
     * @param b
     *            Second value.
     * @return Value a if condition is true, value b otherwise.
     */
    public static Value If(boolean condition, Value a, Value b) {

        if (condition)
            return a;
        else
            return b;
    }

    /**
     * Converts a value to STRING.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static String ToString(Value x) {
        return x.toString();
    }

    /**
     * Converts a value to BOOLEAN.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static boolean ToBoolean(String x) {

        if (x.equals("true"))
            return true;
        else if (x.equals("false"))
            return false;
        else
            throw new ValueException("Invalid cast STRING to BOOLEAN.");
    }

    /**
     * Converts a value to BOOLEAN.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static boolean ToBoolean(int x) {
        throw new ValueException("Invalid cast INTEGER to BOOLEAN.");
    }

    /**
     * Converts a value to BOOLEAN.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static boolean ToBoolean(double x) {
        throw new ValueException("Invalid cast DOUBLE to BOOLEAN.");
    }

    /**
     * Converts a value to BOOLEAN.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static boolean ToBoolean(boolean x) {
        return x;
    }

    /**
     * Converts a value to INTEGER.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static int ToInteger(String x) {
        return Integer.parseInt(x);
    }

    /**
     * Converts a value to INTEGER.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static int ToInteger(int x) {
        return x;
    }

    /**
     * Converts a value to INTEGER.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static int ToInteger(double x) {
        return (int) x;
    }

    /**
     * Converts a value to INTEGER.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static int ToInteger(boolean x) {
        throw new ValueException("Invalid cast BOOLEAN to INTEGER.");
    }

    /**
     * Converts a value to REAL.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static double ToReal(String x) {
        return Double.parseDouble(x);
    }

    /**
     * Converts a value to REAL.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static double ToReal(int x) {
        return (double) x;
    }

    /**
     * Converts a value to REAL.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static double ToReal(double x) {
        return x;
    }

    /**
     * Converts a value to REAL.
     * 
     * @param x
     *            The value to be converted.
     * @return The converted value.
     */
    public static double ToReal(boolean x) {
        throw new ValueException("Invalid cast BOOLEAN to REAL.");
    }

    /**
     * Retrieves the type name of a value.
     * 
     * <p>The name can be one of the following:
     * <ul>
     * <li>NIL</li>
     * <li>STRING</li>
     * <li>INTEGER</li>
     * <li>REAL</li>
     * <li>BOOLEAN</li>
     * </ul>
     * 
     * @param v The value whose type name is to be retrieved.
     * @return The type name.
     */
    public static String GetTypeName(Value v) {
        return v.getType().toString();
    }

    /**
     * Returns a value as it is.
     * 
     * @param v
     *            The value.
     * @return The same identical value.
     */
    public static Value Identity(Value v) {
        return v;
    }

    /**
     * Writes a message to the shared PrintWriter.
     * 
     * @param msg
     *            The message to be written.
     */
    public static void Print(String msg) {
        if (pw != null) {
            pw.println(msg);
            pw.flush();
        }
    }

    /**
     * Writes a value preceded by a label to the shared PrintWriter; the value
     * itself is also returned.
     * 
     * @param label
     *            The label.
     * @param v
     *            The value.
     * @return The value v unchanged.
     */
    public static Value PrintValue(String label, Value v) {
        if (pw != null) {
            pw.println(label + ": " + v);
            pw.flush();
        }
        return v;
    }

    /**
     * Returns the length of a STRING.
     * 
     * @param s
     *            The string.
     * @return The string length.
     */
    public static int StrLen(String s) {
        return s.length();
    }

    /**
     * Acts as a constant function which is always zero. The input value is not
     * taken into account in any way.
     * 
     * @param v
     *            The input value.
     * @return 0 of type INTEGER.
     */
    public static Value Zero(Value v) {
        return new Value(0);
    }

    /**
     * Acts as a constant function which is always zero. The input value is not
     * taken into account in any way.
     * 
     * @param v
     *            The input value.
     * @return 1 of type INTEGER.
     */
    public static Value One(Value v) {
        return new Value(1);
    }
}
