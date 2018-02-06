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

package org.oristool.models.pn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.oristool.lello.Bindings;
import org.oristool.lello.Value;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.parse.LelloLexer;
import org.oristool.lello.parse.LelloParser;
import org.oristool.lello.parse.ListTerminalStream;
import org.oristool.lello.parse.Terminal;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.petrinet.TransitionFeature;

/**
 * Transition feature updating the marking after token additions/removals of a
 * firing.
 */
public final class PostUpdater implements MarkingUpdater, TransitionFeature {

    private final Map<Place, Expression> assignExpressions = new LinkedHashMap<>();

    /**
     * Builds a marking updater from a string of update commands.
     *
     * <p>The syntax used of update commands is:
     * {@code place1 = expr1; place2 = expr2; ...} where {@code place1},
     * {@code place2}, ... are place names and {@code expr1}, {@code expr2}, ... are
     * arbitrary expressions of using place names as variables. For example: <ul>
     * <li>{@code p1 = p1 + p2 + max(p3, p4);} <li>{@code p1 = If(p2 > p3, p4, p5);}
     * </ul>
     *
     * <p>Note that all expressions are evaluated before updating the marking, using
     * the current token counts.
     *
     * @param expression the sequence of marking updates
     * @param pn the Petri net that place names refer to
     */
    public PostUpdater(String expression, PetriNet pn) {

        String[] assignments = expression.indexOf(";") != -1
                ? expression.trim().split("\\s*;\\s*") : expression.trim().split("\\s*,\\s*");

        for (String a : assignments) {
            int firstEqualSign = a.indexOf("=");
            if (firstEqualSign == -1)
                throw new IllegalArgumentException(
                        "Missing equal sign in expression '" + a + "'");

            String place = a.substring(0, firstEqualSign).trim();
            String expr = a.substring(firstEqualSign + 1);

            if (!pn.getPlaceNames().contains(place))
                throw new IllegalArgumentException("Place '" + place
                        + "' not present");

            LelloLexer lexer = new LelloLexer();
            List<Terminal> terminals = lexer.lex(expr);
            LelloParser parser = new LelloParser();

            assignExpressions.put(pn.getPlace(place),
                    parser.parse(new ListTerminalStream(terminals)));
        }
    }

    @Override
    public void update(Marking m, PetriNet petriNet, Transition t) {

        Marking tmp = new Marking(m);
        for (Entry<Place, Expression> e : assignExpressions.entrySet())
            tmp.setTokens(e.getKey(), evaluate(e.getValue(), m));

        m.setTokensFrom(tmp);
    }

    private int evaluate(Expression e, Marking m) {

        Bindings bindings = new Bindings();

        for (String placeName : e.variables())
            if (m.getNonEmptyPlacesNames().contains(placeName))
                bindings.set(placeName, new Value(m.getTokens(placeName)));
            else
                bindings.set(placeName, new Value(0));

        Value res = e.eval(bindings);

        if (!res.isInteger())
            throw new IllegalStateException(
                    "Marking expression must resolve to integer.");

        return res.getIntegerValue();
    }

    @Override
    public String toString() {
        return assignExpressions.toString();
    }
}
