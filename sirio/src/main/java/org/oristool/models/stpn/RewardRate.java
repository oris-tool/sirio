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
import org.oristool.petrinet.MarkingCondition;

/**
 * A marking-dependent rate expression.
 */
public abstract class RewardRate {

    public abstract double evaluate(double time, Marking m);

    public static RewardRate fromString(String expression) {
        return new LelloRewardRate(expression);
    }

    /**
     * Builds a marking condition that matches markings with nonzero reward.
     *
     * @param time time, for time-dependant rewards
     * @param rewardRates reward rates
     * @return a marking condition matching markings with nonzero reward rate
     */
    public static final MarkingCondition nonZero(double time, RewardRate[] rewardRates) {
    	return new MarkingCondition() {
    	    @Override public boolean evaluate(Marking m) {
    	        for (RewardRate r: rewardRates) {
    	            if (r.evaluate(time, m) != 0)
    	                return true;
    	        }
    	        
    	        return false;
    	    }
    	};
    }
    
    private static class LelloRewardRate extends RewardRate {

        private Expression expr;
        private String repr;

        public LelloRewardRate(String expression) {

            this.repr = expression;

            LelloLexer lexer = new LelloLexer();
            List<Terminal> terminals = lexer.lex(expression);

            LelloParser parser = new LelloParser();
            expr = parser.parse(new ListTerminalStream(terminals));
        }

        @Override
        // Map<String, ? extends Number> params
        public double evaluate(double time, Marking m) {

            Bindings bindings = new Bindings();

            for (String paramName : expr.variables())
                if (paramName.equals("t"))
                    bindings.set(paramName, new Value(time));
                else if (m.getNonEmptyPlacesNames().contains(paramName))
                    bindings.set(paramName, new Value(m.getTokens(paramName)));
                else
                    bindings.set(paramName, new Value(0));

            Value res = expr.eval(bindings);

            if (!res.isNumeric())
                throw new RuntimeException(
                        "Reward rate type must resolve to numeric value");

            return res.getNumericValueAsReal();
        }

        @Override
        public String toString() {
            return repr;
        }
    }
}
