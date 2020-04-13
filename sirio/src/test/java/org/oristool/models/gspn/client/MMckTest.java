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

package org.oristool.models.gspn.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * Stationary probabilities of an M/M/c/k queue.
 */
@Tag("slow")
class MMckTest {

    private PetriNet pn;
    private Marking marking;

    void buildModel(double arrivalRate, double serviceRate, int servers, int capacity) {
        pn = new PetriNet();

        Place buffer = pn.addPlace("buffer");
        Transition arrival = pn.addTransition("arrival");
        Transition service = pn.addTransition("service");
        pn.addPostcondition(arrival, buffer);
        pn.addPrecondition(buffer, service);

        // arrival rate is constant, but the buffer is finite
        arrival.addFeature(StochasticTransitionFeature.newExponentialInstance(
                new BigDecimal(arrivalRate)));
        arrival.addFeature(new EnablingFunction("buffer < " + capacity));

        // service rate is multiplied by the number of customers in the buffer
        String serviceScaling = "min(buffer, " + servers + ")";
        service.addFeature(StochasticTransitionFeature.newExponentialInstance(
                new BigDecimal(serviceRate), MarkingExpr.from(serviceScaling, pn)));

        // marking initially empty
        marking = new Marking();
    }

    @Test
    void stationary() {

        for (double arrivalRate : List.of(10.0, 20.0, 50.0, 100.0)) {
            for (double serviceRate : List.of(1.0, 5.0, 10.0, 20.0, 50.0, 100.0)) {
                for (int servers : List.of(1, 10, 50, 100, 200)) {
                    for (int capacity : List.of(1, 10, 50, 100, 200, 500)) {
                        double rho = arrivalRate / serviceRate;
                        if (servers > capacity || rho / servers >= 1.0)
                            continue;

                        buildModel(arrivalRate, serviceRate, servers, capacity);
                        Map<Marking, Double> dist = GSPNSteadyState.builder().build()
                                .compute(pn, marking);

                        double sum = 0.0;
                        Marking m = new Marking();
                        for (int c = 0; c <= capacity; c++) {
                            m.setTokens(pn.getPlace("buffer"), c);
                            double actual = dist.getOrDefault(m, 0.0);
                            double expected = prob(c, rho, servers, capacity);
                            assertEquals(expected, actual, 1e-8);
                            sum += actual;
                        }

                        assertEquals(1.0, sum, 1e-8);
                    }
                }
            }
        }
    }

    private static double prob(int n, double rho, int servers, int capacity) {

        if (n == 0) {
            double den = powFactorialSeries(rho, 0, servers - 1)
                    + powFactorialTerm(rho, servers)
                    * powSeries(rho / servers, 0, capacity - servers);
            return 1 / den;

        } else if (n <= servers) {
            return prob(0, rho, servers, capacity) * powFactorialTerm(rho, n);

        } else if (n <= capacity) {
            return prob(0, rho, servers, capacity) * powFactorialTerm(rho, servers)
                    * Math.pow(rho / servers, n - servers);
        } else {
            return 0.0;
        }
    }

    private static double powFactorialTerm(double x, int i) {
        double currentTerm = 1.0;
        for (int n = 1; n <= i; n++) {
            currentTerm = currentTerm * (x / n);
        }

        return currentTerm;
    }

    private static double powFactorialSeries(double x, int i, int j) {
        double currentTerm = powFactorialTerm(x, i);
        double result = currentTerm;
        for (int n = i + 1; n <= j; n++) {
            currentTerm = currentTerm * (x / n);
            result += currentTerm;
        }

        return result;
    }

    private static double powSeries(double x, int i, int j) {
        double result = 0.0;
        for (int n = i; n <= j; n++) {
            result += Math.pow(x, n);
        }

        return result;
    }
}
