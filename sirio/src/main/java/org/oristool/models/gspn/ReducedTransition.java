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

import org.oristool.analyzer.Event;
import org.oristool.analyzer.Succession;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Transition;

/**
 * A generic weighted transition of a GSPN to simplify the SuccessionGraph by
 * eliminating vanishing states and cycles it doesn't contain any other kind of
 * information and is used solely as a placeholder Event with easily accessible
 * weight_value.
 */
class ReducedTransition implements Event {

    private String name;

    // ReducedTransition are transitions in a reduced GSPN Graph all of them are
    // exponential
    private double rate;

    ReducedTransition(String name) {
        this(name, 0.0);
    }

    ReducedTransition(String name, double value) {
        this.name = name;
        this.rate = value;
    }

    public void calculateRateFromList(List<Succession> list,
            GSPNGraphAnalyzer analyzer) {
        double listRate = 0.0; // deve partire da rate
        double multiplier = 1.0;
        for (Succession s : list) {
            // nodo di partenza ? sempre tangible quindi la prima successione
            // della lista ha RateExpressionFeature
            if (((Transition) s.getEvent())
                    .hasFeature(RateExpressionFeature.class)) {
                listRate = ((Transition) s.getEvent()).getFeature(
                        RateExpressionFeature.class).getRate(
                        analyzer.getPetriNet(),
                        s.getParent().getFeature(PetriStateFeature.class)
                                .getMarking());
            } else {

                multiplier *= ((Transition) s.getEvent()).getFeature(
                        WeightExpressionFeature.class).getWeight(
                        analyzer.getPetriNet(),
                        s.getParent().getFeature(PetriStateFeature.class)
                                .getMarking());
                // multiplier contains the product of probabilities
                // (must divide by denominator)
                multiplier /= (analyzer.getNodeProbabDenominator().get(analyzer
                        .getGraph().getNode(s.getParent())));
            }
        }
        if (listRate <= 0.0 || multiplier <= 0.0) {
            throw new RuntimeException(
                    "Rate or Multiplier of transition added <= 0.0");
        } else
            this.rate += listRate * multiplier;

    }

    public double getRate() {
        return rate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
