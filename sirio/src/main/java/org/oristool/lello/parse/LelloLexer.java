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

package org.oristool.lello.parse;

import java.util.ArrayList;
import java.util.List;

import org.oristool.lello.exception.FormatException;

/**
 * Lello lexical analyzer (lexer).
 *
 * <p>This class is responsible for reading a string representing an expression and
 * breaking it up in terminal symbols.
 */
public class LelloLexer {

    /** Describes the syntax of an identifier. */
    private static final String IDENTIFIER_SYNTAX =
            "^[_a-zA-Z][_a-zA-Z0-9]*(\\.[_a-zA-Z][_a-zA-Z0-9]*)*$";

    /** Parsed terminals. */
    private List<Terminal> tokens;

    /** Token of the current symbol being parsed. */
    private String token;

    /**
     * The first character of a two character operator, such as == or !=. It is
     * set to zero when no such operator is being parsed.
     */
    private char op2beg;

    /**
     * It is true when the lexer is in string mode, which means that it will
     * keep adding characters to the representation of a literal string until
     * the character " is encountered.
     */
    private boolean string;

    /** Indicates the current column in the input file being parsed. */
    private int curCol;

    /** Indicates the current row in the input file being parsed. */
    private int curRow;

    /**
     * It is true if the current symbol being lexed can represent a left operand
     * in some operation; it is used to disambiguate the role of the subsequent
     * token.
     */
    private boolean leftOp;

    /**
     * Initializes Lello lexical analyzer.
     */
    public LelloLexer() {

        tokens = new ArrayList<Terminal>();
    }

    /**
     * Analyzes a string.
     *
     * @param s
     *            The string to be analyzed.
     * @return The terminal symbols that make up the string.
     */
    public List<Terminal> lex(String s) {

        if (s == null)
            throw new NullPointerException("Argument s can not be null.");

        tokens.clear();
        token = "";
        op2beg = 0;
        string = false;
        curCol = 1;
        curRow = 1;

        leftOp = false;

        // Reads the string char by char.
        char[] chars = new char[s.length()];

        s.getChars(0, s.length(), chars, 0);

        for (char c : chars)
            readChar(c);

        // Reads EOF which tells the lexer
        // that the input is finished.
        readEOF();

        return tokens;
    }

    /**
     * Reads a character. Calling this method triggers the analysis routines and
     * can lead to new terminals being added to the symbol list. This method is
     * for internal use only.
     *
     * @param c
     *            The character to read.
     */
    private void readChar(char c) {

        // When a new line is read
        // the cursor conceptually
        // goes to the first column
        // of the next row.
        if (c == '\n') {
            curCol = 1;
            ++curRow;
        } else {
            ++curCol;
        }

        if (!string) {
            // A string is not being parsed.

            if (op2beg != 0 && isOP2End(op2beg, c)) {
                // This is the case in which a two
                // character operator is parsed
                // because the second character
                // is an appropriate companion
                // of the first.

                token += String.valueOf(c);
                addToken();
            } else if (isWS(c) || isOP1(c) || isOP2Beg(c)) {
                // This is the case of a whitespace,
                // a single character operator or
                // could be the beginning of a two
                // character operator.

                // If there was a token being parsed
                // it can be added as it is to the
                // token list because whitespaces and
                // operators are separators.
                if (!token.equals("")) {
                    addToken();
                }

                if (isOP1(c)) {
                    // A single character operator
                    // is added immediately.

                    token = String.valueOf(c);
                    addToken();
                } else if (isOP2Beg(c)) {
                    // The possible beginning of a two
                    // character operator is
                    // added to the current token.

                    op2beg = c;
                    token += String.valueOf(c);
                }
            } else {
                // This is the case in which
                // something else was read.

                // If there was a possible beginning
                // of a two character operator in the
                // current token, then add it by itself
                // to the token list, because in reality
                // it turned out to be a single character
                // operator.
                if (op2beg != 0) {
                    addToken();
                }

                // If the read character is " then we
                // enter string mode, in which we will
                // add to the token every following
                // character until we find another ",
                // which marks the end of the string.
                if (c == '"') {
                    string = true;

                    // If there was a token being parsed
                    // it can be added as it is to the
                    // token list because " is a separator.
                    if (!token.equals("")) {
                        addToken();
                    }
                }

                // The read character is added to the token.
                token += c;
            }
        } else {
            // A string is being parsed.

            // When character " is found exit
            // the string mode and add the parsed
            // string to the token list. Any
            // character different from " is
            // simply added to the string.
            if (c == '"') {
                string = false;
                token += c;

                addToken();
            } else {
                token += c;
            }
        }
    }

    /**
     * Tells the lexer that the input is finished. This method will add the EOF
     * symbol to the terminal symbol list, but may also add another terminal
     * symbol before that; for instance if the input file ends with a integer
     * constant this method will cause the constant to be added and then the
     * EOF. This method is for internal use only.
     */
    private void readEOF() {

        // The last token is delimited by EOF;
        // if it was partially read complete
        // the parsing and add it to the token
        // list.
        if (!token.equals("")) {
            addToken();
        }

        // Then add EOF.
        addEOF();
    }

    /**
     * Completes the lexing of the current symbol and adds it to the list; the
     * internal state of the lexer is set to start the analysis of the next
     * symbol. This method is for internal use only.
     */
    private void addToken() {
        Terminal terminal = analyzeToken();

        tokens.add(terminal);
        token = "";
        op2beg = 0;
    }

    /**
     * Adds EOF to the list. This method is for internal use only.
     */
    private void addEOF() {
        Terminal terminal = new Terminal(TerminalKind.EOF, null, curRow, curCol);

        tokens.add(terminal);
        token = "";
        op2beg = 0;
    }

    /**
     * This method will try to build a new terminal symbol using all the
     * characters between the previous terminal symbol and the beginning of the
     * next one; this substring of characters is contained in the string member
     * token. Additionally this method will set the leftOp to indicate whether
     * the symbol just built can be used as a left operand or not. This is
     * required in order to disambiguate between unary and binary versions of
     * some operators, such as '+' and '-'. This method is for internal use
     * only.
     *
     * @return The
     */
    private Terminal analyzeToken() {

        if (token.equals("nil")) {
            leftOp = true;
            return new Terminal(TerminalKind.CONSTANT_NIL, token, curRow,
                    curCol);
        } else if (token.equals("true") || token.equals("false")) {
            leftOp = true;
            return new Terminal(TerminalKind.CONSTANT_BOOLEAN, token, curRow,
                    curCol);
        } else if (token.matches("^[0-9]+$")) {
            leftOp = true;
            return new Terminal(TerminalKind.CONSTANT_INTEGER, token, curRow,
                    curCol);
        } else if (token.matches("^[0-9]*\\.?[0-9]+$")) {
            leftOp = true;
            return new Terminal(TerminalKind.CONSTANT_REAL, token, curRow,
                    curCol);
        } else if (token.startsWith("\"") && token.endsWith("\"")) {
            leftOp = true;
            return new Terminal(TerminalKind.CONSTANT_STRING, token, curRow,
                    curCol);
        } else if (token.equals("(")) {
            leftOp = false;
            return new Terminal(TerminalKind.BRACKET_OPEN, token, curRow,
                    curCol);
        } else if (token.equals(")")) {
            leftOp = true;
            return new Terminal(TerminalKind.BRACKET_CLOSE, token, curRow,
                    curCol);
        } else if (token.equals(",")) {
            leftOp = false;
            return new Terminal(TerminalKind.COMMA, token, curRow, curCol);
        } else if (token.equals("+") || token.equals("-")) {
            if (leftOp) {
                leftOp = false;
                return new Terminal(TerminalKind.BIN_OP_TERMS, token, curRow,
                        curCol);
            } else {
                leftOp = false;
                return new Terminal(TerminalKind.PREFIX_OP, token, curRow,
                        curCol);
            }
        } else if (token.equals("*") || token.equals("/") || token.equals("%")) {
            leftOp = false;
            return new Terminal(TerminalKind.BIN_OP_FACTORS, token, curRow,
                    curCol);
        } else if (token.equals("<") || token.equals(">") || token.equals("<=")
                    || token.equals(">=") || token.equals("!=")
                    || token.equals("==")) {
            leftOp = false;
            return new Terminal(TerminalKind.BIN_OP_RELS, token, curRow, curCol);
        } else if (token.equals("^")) {
            leftOp = false;
            return new Terminal(TerminalKind.BIN_OP_POWERS, token, curRow,
                    curCol);
        } else if (token.equals("||")) {
            leftOp = false;
            return new Terminal(TerminalKind.BIN_OP_LOGIC_TERMS, token, curRow,
                    curCol);
        } else if (token.equals("&&")) {
            leftOp = false;
            return new Terminal(TerminalKind.BIN_OP_LOGIC_FACTORS, token,
                    curRow, curCol);
        } else if (token.equals("!")) {
            leftOp = false;
            return new Terminal(TerminalKind.PREFIX_OP_LOGIC, token, curRow,
                    curCol);
        } else if (token.matches(IDENTIFIER_SYNTAX)) {
            leftOp = true;
            return new Terminal(TerminalKind.IDENTIFIER, token, curRow, curCol);
        } else {
            return new Terminal(TerminalKind.UNKNOWN, token, curRow, curCol);
        }
    }

    /**
     * Returns whether the argument is a whitespace. This method is for internal
     * use only.
     *
     * @param c
     *            The character to be tested.
     * @return true if c is a whitespace, false otherwise.
     */
    private static boolean isWS(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    /**
     * Returns whether the argument is a valid single-character operator or not.
     * This method is for internal use only.
     *
     * @param c
     *            The character to be tested.
     * @return true if c is a single character operator, false otherwise.
     */
    private static boolean isOP1(char c) {
        return c == '(' || c == ')' || c == ',' || c == '+' || c == '-'
                || c == '*' || c == '/' || c == '%' || c == '^';
    }

    /**
     * Returns whether the argument could be the beginning of a two-character
     * operator. This method is for internal use only.
     *
     * @param c
     *            The character to be tested.
     * @return true if c could be the beginning of a two character operator,
     *         false otherwise.
     */
    private static boolean isOP2Beg(char c) {
        // !!!MODIFIED!!!
        return c == '<' || c == '!' || c == '>' || c == '&' || c == '|'
                || c == '=';
    }

    /**
     * Returns whether the argument is the end of a two-character operator whose
     * first character is given. This method is for internal use only.
     *
     * @param beg
     *            The first character of the operator.
     * @param c
     *            The character to be tested.
     * @return true if the concatenation of beg and c is a two character
     *         operator.
     */
    private static boolean isOP2End(char beg, char c) {
        return (beg == '<' && c == '=') || (beg == '!' && c == '=')
                || (beg == '>' && c == '=') || (beg == '&' && c == '&')
                || (beg == '|' && c == '|') || (beg == '=' && c == '=');
    }

    /**
     * Checkes whether a string is a valid identifier. If it is not an exception
     * will be thrown.
     *
     * @param n
     *            The string to be tested.
     */
    public static void validateName(String n) {

        if (n == null)
            throw new NullPointerException("Argument n can not be null.");

        if (!n.matches(IDENTIFIER_SYNTAX))
            throw new FormatException("Invalid identifier '" + n + "'.");
    }
}
