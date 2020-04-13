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

package org.oristool.math.expression;

/**
 * A generic variable-name holder.
 */
public final class Variable implements Comparable<Variable> {

    public static final Variable TSTAR = new Variable("t*");
    public static final Variable AGE = new Variable("age");
    public static final Variable X = new Variable("x");

    private final String name;

    /**
     * Builds a variable from a string.
     *
     * @param name name of the variable
     */
    public Variable(String name) {
        this.name = name.intern();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Variable))
            return false;

        Variable other = (Variable) obj;
        return this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Variable other) {
        return this.name.compareTo(other.name);
    }
}
