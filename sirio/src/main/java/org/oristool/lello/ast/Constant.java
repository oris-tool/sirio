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

package org.oristool.lello.ast;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.oristool.lello.Bindings;
import org.oristool.lello.Truth;
import org.oristool.lello.Value;
import org.oristool.lello.visitor.DefaultFormatter;
import org.oristool.lello.visitor.FormatVisitor;
import org.oristool.lello.visitor.SymbolicVisitor;

/**
 * Represents a constant expression.
 */
public class Constant extends Expression {

    /** The value of this constant expression. */
    private Value value;

    /**
     * Initializes a new constant expression.
     *
     * @param value
     *            The value of the constant.
     */
    public Constant(Value value) {
        if (value == null)
            throw new NullPointerException("Argument value can not be null.");

        this.value = value;
    }

    /**
     * Retrieves the value of this constant.
     *
     * @return the value of this constant.
     */
    public Value getValue() {
        return value;
    }

    @Override
    public Value eval(Bindings bindings) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        return value;
    }

    @Override
    public Expression simplify(Bindings bindings, SymbolicVisitor visitor) {

        if (bindings == null)
            throw new NullPointerException(
                    "Argument bindings can not be null. Use Bindings.NOBINDINGS instead.");

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitConstant(bindings, this);
    }

    @Override
    public Truth isZero() {

        if (isZeroNumber(value))
            return Truth.YES;
        else
            return Truth.NO;
    }

    @Override
    public Truth isOne() {

        if (isOneNumber(value))
            return Truth.YES;
        else
            return Truth.NO;
    }

    @Override
    public Set<String> variables() {

        return Collections.unmodifiableSet(new HashSet<String>());
    }

    /**
     * Creates a copy of this object.
     */
    @Override
    public Constant copy() {

        return new Constant(value);
    }

    @Override
    public String format(FormatVisitor visitor) {

        if (visitor == null)
            throw new NullPointerException("Argument visitor can not be null.");

        return visitor.visitConstant(this);
    }

    @Override
    public String toString() {
        return format(new DefaultFormatter());
    }
}
