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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.oristool.lello.Bindings;
import org.oristool.lello.JavaInterop;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.exception.EvalException;
import org.oristool.lello.parse.LelloLexer;
import org.oristool.lello.visitor.DefaultFormatter;
import org.oristool.lello.visitor.FormatVisitor;
import org.oristool.lello.visitor.SymbolicVisitor;

/**
 * Represents an expression consisting of a variable reference.
 */
public class Variable extends Expression {

    /** The name of the variable being referenced. */
    private String name;

    /**
     * Initializes a new variable reference expression.
     * 
     * @param name
     *            The name of the variable being referenced.
     */
    public Variable(String name) {

        if (name == null)
            throw new NullPointerException("Argument name can not be null.");

        // Identifier format check.
        LelloLexer.validateName(name);

        this.name = name;
    }

    /**
     * Retrieves the name of the variable being referenced.
     * 
     * @return The name of the variable being referenced.
     */
    public String getName() {
        return name;
    }

    /**
     * Finds a static field within a class, using reflection. For internal use
     * only.
     * 
     * @param className
     *            The name of the class.
     * @param fieldName
     *            The name of the field.
     * @return The requested field, or null if it could not be found.
     */
    private static Field findField(String className, String fieldName) {

        Field f;

        // Try to retrieve the field.
        f = getFieldSmart(className, fieldName);

        // Only static fields are fine.
        if (f != null && !Modifier.isStatic(f.getModifiers()))
            f = null;

        return f;
    }

    /**
     * Simple wrapper around some Java reflection methods; this was added for
     * uniformity with class FunctionCall, in which getFieldSmart is really
     * smart and implements enough logic to resolve function calls in overload.
     * For internal use only.
     * 
     * @param className
     *            The name of the class.
     * @param fieldName
     *            The name of the field.
     * @return The requested field, or null if it could not be found.
     */
    private static Field getFieldSmart(String className, String fieldName) {

        try {
            return Class.forName(className).getField(fieldName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Value eval(Bindings bindings) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        try {
            if (bindings.isBound(name))
                return bindings.get(name);
            else {
                Field f = null;

                if (name.lastIndexOf('.') != -1) {
                    String className = name.substring(0, name.lastIndexOf('.'));
                    String methodName = name
                            .substring(name.lastIndexOf('.') + 1);

                    f = findField(className, methodName);
                } else {
                    f = findField("it.unifi.oris.sirio.lello.ValueFields", name);

                    if (f == null) {
                        f = findField("java.lang.Math", name);
                    }
                }

                Object o = null;

                if (f != null)
                    o = f.get(null);
                else
                    throw new EvalException("Field not found: '" + name + "'.");

                return JavaInterop.ObjectToValue(o);
            }
        } catch (IllegalAccessException e) {
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

        return visitor.visitVariable(bindings, this);
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
        s.add(name);

        return Collections.unmodifiableSet(s);
    }

    /**
     * Creates a copy of this object.
     */
    @Override
    public Variable copy() {

        return new Variable(name);
    }

    @Override
    public String format(FormatVisitor visitor) {

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitVariable(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
