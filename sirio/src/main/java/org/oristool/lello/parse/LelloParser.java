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

import java.util.ArrayList;
import java.util.List;

import org.oristool.lello.Value;
import org.oristool.lello.ast.BinaryExpression;
import org.oristool.lello.ast.Brackets;
import org.oristool.lello.ast.Constant;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.ast.FunctionCall;
import org.oristool.lello.ast.UnaryExpression;
import org.oristool.lello.ast.UnaryExpression.UnaryOperationType;
import org.oristool.lello.ast.Variable;
import org.oristool.lello.exception.ParsingException;

/**
 * Lello syntax analyzer (parser).
 * 
 * <p>This class is responsible for reading a stream of terminal symbols and build
 * the expression they represent.
 * 
 * <p>This class implements a recursive descent parser, so it basically consists in
 * a set of methods which are mutually recursive. Such methods have names that
 * starts with the word 'rule'.
 * 
 * <p>Concerning the language it parses, it is C-like; variables and function calls
 * are supported; the modulus operator '%' works also with floating point
 * numbers according to the IEEE 754 specification. More information is
 * available in the user documentation.
 */
public class LelloParser {

    /**
     * Stream of terminal symbols on which parsing will take place.
     */
    private TerminalStream terminals;

    /**
     * Parses a stream of terminal symbols and returns the parsed expression. It
     * delegates all the work to the root rule, which is implemented by the
     * ruleExpr method.
     * 
     * @param terminals
     *            Stream of terminal symbols to parse.
     * @return The parsed expression.
     */
    public Expression parse(TerminalStream terminals) {
        this.terminals = terminals;

        Expression expr = ruleExpr();

        if (!this.terminals.getCurrent().getKind().equals(TerminalKind.EOF))
            error("Could not parse the whole input.");

        return expr;
    }

    /**
     * Checks whether the current symbol is of the specified kind or not.
     * 
     * @param k
     *            The kind to be tested.
     * @return true if the current symbol is of kind k, false otherwise; false
     *         is also returned if there are non more symbols in the stream.
     *         This method is for internal use only.
     */
    private boolean test(TerminalKind k) {
        if (!terminals.hasNext())
            return false;

        Terminal t = terminals.getCurrent();
        return t.getKind().equals(k);
    }

    /**
     * Advances to the next symbol. This method is for internal use only.
     */
    private void next() {
        terminals.moveNext();
    }

    /**
     * Checks whether the current symbol is of the specified kind or not; in the
     * former case it then advances to the next symbol; in the latter it throws
     * an exception. This method is for internal use only.
     * 
     * @param k
     *            The kind to be tested.
     */
    private void match(TerminalKind k) {
        if (test(k)) {
            next();
        } else {
            error("Lello parsing exception A (match).");
        }
    }

    /**
     * Throws a ParsingException using the specified message. The message will
     * be preceded by a label which will contain the row and column numbers of
     * where in the input file the error occurred. This method is for internal
     * use only.
     * 
     * @param message
     *            The message to be presented as an exception.
     */
    private void error(String message) {
        throw new ParsingException("Error at ("
                + terminals.getCurrent().getRow() + ", "
                + terminals.getCurrent().getCol() + "): " + message);
    }

    /**
     * Retrieves the token of the current symbol.
     * 
     * @return The token of the current symbol.
     */
    private String token() {
        return terminals.getCurrent().getToken();
    }

    /**
     * Parses the expression starting at the current symbol. This method is for
     * internal use only.
     * 
     * @return The parsed expression.
     */
    private Expression ruleExpr() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.PREFIX_OP)
                || test(TerminalKind.PREFIX_OP_LOGIC)
                || test(TerminalKind.BRACKET_OPEN)) {
            Expression lt = ruleLogicTerm();
            return ruleLogicTermsR(lt);
        } else {
            error("Lello parsing exception B (ruleExpr).");
            return null;
        }
    }

    /**
     * Parses the expression list starting at the current symbol. This method is
     * for internal use only.
     * 
     * @param i
     *            If the expression list is the argument list of a function
     *            call, this parameter will contain the identifier of the
     *            function.
     * @return The parsed expression.
     */
    private Expression ruleExprList(String i) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.PREFIX_OP)
                || test(TerminalKind.PREFIX_OP_LOGIC)
                || test(TerminalKind.BRACKET_OPEN)) {
            List<Expression> expressions = new ArrayList<Expression>();
            Expression e = ruleExpr();
            expressions.add(e);
            return ruleExprListR(i, expressions);
        } else {
            return ruleExprListR(i, new ArrayList<Expression>());
        }
    }

    /**
     * Continues parsing an expression list. This method is for internal use
     * only.
     * 
     * @param i
     *            If the expression list is the argument list of a function
     *            call, this parameter will contain the identifier of the
     *            function.
     * @param expressions
     *            The previous expressions in the list.
     * @return The parsed expression.
     */
    private Expression ruleExprListR(String i, List<Expression> expressions) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.COMMA)) {
            next();
            Expression e = ruleExpr();
            expressions.add(e);
            return ruleExprListR(i, expressions);
        } else {
            if (i != null) {
                return new FunctionCall(i, expressions);
            } else {
                return new Brackets(expressions);
            }
        }
    }

    /**
     * Parses the logic term starting at the current symbol. This method is for
     * internal use only.
     * 
     * @return The parsed expression.
     */
    private Expression ruleLogicTerm() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.PREFIX_OP)
                || test(TerminalKind.PREFIX_OP_LOGIC)
                || test(TerminalKind.BRACKET_OPEN)) {
            Expression lf = ruleLogicFactor();
            return ruleLogicFactorsR(lf);
        } else {
            error("Lello parsing exception C (ruleLogicTerm).");
            return null;
        }
    }

    /**
     * Continues parsing a logical or (or something of equal precedence). This
     * method is for internal use only.
     * 
     * @param l
     *            The left logic term.
     * @return The parsed expression.
     */
    private Expression ruleLogicTermsR(Expression l) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BIN_OP_LOGIC_TERMS)) {
            String o = token();
            next();
            Expression r = ruleLogicTerm();
            Expression binExp = new BinaryExpression(l, o, r);
            return ruleLogicTermsR(binExp);
        } else {
            return l;
        }
    }

    /**
     * Parses the logic factor starting at the current symbol. This method is
     * for internal use only.
     * 
     * @return The parsed logic factor.
     */
    private Expression ruleLogicFactor() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.PREFIX_OP)
                || test(TerminalKind.BRACKET_OPEN)) {
            Expression rel = ruleRel();
            return ruleRelsR(rel);
        } else if (test(TerminalKind.PREFIX_OP_LOGIC)) {
            String o = token();
            next();
            Expression u = ruleLogicFactor();
            return new UnaryExpression(UnaryOperationType.PREFIX, o, u);
        } else {
            error("Lello parsing exception D (ruleLogicFactor).");
            return null;
        }
    }

    /**
     * Continues parsing a logical and (or something of equal precedence). This
     * method is for internal use only.
     * 
     * @param l
     *            The left logic factor.
     * @return The parsed expression.
     */
    private Expression ruleLogicFactorsR(Expression l) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BIN_OP_LOGIC_FACTORS)) {
            String o = token();
            next();
            Expression r = ruleLogicFactor();
            Expression binExp = new BinaryExpression(l, o, r);
            return ruleLogicFactorsR(binExp);
        } else {
            return l;
        }
    }

    /**
     * Parses the relational expression starting at the current symbol. This
     * method is for internal use only.
     * 
     * @return The parsed expression.
     */
    private Expression ruleRel() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.PREFIX_OP)
                || test(TerminalKind.BRACKET_OPEN)) {
            Expression t = ruleTerm();
            return ruleTermsR(t);
        } else {
            error("Lello parsing exception E (ruleRel).");
            return null;
        }
    }

    /**
     * Continues parsing a relational expression.
     * 
     * @param l
     *            The left side operand.
     * @return The parsed expression.
     */
    private Expression ruleRelsR(Expression l) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BIN_OP_RELS)) {
            String o = token();
            next();
            Expression r = ruleRel();
            Expression binExp = new BinaryExpression(l, o, r);
            return ruleRelsR(binExp);
        } else {
            return l;
        }
    }

    /**
     * Parses the arithmetic term starting at the current symbol. This method is
     * for internal use only.
     * 
     * @return The parsed expression.
     */
    private Expression ruleTerm() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.PREFIX_OP)
                || test(TerminalKind.BRACKET_OPEN)) {
            Expression f = ruleFactor();
            return ruleFactorsR(f);
        } else {
            error("Lello parsing exception F (ruleTerm).");
            return null;
        }
    }

    /**
     * Continues parsing an expression list. This method is for internal use
     * only.
     * 
     * @param i
     *            If the expression list is the argument list of a function
     *            call, this parameter will contain the identifier of the
     *            function.
     * @param expressions
     *            The previous expressions in the list.
     * @return The parsed expression.
     */
    private Expression ruleTermsR(Expression l) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BIN_OP_TERMS)) {
            String o = token();
            next();
            Expression r = ruleTerm();
            Expression binExp = new BinaryExpression(l, o, r);
            return ruleTermsR(binExp);
        } else {
            return l;
        }
    }

    /**
     * Parses the arithmetic factor starting at the current symbol. This method
     * is for internal use only.
     * 
     * @return The parsed expression.
     */
    private Expression ruleFactor() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)
                || test(TerminalKind.CONSTANT_BOOLEAN)
                || test(TerminalKind.CONSTANT_INTEGER)
                || test(TerminalKind.CONSTANT_REAL)
                || test(TerminalKind.CONSTANT_STRING)
                || test(TerminalKind.IDENTIFIER)
                || test(TerminalKind.BRACKET_OPEN)) {
            Expression p = rulePower();
            return rulePowersR(p);
        } else if (test(TerminalKind.PREFIX_OP)) {
            String o = token();
            next();
            Expression u = ruleFactor();
            return new UnaryExpression(UnaryOperationType.PREFIX, o, u);
        } else {
            error("Lello parsing exception H (ruleFactor).");
            return null;
        }
    }

    /**
     * Continues parsing an arithmetic addition (or something of equal
     * precedence). This method is for internal use only.
     * 
     * @param l
     *            The left arithmetic factor.
     * @return The parsed expression.
     */
    private Expression ruleFactorsR(Expression l) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BIN_OP_FACTORS)) {
            String o = token();
            next();
            Expression r = ruleFactor();
            BinaryExpression binExp = new BinaryExpression(l, o, r);
            return ruleFactorsR(binExp);
        } else {
            return l;
        }
    }

    /**
     * Parses the arithmetic power starting at the current symbol. This method
     * is for internal use only.
     * 
     * @return The parsed expression.
     */
    private Expression rulePower() {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.CONSTANT_NIL)) {
            Constant c = new Constant(Value.nil());
            next();
            return c;
        } else if (test(TerminalKind.CONSTANT_BOOLEAN)) {
            Constant c = new Constant(Value.parseBoolean(token()));
            next();
            return c;
        } else if (test(TerminalKind.CONSTANT_INTEGER)) {
            Constant c = new Constant(Value.parseInteger(token()));
            next();
            return c;
        } else if (test(TerminalKind.CONSTANT_REAL)) {
            Constant c = new Constant(Value.parseReal(token()));
            next();
            return c;
        } else if (test(TerminalKind.CONSTANT_STRING)) {
            String str = token();
            str = str.substring(0, str.length() - 1);
            str = str.substring(1);
            Constant c = new Constant(Value.parseString(str));
            next();
            return c;
        } else if (test(TerminalKind.IDENTIFIER)) {
            String i = token();
            next();
            return ruleBrackets(i);
        } else if (test(TerminalKind.BRACKET_OPEN)) {
            return ruleBrackets(null);
        } else {
            error("Lello parsing exception I (rulePower).");
            return null;
        }
    }

    /**
     * Continues parsing an arithmetic power. This method is for internal use
     * only.
     * 
     * @param l
     *            The base of this power.
     * @return The parsed expression.
     */
    private Expression rulePowersR(Expression l) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BIN_OP_POWERS)) {
            String o = token();
            next();
            Expression r = rulePower();
            BinaryExpression binExp = new BinaryExpression(l, o, r);
            return rulePowersR(binExp);
        } else {
            return l;
        }
    }

    /**
     * Parses an expression between brackets.
     * 
     * @param i
     *            If the expression list is the argument list of a function
     *            call, this parameter will contain the identifier of the
     *            function.
     * @return The parsed expression.
     */
    private Expression ruleBrackets(String i) {

        if (test(TerminalKind.UNKNOWN))
            throw new ParsingException("Unknown token: " + token());

        if (test(TerminalKind.BRACKET_OPEN)) {
            next();
            Expression el = ruleExprList(i);
            match(TerminalKind.BRACKET_CLOSE);
            return el;
        } else {
            if (i != null)
                return new Variable(i);
            else
                return null;
        }
    }
}
