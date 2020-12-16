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

package org.oristool.models.gspn.chains;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.sparse.csc.CommonOps_DSCC;

/**
 * Iterative computation of DTMC transient probabilities.
 */
public final class DTMCTransientIter {
    private final DMatrixSparseCSC transposeOneStep;
    private int currTime;
    private DMatrixRMaj currProbs;
    private DMatrixRMaj nextProbs;   // for product output
    private DMatrixRMaj tmp = null;  // for swapping

    private DTMCTransientIter(DMatrixSparseCSC transposeOneStep, DMatrixRMaj initialProbs) {
        this.transposeOneStep = transposeOneStep;
        this.currProbs = initialProbs;
        this.nextProbs = initialProbs.createLike();
        this.currTime = 0;
    }

    public static DTMCTransientIter from(DMatrixSparseCSC transposeOneStep,
            DMatrixRMaj initialProbs) {
        return new DTMCTransientIter(transposeOneStep, initialProbs);
    }

    public int currentTime() {
        return currTime;
    }

    public DMatrixRMaj currentProbs() {
        return currProbs;
    }

    /**
     * Advances the probability distribution to the given time point.
     *
     * @param timePoint target time point (must be greater or equal than current time)
     */
    public void advanceTo(int timePoint) {

        int steps = timePoint - currTime;

        if (steps < 0)
            throw new IllegalArgumentException("Target time lower than current one");

        for (int i = 0; i < steps; i++) {
            CommonOps_DSCC.mult(transposeOneStep, currProbs, nextProbs);
            tmp = currProbs;
            currProbs = nextProbs;
            nextProbs = tmp;
            currTime++;
        }
    }
}
