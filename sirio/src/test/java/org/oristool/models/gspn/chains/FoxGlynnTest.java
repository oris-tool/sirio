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

package org.oristool.models.gspn.chains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.jupiter.api.Test;

class FoxGlynnTest {

    @Test
    void checkError() {
        List<Double> rates = List.of(0.000001, 0.001, 0.01, 0.1,
                1.0, 2.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0,
                200.0, 400.0, 600.0, 1000.0, 1e6, 1e9);

        List<Double> errors = List.of(0.5, 0.01, 1e-6, 1e-9, 1e-10);

        for (double lambda : rates) {
            for (double error : errors) {
                FoxGlynn probs = FoxGlynn.compute(lambda, error);
                double sum = 0.0;
                for (int i = probs.leftPoint(); i <= probs.rightPoint(); i++)
                    sum += probs.poissonProb(i);
                assertTrue(1.0 - sum < error);
            }
        }
    }

    @Test
    void checkValues() {
        double lambda = 1.0;
        double error = 1e-6;

        FoxGlynn fg = FoxGlynn.compute(lambda, error);
        for (int k = fg.leftPoint(); k <= fg.rightPoint(); k++) {
            double expected = Math.pow(lambda, k)
                    / CombinatoricsUtils.factorialDouble(k) * Math.exp(-lambda);
            assertEquals(expected, fg.poissonProb(k), 1e-6);
        }
    }
}
