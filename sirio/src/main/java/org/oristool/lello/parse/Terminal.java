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

package org.oristool.lello.parse;

/**
 * Represents a terminal symbol. A terminal symbol consists of a kind, which
 * influences the behavior (precedence, associativity, etc...) of the parser
 * when it encounters the symbol, and a string called token that specifies the
 * exact form in which the token was written within the input file; the token
 * influences evaluation and AST manipulation. In order to allow accurate error
 * reporting every token stores the row and column number where it was located
 * in the input file.
 */
public class Terminal {

    /**
     * Kind of the terminal symbol; this is what is relevant for parsing, while
     * the token is relevant for evaluation and AST manipulation.
     */
    private TerminalKind kind;

    /**
     * Token of the terminal symbol; this is what is relevant for evaluation and
     * AST manipulation, while the terminal kind is relevant for parsing.
     */
    private String token;

    /**
     * The row number in the input file at which this terminal symbol was
     * located.
     */
    private int row;

    /**
     * The column number in the input file at which this terminal symbol was
     * located.
     */
    private int col;

    /**
     * Initializes a new terminal symbol.
     *
     * @param kind
     *            The kind of the terminal symbol.
     * @param token
     *            The token of the terminal symbol.
     * @param row
     *            The row number in the input file at which this terminal symbol
     *            was located.
     * @param col
     *            The column number in the input file at which this terminal
     *            symbol was located.
     */
    public Terminal(TerminalKind kind, String token, int row, int col) {
        this.kind = kind;
        this.token = token;
        this.row = row;
        this.col = col;
    }

    /**
     * Retrieves the kind of the terminal symbol.
     *
     * @return The kind of the terminal symbol.
     */
    public TerminalKind getKind() {
        return kind;
    }

    /**
     * Retrieves the token of the terminal symbol.
     *
     * @return The token of the terminal symbol.
     */
    public String getToken() {
        return token;
    }

    /**
     * Retrieves the row number in the input file at which this terminal symbol
     * was located.
     *
     * @return Row number.
     */
    public int getRow() {
        return row;
    }

    /**
     * Retrieves the column number in the input file at which this terminal
     * symbol was located.
     *
     * @return Column number.
     */
    public int getCol() {
        return col;
    }
}
