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

package org.oristool.lello.parse;

/**
 * Represents the kind of a terminal symbol. The kind of a terminal symbol
 * influences the behavior (precedence, associativity, etc...) of the parser
 * when it encounters the symbol.
 */
public enum TerminalKind {

    /**
     * An unknown terminal symbol.
     */
    UNKNOWN,

    /**
     * Indicates the end of an input.
     */
    EOF,

    /**
     * Represents the constant nil.
     */
    CONSTANT_NIL,

    /**
     * A BOOLEAN constant.
     */
    CONSTANT_BOOLEAN,

    /**
     * An INTEGER constant.
     */
    CONSTANT_INTEGER,

    /**
     * A REAL constant.
     */
    CONSTANT_REAL,

    /**
     * A STRING constant.
     */
    CONSTANT_STRING,

    /**
     * An identifier such as a variable name of function name.
     */
    IDENTIFIER,

    /**
     * A logic binary operator which has the same precedence as logical or.
     */
    BIN_OP_LOGIC_TERMS,

    /**
     * A logic binary operator which has the same precedence as logical and.
     */
    BIN_OP_LOGIC_FACTORS,

    /**
     * A prefixed logic unary operator.
     */
    PREFIX_OP_LOGIC,

    /**
     * A suffixed logic unary operator.
     */
    SUFFIX_OP_LOGIC,

    /**
     * A relational binary operator such as == or <=.
     */
    BIN_OP_RELS,

    /**
     * An arithmetic binary operator which has the same precedence as the
     * addition.
     */
    BIN_OP_TERMS,

    /**
     * An arithmetic binary operator which has the same precedence as the
     * multiplication.
     */
    BIN_OP_FACTORS,

    /**
     * A prefixed arithmetic unary operator.
     */
    PREFIX_OP,

    /**
     * A suffixed arithmetic unary operator.
     */
    SUFFIX_OP,

    /**
     * An arithmetic binary operator which has the same precedence as the power.
     */
    BIN_OP_POWERS,

    /**
     * An open bracket.
     */
    BRACKET_OPEN,

    /**
     * A closing bracket.
     */
    BRACKET_CLOSE,

    /**
     * A comma.
     */
    COMMA
}
