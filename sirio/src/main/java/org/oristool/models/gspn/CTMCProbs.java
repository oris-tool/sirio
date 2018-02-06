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

/**
 * Computes CTMC transient probabilities from those of the uniformized embedded
 * DTMC.
 */
class CTMCProbs {

    public static DataContainer setFromDTMCProbs(DataContainer data) {

        List<double[]> dtmcProbs = data.getDtmcTransientProbs();
        double[] weights = data.getFoxGlynnWeights();
        double totalWeight = data.getFoxGlynnTotalWeight();
        int L = data.getLeftTruncPoint();
        int R = data.getRightTruncPoint();
        int size = dtmcProbs.get(0).length;

        double[] result = new double[size];

        for (int i = L; i <= R; i++) {
            for (int j = 0; j < size; j++) {
                result[j] += dtmcProbs.get(i)[j] * weights[i - L];
            }
        }

        for (int i = 0; i < size; i++) {
            result[i] = result[i] / totalWeight;
        }

        data.setCmtcTransientProbs(result);
        return data;
    }
}