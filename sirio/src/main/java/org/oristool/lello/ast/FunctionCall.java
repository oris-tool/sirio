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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.oristool.lello.Bindings;
import org.oristool.lello.JavaInterop;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.exception.EvalException;
import org.oristool.lello.exception.JavaInteropException;
import org.oristool.lello.parse.LelloLexer;
import org.oristool.lello.visitor.DefaultFormatter;
import org.oristool.lello.visitor.FormatVisitor;
import org.oristool.lello.visitor.SymbolicVisitor;

/**
 * Represents an expression consisting of a function call.
 */
public class FunctionCall extends Expression {

    /** The name of the function being called. */
    private String name;

    /** The argument list of the call. */
    private List<Expression> parameters;

    /**
     * Initializes a new function call expression.
     * 
     * @param name
     *            The name of the function being called.
     * @param parameters
     *            The argument list of the call.
     */
    public FunctionCall(String name, List<Expression> parameters) {

        if (name == null)
            throw new NullPointerException("Argument name can not be null.");

        if (parameters == null)
            throw new NullPointerException(
                    "Argument parameters can not be null.");

        // Identifier format check.
        LelloLexer.validateName(name);

        this.name = name;
        this.parameters = parameters;
    }

    /**
     * Retrieves the name of the function being called.
     * 
     * @return The name of the function being called.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the argument list of the call.
     * 
     * @return The argument list of the call.
     */
    public List<Expression> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Finds a static method within a class, using reflection. For internal use
     * only.
     * 
     * @param className
     *            The class name.
     * @param methodName
     *            The method name.
     * @param lelloValues
     *            The Lello values to be passed to the method.
     * @param javaValues
     *            If a suitable method is found javaValues is filled with the
     *            appropriate casts of the values in lelloValues.
     * @return A suitable overload of the requested method, or null if such an
     *         overload could not be found.
     */
    private static Method findMethod(String className, String methodName,
            List<Value> lelloValues, List<Object> javaValues) {

        Method m;

        // Try to retrieve an overload with an
        // argument for each element in lelloValues.
        m = getMethodSmart(className, methodName, lelloValues, javaValues,
                false);

        // Only static methods are fine.
        if (m != null && !Modifier.isStatic(m.getModifiers()))
            m = null;

        if (m == null) {
            // Try to retrieve an overload with a
            // single List<Value> argument.
            m = getMethodSmart(className, methodName, lelloValues, javaValues,
                    true);

            // Only static methods are fine.
            if (m != null && !Modifier.isStatic(m.getModifiers()))
                m = null;
        }

        return m;
    }

    /**
     * Advanced wrapper around some Java reflection methods; it is able to
     * resolve function calls in overload. For internal use only.
     * 
     * @param className
     *            The class name.
     * @param methodName
     *            The method name.
     * @param lelloValues
     *            The Lello values to be passed to the method.
     * @param javaValues
     *            If a suitable method is found javaValues is filled with the
     *            appropriate casts of the values in lelloValues.
     * @param useValueList
     *            true if a method with a single {@code List<Value>} argument is
     *            desired, false if a method with an argument for each value in
     *            lelloValues is desired.
     * @return A suitable overload of the requested method, or null if such an
     *         overload could not be found.
     */
    private static Method getMethodSmart(String className, String methodName,
            List<Value> lelloValues, List<Object> javaValues,
            boolean useValueList) {

        if (!useValueList) {
            // Looking for an overload with
            // an argument for each value in
            // lelloValues.

            // Retrieves all the methods of the class.
            Method[] methods = null;

            try {
                methods = Class.forName(className).getMethods();
            } catch (Exception ex) {

                // No suitable overloads were found.
                javaValues.clear();
                return null;
            }

            // For each method...
            for (Method m : methods) {

                // If name and number of arguments match
                // the request then a series of casts is
                // attempted.
                if (m.getName().equals(methodName)
                        && m.getParameterTypes().length == lelloValues.size()) {

                    // The initial assumption is that
                    // a complete series of casts will
                    // be found.
                    boolean ok = true;

                    // Clear the list of converted values,
                    // which may still contain a partial
                    // series of casts from a previous attempt.
                    javaValues.clear();

                    // For each argument...
                    for (int i = 0; ok && i < lelloValues.size(); ++i) {

                        // Take the argument value.
                        Value v = lelloValues.get(i);

                        // Retrieves the wrapper class of the corresponding
                        // argument in the argument list of the method.
                        Class<?> a = JavaInterop.getWrapperType(m
                                .getParameterTypes()[i]);

                        try {
                            // Try to convert the value to a Java
                            // Object. If the operation succeds then
                            // the converted value can be added to the
                            // list of converted values.
                            Object o = JavaInterop.ValueToObject(v, a);
                            javaValues.add(o);
                        } catch (JavaInteropException ex) {
                            // Otherwise if the conversion is not
                            // possible then the series of casts
                            // can not be completed.
                            ok = false;
                        }
                    }

                    // A method was found; it can be
                    // returned and javaValues now
                    // contains a complete series
                    // of converted values.
                    if (ok)
                        return m;
                }
            }

            // No suitable overloads were found.
            javaValues.clear();
            return null;
        } else {
            // Looking for an overload with
            // a single List<Value> argument.

            javaValues.clear();
            List<Value> list = new ArrayList<Value>();

            for (Value v : lelloValues) {
                list.add(v.copy());
            }

            javaValues.add(list);

            try {
                return Class.forName(className).getMethod(methodName,
                        new Class<?>[] { List.class });
            } catch (Exception ex) {

                // No suitable overloads were found.
                javaValues.clear();
                return null;
            }
        }
    }

    @Override
    public Value eval(Bindings bindings) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        try {
            List<Value> lelloValues = new ArrayList<Value>();

            for (Expression e : parameters) {
                Value v = e.eval(bindings);
                lelloValues.add(v);
            }

            List<Object> javaValues = new ArrayList<Object>();
            Method m = null;

            if (name.lastIndexOf('.') != -1) {
                String className = name.substring(0, name.lastIndexOf('.'));
                String methodName = name.substring(name.lastIndexOf('.') + 1);

                m = findMethod(className, methodName, lelloValues, javaValues);
            } else {
                m = findMethod("it.unifi.oris.sirio.lello.ValueFuncs", name,
                        lelloValues, javaValues);

                if (m == null) {
                    m = findMethod("java.lang.Math", name, lelloValues,
                            javaValues);
                }
            }

            Object o = null;

            if (m != null)
                o = m.invoke(null, javaValues.toArray(new Object[0]));
            else
                throw new EvalException("Function not found: '" + name + "'.");

            return JavaInterop.ObjectToValue(o);
        } catch (IllegalArgumentException e) {
            throw new EvalException("Unknown error in function call.", e);
        } catch (IllegalAccessException e) {
            throw new EvalException("Unknown error in function call.", e);
        } catch (InvocationTargetException e) {
            throw new EvalException("Unknown error in function call.", e);
        }
    }

    @Override
    public Expression simplify(Bindings bindings, SymbolicVisitor visitor) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitFunctionCall(bindings, this);
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

        Set<String> s = new HashSet<String>();

        for (Expression e : parameters) {
            s.addAll(e.variables());
        }

        return Collections.unmodifiableSet(s);
    }

    /**
     * Creates a copy of this object.
     */
    @Override
    public Expression copy() {
        List<Expression> copied = new ArrayList<Expression>();

        for (Expression e : parameters) {
            copied.add(e.copy());
        }

        return new FunctionCall(name, copied);
    }

    @Override
    public String format(FormatVisitor visitor) {

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitFunctionCall(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
