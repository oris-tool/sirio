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

/**
 * Solver for Fox-Glynn weights.
 */
class FoxGlynnWeighter {

    /**
     * Computes weights for the Fox-Glynn approximation.
     *
     * <p>The input {@code DataContainer} is used as follows.
     * <ul>
     * <li>{@code lambda}: the rate of the Poisson distribution (input)
     * <li>{@code underFlowLimit}: the underflow threshold
     * <li>{@code overFlowLimit}: the overflow threshold.
     * <li>{@code requiredAccuracy}: the wanted accuracy (input)
     * <li>{@code leftTruncPoint}: the left truncation point (input)
     * <li>{@code rightTruncPoint}: the right truncation point (input/output)
     * <li>{@code noUnderflow}: the flag for successful ending of the method
     * <li>{@code foxGlynnWeights}: is the array of weights
     * <li>{@code foxGlynnTotalWeight}: is the sum of the weights
     * </ul>
     */
    public static void setWeights(DataContainer data) {
        final double lambda = data.getLambda();
        final double m = Math.floor(lambda);
        final double tau = data.getUnderFlowLimit();
        final double omega = data.getOverFlowLimit();
        final int L = data.getLeftTruncPoint();

        int R = data.getRightTruncPoint();
        final double[] weights = new double[R - L + 1];

        /**
         * CALCOLO DI w(m): w(m) rappresenta il valore medio della moda da cui
         * si parte per effettuare il calcolo dei restanti weights. La scelta di
         * w(m) deve essere fatta a priori. Il valore ideale sarebbe pari ad 1
         * (v. pg. 441 Computing Poisson Probabilities). Ma per evitare
         * eventuali errori di underflow, lo poniamo uguale a
         * Omega/(10^10)*(R-L). Questa scelta euristica garantisce che il Total
         * Weights sara tale da risultare minore o uguale di Omega/(10^10) (v.
         * pg. 443 Computing Poisson Probabilities). Dove con Omega si intende
         * il massimo valore rappresentabile.
         */
        double LL = L;
        double RR = R;
        double omega_m = omega / (Math.pow((double) 10, (double) 10) * (RR - LL));

        int mm = (int) m - L; // indice del valore della moda translato sul vettore tra 0 ed R-L
        weights[mm] = omega_m;

        // Caso L
        int j = (int) m;

        while (j > L) {
            weights[mm - 1] = ((double) j / lambda) * weights[mm];
            mm = mm - 1;
            j = j - 1;
        }

        if (lambda < 400 && R > 600) {
            data.setNoUnderflow(false);
            return;
        }

        if (lambda < 400 && R <= 600) {
            j = (int) m;
            mm = (int) m - L;
            double q = 0.0;
            while (j < R) {
                q = lambda / (((double) j) + ((double) 1));
                if (weights[mm] > (tau / q)) {
                    weights[mm + 1] = q * weights[mm];
                    mm = mm + 1;
                    j = j + 1;
                } else {
                    R = j;
                }
            }
        }

        if (lambda >= 400) {
            j = (int) m;
            mm = (int) m - L;
            while (j < R) {
                weights[mm + 1] = (lambda / (((double) j) + ((double) 1))) * weights[mm];
                j = j + 1;
                mm = mm + 1;
            }
        }

        double totalWeight = computeW(L, R, weights);

        data.setRightTruncPoint(R);
        data.setFoxGlynnTotalWeight(totalWeight);
        data.setFoxGlynnWeights(weights);
    }

    /**
     * This method compute the Total Weight W adding the weights between L and
     * R. For numerical stability (see Computing Poisson Probabilities pg. 441)
     * small weights are added first.
     *
     * @param rightTruncPoint
     *            the right truncation point
     * @param leftTruncPoint
     *            the left truncation point
     * @param weights
     *            the array of weights
     * @return the Total Weight W
     */
    private static double computeW(int leftTruncPoint, int rightTruncPoint, double[] weights) {
        int s = 0; // L-L
        int t = rightTruncPoint - leftTruncPoint;
        double W = 0.0;
        while (s < t) {
            if (weights[s] <= weights[t]) {
                W = W + weights[s];
                s = s + 1;
            } else {
                W = W + weights[t];
                t = t - 1;
            }
        }
        // A questo punto s==t e rappresenta la moda.
        // Quindi sommo il weights centrale: omega_m.
        W = W + weights[s];
        return W;
    }
}