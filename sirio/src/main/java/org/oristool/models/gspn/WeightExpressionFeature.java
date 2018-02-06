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

package org.oristool.models.gspn;

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
import org.oristool.petrinet.TransitionFeature;

/**
 * A transition feature specifying a marking-dependent weight.
 */
public final class WeightExpressionFeature implements TransitionFeature {

    private final LelloLexer lexer;
    private final LelloParser parser;
    private final String expression;

    /**
     * Creates a weight with the given expression.
     * @param expr expression
     */
    public WeightExpressionFeature(String expr) {
        expression = expr;
        lexer = new LelloLexer();
        parser = new LelloParser();
    }

    public String getExpression() {
        return expression;
    }

    /**
     * Returns the weight value for a given marking.
     *
     * @param n Petri net
     * @param m marking
     * @return the weight value for the marking
     */
    public double getWeight(PetriNet n, Marking m) {

        List<Terminal> terminals = lexer.lex(expression);
        Expression expr = parser.parse(new ListTerminalStream(terminals));

        Bindings bindings = new Bindings();
        for (String placeName : expr.variables()) {
            if (m.getNonEmptyPlacesNames().contains(placeName)) {
                bindings.set(placeName, new Value(m.getTokens(placeName)));
            } else {
                bindings.set(placeName, new Value(0));
            }
        }

        Value res = expr.eval(bindings);
        if (!res.isNumeric()) {
            throw new IllegalStateException("Weights must resolve to a numeric value."
                    + " Expression: " + expression + " - Marking: " + m);
        }

        double rec = res.getNumericValueAsReal();
        if (rec < 0.0) {
            throw new IllegalStateException("Weights must be nonnegative."
                    + " Expression: " + expression + " - Marking: " + m);
        }

        return rec;
    }

    @Override
    public String toString() {
        return expression;
    }
}
