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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Transition;

class Utils {

    public static String getName(State s) {
        String sourceMarking = s.getFeature(PetriStateFeature.class).getMarking().toString()
                .replace(" ", "");
        String regenerative = s.hasFeature(Regeneration.class) ? "+" : " ";
        return regenerative + sourceMarking;
    }

    public static void addExpTransition(Transition t, BigDecimal rate) {
        t.addFeature(StochasticTransitionFeature.newExponentialInstance(rate));
    }

    public static void addDetTransition(Transition t, BigDecimal weight, BigDecimal ttf) {
        t.addFeature(StochasticTransitionFeature.newDeterministicInstance(ttf,
                MarkingExpr.of(weight.doubleValue())));
    }

    public static void addImmTransition(Transition t, BigDecimal weight) {
        addDetTransition(t, weight, BigDecimal.ZERO);
    }

    public static boolean isImmediate(Transition t) {
        StochasticTransitionFeature feat = t.getFeature(StochasticTransitionFeature.class);
        return feat.asTimedTransitionFeature().isDeterministic()
                && feat.asTimedTransitionFeature().getLFT().equals(OmegaBigDecimal.ZERO);
    }

    public static boolean isExponential(Transition t) {
        if (t.getFeature(StochasticTransitionFeature.class).density().getDensities()
                .size() != 1)
            return false;

        return t.getFeature(StochasticTransitionFeature.class).density().getDensities()
                .get(0).isExponential();
    }

    public static boolean isGeneral(Transition t) {
        return !isExponential(t) && !isImmediate(t);
    }

    public static <T> Set<T> newSet(T initialValue) {
        Set<T> set = new HashSet<>();
        set.add(initialValue);
        return set;
    }

    public static <T> Deque<T> newQueue(T initialValue) {
        Deque<T> queue = new ArrayDeque<>();
        queue.push(initialValue);
        return queue;
    }
}
