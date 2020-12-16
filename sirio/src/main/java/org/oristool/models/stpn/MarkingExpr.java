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

package org.oristool.models.stpn;

import java.util.List;

import org.oristool.lello.Bindings;
import org.oristool.lello.Value;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.parse.LelloLexer;
import org.oristool.lello.parse.LelloParser;
import org.oristool.lello.parse.ListTerminalStream;
import org.oristool.lello.parse.Terminal;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

/**
 * A function from {@code Marking} to {@code Double}.
 */
@FunctionalInterface
public interface MarkingExpr {

    public static final MarkingExpr ZERO = new ConstantExpr(0.);
    public static final MarkingExpr ONE  = new ConstantExpr(1.);

    /**
     * Creates a marking expression from a string.
     *
     * @param expr input expression string
     * @param pn PetriNet used to extract places
     * @return an instance encoding the input expression
     * @throws IllegalArgumentException if the variables are not places of the Petri net
     */
    public static MarkingExpr from(String expr, PetriNet pn) {
        try {
            double value = Double.parseDouble(expr);
            return new ConstantExpr(value);

        } catch (NumberFormatException e) {
            return new LelloExpr(expr, pn);
        }
    }

    /**
     * Creates a constant marking expression.
     *
     * @param value constant value
     * @return constant marking expression
     */
    public static MarkingExpr of(double value) {
        return new ConstantExpr(value);
    }

    public abstract double evaluate(Marking m);

    static class ConstantExpr implements MarkingExpr {

        private final double value;

        ConstantExpr(double value) {
            this.value = value;
        }

        @Override
        public double evaluate(Marking m) {
            return value;
        }

        @Override
        public String toString() {
            return "ConsExpr{value=" + value + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ConstantExpr))
                return false;
            return Double.compare(value, ((ConstantExpr) o).value) == 0;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }
    }

    static class LelloExpr implements MarkingExpr {

        private final Expression expr;
        private final PetriNet pn;

        LelloExpr(String expr, PetriNet pn) {
            List<Terminal> terminals = new LelloLexer().lex(expr);
            Expression parsedExpr = new LelloParser().parse(new ListTerminalStream(terminals));
            for (String v : parsedExpr.variables())
                if (pn.getPlace(v) == null)
                    throw new IllegalArgumentException("Variable " + v + " is not a place");

            this.expr = parsedExpr;
            this.pn = pn;
        }

        @Override
        public double evaluate(Marking m) {
            Bindings b = new Bindings();

            for (String v : this.expr.variables()) {
                int tokenCount = m.getTokens(this.pn.getPlace(v));
                b.set(v, new Value(tokenCount));
            }

            Value res = expr.eval(b);

            if (!res.isNumeric()) {
                throw new IllegalStateException(
                        "The marking expression must resolve to a numeric value");
            }

            return res.getNumericValueAsReal();
        }

        @Override
        public String toString() {
            return "MarkingExpr{format=" + expr + "}";
        }
    }
}
