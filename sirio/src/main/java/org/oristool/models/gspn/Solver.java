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

import java.util.LinkedList;
import java.util.List;

import org.oristool.analyzer.log.AnalysisLogger;

/**
 * Solver using {@link FoxGlynnTruncator}, {@link FoxGlynnWeighter}, {@link CTMCProbs}.
 */
class Solver {

    private final DataContainer data;

    public DataContainer getData() {
        return data;
    }

    public Solver(double requiredAccuracy,
            double[][] matrixUnif, double[] initialProbsVector, AnalysisLogger log) {

        if (requiredAccuracy <= 0.0
                || matrixUnif[0].length != matrixUnif.length
                || matrixUnif[0].length != initialProbsVector.length) {

            throw new IllegalArgumentException();
        }

        this.data = new DataContainer();
        data.setLogger(log);
        data.setLeftTruncPoint(0);
        data.setRightTruncPoint(0);
        data.setNoUnderflow(false);
        data.setRequiredAccuracy(requiredAccuracy);
        data.setUnderFlowLimit(Double.MIN_VALUE);
        data.setOverFlowLimit(Double.MAX_VALUE);
        data.setMatrixUnif(matrixUnif);
        data.setInitialProbsVector(initialProbsVector);
    }

    public double[] computeCtmcTransientProbabilities(double lambda, AnalysisLogger log) {

        if (lambda <= 0.0)
            throw new IllegalArgumentException();
        this.data.setLambda(lambda);

        FoxGlynnTruncator.setTruncationPoints(data);
        if (!data.isNoUnderflow()) {
            // cannot apply Fox&Glynn
            if (log != null)
                log.log("Cannot apply Fox-Glynn\n");

            throw new IllegalStateException();
        }

        FoxGlynnWeighter.setWeights(data);
        if (!data.isNoUnderflow()) {
            // cannot apply Fox&Glynn
            if (log != null)
                log.log("Cannot apply Fox-Glynn\n");

            throw new IllegalStateException();
        }

        data.setDtmcTransientProbs(calculateEmbeddedProbabilities(
                data.getDtmcTransientProbs(), data.getMatrixUnif(),
                data.getInitialProbsVector(), data.getRightTruncPoint(), log));

        CTMCProbs.setFromDTMCProbs(data);

        return data.getCmtcTransientProbs();
    }

    private static List<double[]> calculateEmbeddedProbabilities(List<double[]> oldValues,
            double[][] matrixUnif, double[] rootProbs, int leftTruncPoint, AnalysisLogger log) {
        List<double[]> orderedValues = oldValues;
        if (orderedValues == null)
            orderedValues = new LinkedList<>();

        int statesSize = matrixUnif.length;
        int oldRIndex = orderedValues.size() - 1;

        if (oldRIndex == -1) {
            // First passage
            oldRIndex = 0;
            orderedValues.add(rootProbs);// R=0
        }
        if (oldRIndex + 1 <= leftTruncPoint && log != null) {
            log.log("Evaluating new embedded uniformized DTMC transient solution for R in ["
                    + (oldRIndex + 1) + "," + leftTruncPoint + "]\n");
        }
        // [1,R]
        for (int i = oldRIndex + 1; i <= leftTruncPoint; i++) {
            double[] array = new double[statesSize];
            double[] oldArray = orderedValues.get(orderedValues.size() - 1);
            for (int j = 0; j < statesSize; j++) {
                double counter = 0.0;
                for (int k = 0; k < statesSize; k++) {
                    counter += oldArray[k] * matrixUnif[k][j];
                }
                array[j] = counter;
            }
            orderedValues.add(array);
        }

        return orderedValues;
    }
}