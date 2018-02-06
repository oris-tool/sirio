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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.oristool.lello.exception.ReadOnlyException;
import org.oristool.lello.exception.UndefinedSymbolException;
import org.oristool.lello.parse.LelloLexer;

/**
 * A collection of pairs name/value, also called bindings. This can be seen as a
 * set of variables each bound to a given value.
 */
public class Bindings {

    /**
     * A predefined empty set of bindings; use this instead of null when you do
     * not want to bind any name. This collection is read-only.
     */
    public static final Bindings NOBINDINGS = new Bindings(true);

    /** Maps a name to its value. */
    private Map<String, Value> nameValues;

    /** If true the collection can not be altered in any way. */
    private boolean readOnly;

    /**
     * Initializes an empty set of bindings.
     */
    public Bindings() {

        nameValues = new HashMap<String, Value>();
        this.readOnly = false;
    }

    /**
     * Initializes an empty set of bindings.
     *
     * <p>If the argument is true this constructor initializes the empty set of
     * bindings to be read-only; this is for internal use only, to allow for
     * creation of the NOBINDINGS field.
     *
     * @param readOnly
     *            true if these bindings are to be read-only, false otherwise.
     */
    private Bindings(boolean readOnly) {

        nameValues = new HashMap<String, Value>();
        this.readOnly = readOnly;
    }

    /**
     * Assigns a value to a variable.
     *
     * @param name
     *            The name of the variable to set.
     * @param value
     *            The new value.
     */
    public void set(String name, Value value) {

        if (readOnly)
            throw new ReadOnlyException("This Bindings instance is read-only.");

        if (name == null)
            throw new NullPointerException("Argument name can not be null.");

        if (value == null)
            throw new NullPointerException("Argument value can not be null.");

        // Identifier format check.
        LelloLexer.validateName(name);

        nameValues.put(name, value.copy());
    }

    /**
     * Retrieves a value.
     *
     * @param name
     *            The name of the variable whose value must be retrieved.
     * @return The associated value.
     */
    public Value get(String name) {

        if (name == null)
            throw new NullPointerException("Argument name can not be null.");

        if (!nameValues.containsKey(name))
            throw new UndefinedSymbolException("Undefined variable '" + name
                    + "'.");

        return nameValues.get(name).copy();
    }

    /**
     * Destroys a variable.
     *
     * @param name
     *            The name of the variable to be destroyed.
     */
    public void unset(String name) {

        if (readOnly)
            throw new ReadOnlyException("This Bindings instance is read-only.");

        if (name == null)
            throw new NullPointerException("Argument name can not be null.");

        if (!nameValues.containsKey(name))
            throw new UndefinedSymbolException("Undefined variable '" + name
                    + "'.");

        nameValues.remove(name);
    }

    /**
     * Tells if a given variable is bound to a value or not.
     *
     * @param name
     *            The name of the variable to be tested.
     * @return true if bound, false otherwise.
     */
    public boolean isBound(String name) {

        if (name == null)
            throw new NullPointerException("Argument name can not be null.");

        return nameValues.containsKey(name);
    }

    /**
     * Retrieves all the name/value mappings.
     *
     * @return A map describing all the bindings.
     */
    public Map<String, Value> mapValues() {

        return Collections.unmodifiableMap(nameValues);
    }
}
