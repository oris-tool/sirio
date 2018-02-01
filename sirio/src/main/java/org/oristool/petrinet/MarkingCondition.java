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

package org.oristool.petrinet;

import java.util.Arrays;
import java.util.List;

import org.oristool.lello.Bindings;
import org.oristool.lello.Value;
import org.oristool.lello.ast.Expression;
import org.oristool.lello.parse.LelloLexer;
import org.oristool.lello.parse.LelloParser;
import org.oristool.lello.parse.ListTerminalStream;
import org.oristool.lello.parse.Terminal;

/**
 * A predicate on token counts.   
 */
public abstract class MarkingCondition {

    public static final MarkingCondition ANY = new MarkingCondition() {
        @Override
        public boolean evaluate(Marking m) {
            return true;
        }

        @Override
        public String toString() {
            return "TRUE";
        }
    };

    public static final MarkingCondition NONE = new MarkingCondition() {
        @Override
        public boolean evaluate(Marking m) {
            return false;
        }

        @Override
        public String toString() {
            return "FALSE";
        }
    };

    /**
     * Builds a marking condition that matches only one of the input markings.
     * 
     * @param givenMarkings markings to match
     * @return true a marking condition matching the input markings  
     */
    public static final MarkingCondition getMarkingMatcherInstance(
            Marking... givenMarkings) {

        return new MarkingCondition() {
            private final List<Marking> matching = Arrays.asList(givenMarkings);

            @Override
            public boolean evaluate(Marking m) {
                return matching.contains(m);
            }
        };
    }

    /**
     * Builds a marking condition that matches those markings that include the same
     * token counts for a subset of places.
     * 
     * @param subMarking a marking
     * @return a marking condition matching markings with a subset of token counts 
     */
    public static final MarkingCondition getSubMarkingMatcherInstance(
            Marking subMarking) {

        return new MarkingCondition() {
            @Override
            public boolean evaluate(Marking m) {
                return m.containsSubMarking(subMarking);
            }
        };
    }

    /**
     * Checks whether the input marking satisfies the predicate encoded by this
     * marking condition.
     * 
     * @param m a marking
     * @return true if the marking satisfies this marking condition
     */
    public abstract boolean evaluate(Marking m);

    /** Parses a marking condition from a string.
     * 
     * @param expression input string
     * @return a marking condition object
     */
    public static MarkingCondition fromString(String expression) {
        return new LelloMarkingCondition(expression);
    }

    private static class LelloMarkingCondition extends MarkingCondition {

        private final Expression expr;
        private final String repr;

        public LelloMarkingCondition(String expression) {

            this.repr = expression;

            LelloLexer lexer = new LelloLexer();
            List<Terminal> terminals = lexer.lex(expression);

            LelloParser parser = new LelloParser();
            expr = parser.parse(new ListTerminalStream(terminals));
        }

        @Override
        public boolean evaluate(Marking m) {

            Bindings bindings = new Bindings();

            for (String placeName : expr.variables())
                if (m.getNonEmptyPlacesNames().contains(placeName))
                    bindings.set(placeName, new Value(m.getTokens(placeName)));
                else
                    bindings.set(placeName, new Value(0));

            Value res = expr.eval(bindings);

            if (!res.isBoolean())
                throw new RuntimeException(
                        "Marking condition type must resolve to boolean.");

            return res.getBooleanValue();
        }

        @Override
        public String toString() {
            return repr;
        }
    }
}
