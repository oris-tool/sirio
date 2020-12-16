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

import org.oristool.lello.exception.JavaInteropException;

/**
 * Marshals and unmarshals data types and function calls between Lello and Java.
 */
public class JavaInterop {

    /**
     * Converts a Lello value to a Java object.
     *
     * @param v
     *            The Lello value.
     * @return The Java object.
     */
    public static Object ValueToObject(Value v) {

        if (v == null)
            throw new NullPointerException("Argument v can not be null.");

        Object o;

        if (v.isNil()) {
            o = null;
        } else if (v.isString()) {
            o = v.getStringValue();
        } else if (v.isInteger()) {
            o = v.getIntegerValue();
        } else if (v.isReal()) {
            o = v.getRealValue();
        } else if (v.isBoolean()) {
            o = v.getBooleanValue();
        } else
            throw new RuntimeException("You should never see this exception.");

        return o;
    }

    /**
     * Converts a Lello value to a Java object of a specific class.
     *
     * <p>The allowed classes are:
     * <ul>
     * <li>Byte.class</li>
     * <li>Short.class</li>
     * <li>Integer.class</li>
     * <li>Long.class</li>
     * <li>Float.class</li>
     * <li>Double.class</li>
     * <li>String.class</li>
     * <li>Boolean.class</li>
     * </ul>
     *
     * @param v
     *            The Lello value.
     * @param a
     *            One of the classes listed above.
     * @return The Java object.
     */
    public static Object ValueToObject(Value v, Class<?> a) {

        if (v == null)
            throw new NullPointerException("Argument v can not be null.");

        if (a == null)
            throw new NullPointerException("Argument a can not be null.");

        if (a.equals(Value.class))
            return v.copy();

        if (v.isNil())
            return null;

        Object o = JavaInterop.ValueToObject(v);

        if (o.getClass().equals(Integer.class) && a.equals(Boolean.class))
            return Boolean.valueOf(((Integer) o) != 0);
        else if (o.getClass().equals(Integer.class) && a.equals(Byte.class))
            return Byte.valueOf((byte) ((Integer) o).intValue());
        else if (o.getClass().equals(Integer.class) && a.equals(Short.class))
            return Short.valueOf((short) ((Integer) o).intValue());
        else if (o.getClass().equals(Integer.class) && a.equals(Integer.class))
            return Integer.valueOf(((Integer) o).intValue());
        else if (o.getClass().equals(Integer.class) && a.equals(Long.class))
            return Long.valueOf(((Integer) o).intValue());
        else if (o.getClass().equals(Integer.class) && a.equals(Float.class))
            return Float.valueOf(((Integer) o).intValue());
        else if (o.getClass().equals(Integer.class) && a.equals(Double.class))
            return Double.valueOf(((Integer) o).intValue());
        else if (o.getClass().equals(Double.class) && a.equals(Float.class))
            return Float.valueOf((float) ((Double) o).doubleValue());
        else if (o.getClass().equals(Double.class) && a.equals(Double.class))
            return Double.valueOf(((Double) o).doubleValue());
        else if (o.getClass().equals(String.class) && a.equals(String.class))
            return o;
        else if (o.getClass().equals(Boolean.class) && a.equals(Boolean.class))
            return Boolean.valueOf(((Boolean) o).booleanValue());
        else
            throw new JavaInteropException("Lello Value of type ["
                    + v.getType()
                    + "] can not be represented by Java Object of type ["
                    + a.getName() + "].");
    }

    /**
     * Converts a Java object to a Lello value.
     *
     * @param o
     *            The Java object.
     * @return The Lello value.
     */
    public static Value ObjectToValue(Object o) {

        Value v;

        if (o == null) {
            v = Value.nil();
        } else if (o instanceof Value) {
            v = ((Value) o).copy();
        } else if (o instanceof String) {
            v = new Value((String) o);
        } else if (o instanceof Byte) {
            v = new Value(((Byte) o).byteValue());
        } else if (o instanceof Short) {
            v = new Value(((Short) o).shortValue());
        } else if (o instanceof Integer) {
            v = new Value(((Integer) o).intValue());
        } else if (o instanceof Long) {
            v = new Value(((Long) o).intValue());
        } else if (o instanceof Float) {
            v = new Value(((Float) o).floatValue());
        } else if (o instanceof Double) {
            v = new Value(((Double) o).doubleValue());
        } else if (o instanceof Boolean) {
            v = new Value(((Boolean) o).booleanValue());
        } else if (o instanceof Character) {
            v = new Value(o.toString());
        } else
            throw new JavaInteropException("Java Object of type ["
                    + o.getClass().getName()
                    + "] can not be represented by Lello Value.");

        return v;
    }

    /**
     * Returns a class which can be used to represent the given class or
     * primitive type. Passing X.class will cause the method to return X.class;
     * passing SomeWrapperClass.TYPE will cause the method to return
     * SomeWrapperClass.class; in this example SomeWrapperClass is any of the
     * Java wrapper classes for primitive types, such as Integer or Double.
     *
     * @param t
     *            The class or primitive type.
     * @return If t is a class, t itself is returned; if it is a primitive type,
     *         its wrapper class is returned.
     */
    public static Class<?> getWrapperType(Class<?> t) {

        if (t == null)
            throw new NullPointerException("Argument t can not be null.");

        if (t.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (t.equals(Short.TYPE)) {
            return Short.class;
        } else if (t.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (t.equals(Long.TYPE)) {
            return Long.class;
        } else if (t.equals(Float.TYPE)) {
            return Float.class;
        } else if (t.equals(Double.TYPE)) {
            return Double.class;
        } else if (t.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (t.equals(Character.TYPE)) {
            return Character.class;
        } else
            return t;
    }
}
