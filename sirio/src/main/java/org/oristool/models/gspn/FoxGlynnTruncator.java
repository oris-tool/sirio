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

import org.oristool.analyzer.log.AnalysisLogger;

/**
 * Finds the truncation points according to Fox-Glynn.
 */
class FoxGlynnTruncator {

    public static void setTruncationPoints(DataContainer data) {
        final double lambda = data.getLambda();
        final double m = Math.floor(lambda);
        final double wantedAccuracy = data.getRequiredAccuracy();
        final double underFlowLimit = data.getUnderFlowLimit();
        final double overFlowLimit = data.getOverFlowLimit();
        final double foxGlynnModeWeight = data.getFoxGlynnModeWeight();
        boolean F = data.isNoUnderflow();
        final AnalysisLogger log = data.getLogger();

        int L = data.getLeftTruncPoint();
        int R = data.getRightTruncPoint();

        // lambda should be > 0
        double halfwantedAccuracy = wantedAccuracy / ((double) 2);
        double kOut_L = 0.0;
        double kOut_R = 0.0;

        // CALCULATE R: based on lambda values
        boolean rFound = false;
        if (lambda < 400) {

            // apply corollary 1 with lambda=400 pg. 444
            double lambda_app = 400.0; // fixed lambda by the algorithm
            double k = ((double) 1) / (((double) 2) * Math.sqrt(((double) 2) * lambda_app));
            double top = (Math.sqrt(lambda_app) / (((double) 2) * Math.sqrt(((double) 2))));
            double a_lambda = (Double.valueOf(1).doubleValue()
                    + (Double.valueOf(1).doubleValue() / lambda_app))
                    * Math.sqrt(Double.valueOf(2).doubleValue())
                    * Math.exp((Double.valueOf(1).doubleValue() / (double) 16));

            double qUpperBound = 0.0;

            double a0;
            double a1;
            double b;
            double c;
            double e;
            double dd;

            for (double y = k; y <= top; y = y + 1) {

                a0 = Math.sqrt(Double.valueOf(2).doubleValue() * lambda_app);
                a1 = a0 * y;
                b = a1 + 1.5;
                c = (-2.0 / 9.0) * b;
                e = Math.exp(c);
                dd = 1.0 / (1.0 - e);

                qUpperBound = (a_lambda * dd * Math.exp(-((y * y) / ((double) 2))))
                        / (y * (Math.sqrt(((double) 2) * Math.PI)));

                if (qUpperBound <= halfwantedAccuracy) {
                    int r = (int) Math.round((m + (y * Math.sqrt(((double) 2) * lambda_app))
                            + Double.valueOf(1.5).doubleValue()));
                    R = Integer.valueOf(r);
                    kOut_R = y;
                    rFound = true;

                    break;
                }
            }

            // if R was not found, we need a better precision on L.
            // So set R as best possible then set the new accuracy for L.
            if (!rFound) {
                k = Math.sqrt(lambda_app) / (((double) 2) * Math.sqrt(((double) 2)));
                kOut_R = k;
                R = Integer.valueOf((int) Math.round((m + (k * Math.sqrt(((double) 2) * lambda_app))
                        + (((double) 3) / ((double) 2)))));
                dd = ((double) 1) / (((double) 1) - Math.exp(
                        (-(((double) 2) / ((double) 9))) * (k * Math.sqrt(((double) 2) * lambda_app)
                                + (((double) 3) / ((double) 2)))));
                qUpperBound = (a_lambda * dd * Math.exp(-((k * k) / ((double) 2))))
                        / (k * (Math.sqrt(((double) 2) * Math.PI)));
                halfwantedAccuracy = wantedAccuracy - qUpperBound;
            }
        } else {
            // apply corollary 1, leave lambda as is
            // System.out.println("\t Case: (lambda >= 400) --> apply
            // corollary1");
            double k = ((double) 1) / (((double) 2) * Math.sqrt(((double) 2)));
            double lambda_app = lambda;
            double top = (Math.sqrt(lambda_app) / (((double) 2) * Math.sqrt(((double) 2))));
            double a_lambda = (((double) 1) + (((double) 1) / lambda_app)) * Math.sqrt(((double) 2))
                    * Math.exp(((double) 1) / ((double) 16));

            double d;
            double qUpperBound;

            for (; k <= top; k = k + 1) {
                d = ((double) 1) / (((double) 1) - Math.exp(
                        (-(((double) 2) / ((double) 9))) * (k * Math.sqrt(((double) 2) * lambda_app)
                                + Double.valueOf(1.5).doubleValue())));

                qUpperBound = (a_lambda * d * Math.exp(-((k * k) / ((double) 2))))
                        / (k * (Math.sqrt(((double) 2) * Math.PI)));

                if (qUpperBound <= halfwantedAccuracy) {
                    int r = (int) Math.round((m + (k * Math.sqrt(((double) 2) * lambda_app))
                            + Double.valueOf(1.5).doubleValue()));
                    R = Integer.valueOf(r);
                    kOut_R = k;
                    rFound = true;
                    break;
                }
            }

            if (!rFound) {
                k = Math.sqrt(lambda_app) / (((double) 2) * Math.sqrt(((double) 2)));
                kOut_R = k;
                R = Integer.valueOf((int) Math.round((m + (k * Math.sqrt(((double) 2) * lambda_app))
                        + Double.valueOf(1.5).doubleValue())));
                d = ((double) 1) / (((double) 1) - Math.exp(
                        (-(((double) 2) / ((double) 9))) * (k * Math.sqrt(((double) 2) * lambda_app)
                                + Double.valueOf(1.5).doubleValue())));
                qUpperBound = (a_lambda * d * Math.exp(-((k * k) / ((double) 2))))
                        / (k * (Math.sqrt(((double) 2) * Math.PI)));
                halfwantedAccuracy = wantedAccuracy - qUpperBound;

            }
        }

        boolean lFound = false;
        if (lambda < 25) {
            L = Integer.valueOf(0);
            kOut_L = 0;
            F = false;
        } else {
            double k = ((double) 1) / (Math.sqrt(((double) 2) * lambda));
            double b_lambda = (((double) 1) + (((double) 1) / lambda))
                    * (Math.exp(((double) 1) / (((double) 8) * lambda)));
            double kUpperBound = 1000000.0;
            double t_lambdaUpperBound;
            for (; k < kUpperBound; k = k + 1) {
                t_lambdaUpperBound = (b_lambda * Math.exp(-((k * k) / ((double) 2))))
                        / (k * Math.sqrt(((double) 2) * (Math.PI)));

                if (t_lambdaUpperBound < halfwantedAccuracy) {
                    L = Integer.valueOf((int) Math.floor(
                            m - (k * Math.sqrt(lambda)) - Double.valueOf(1.5).doubleValue()));

                    if (L < 0) {
                        L = 0;
                    }

                    kOut_L = k;
                    lFound = true;
                    break;
                }
            }
        }

        if (!lFound)
            throw new IllegalStateException();

        F = checkF(lambda, underFlowLimit, overFlowLimit, kOut_L, kOut_R, m, L, R, log);

        data.setLambda(lambda);
        data.setLeftTruncPoint(L);
        data.setRightTruncPoint(R);
        data.setNoUnderflow(F);
        data.setFoxGlynnModeWeight(foxGlynnModeWeight);
    }

    /**
     * Evaluates some parameters and returns the Fox & Glynn boolean flag.
     *
     * @param lambda the rate of the uniformization (input)
     * @param underFlowLimit the underflow precision of the machine (input)
     * @param overFlowLimit the overflow precision of the machine (input)
     * @param outLeftK value of last k in the helper function for L
     * @param outRightK value of last k in the helper function for R
     * @param m the mode of the Poisson distribution
     * @param leftTruncPoint the left truncation point (output)
     * @param rightTruncPoint the right truncation point (output)
     * @param log logger
     * @return the flag F value of the finder algorithm
     */
    private static boolean checkF(double lambda, double underFlowLimit, double overFlowLimit,
            double outLeftK, double outRightK, double m, int leftTruncPoint, int rightTruncPoint,
            AnalysisLogger log) {

        if (lambda == 0.0) {
            return false;
        }

        if ((lambda < 25) && (Math.exp(-lambda) < underFlowLimit)) {
            return false;
        }

        // Bounding Poisson Probabilities, pg.444 in basso. Variabile di appoggio.
        double c_m = (((double) 1) / Math.sqrt(((double) 2) * (Math.PI) * m))
                * Math.exp(m - lambda - (((double) 1) / (((double) 12) * m)));
        // Calcolo del lower bound di R, utilizzando il corollario 3 di pg. 444
        double k_r = outRightK * Math.sqrt(((double) 2))
                + (((double) 3) / (((double) 2) * Math.sqrt(lambda)));
        double lowerBoundR = c_m
                * Math.exp(-((k_r + ((double) 1)) * (k_r + ((double) 1))) / ((double) 2));

        // Calcolo del lower bound di L, utilizzando il corollario 4 di pg. 444
        double k_l = outLeftK + ((double) 3) / (((double) 2) * Math.sqrt(lambda));
        double lowerBoundL = 0.0;
        if ((k_l > 0) && (k_l <= Math.sqrt(lambda) / ((double) 2))) {
            lowerBoundL = c_m * Math.exp(-((k_l * k_l) / ((double) 2))
                    - (Math.pow(k_l, 3) / (((double) 3) * Math.sqrt(lambda))));
        } else {
            double lowerBoundL_ii = 0.0;
            double lowerBoundL_iii = 0.0;
            if (k_l <= Math.sqrt((m + ((double) 1 / m)))) {
                lowerBoundL_ii = c_m
                        * Math.pow((((double) 1) - (k_l / (Math.sqrt(m + ((double) 1))))),
                                (k_l * Math.sqrt(m + ((double) 1))));
                lowerBoundL_iii = Math.exp(-lambda);
                lowerBoundL = Math.max(lowerBoundL_ii, lowerBoundL_iii);
            } else {
                if (log != null) {
                    log.log("Corollary4: FAIL\n");
                }
                return false;
            }
        }

        // Caso particolare: Controllo su L
        double RR = (double)rightTruncPoint;
        double LL = (double)leftTruncPoint;
        if ((lambda >= 25) && ((lowerBoundL * overFlowLimit)
                / (Math.pow(10, 10) * (RR - LL)) <= underFlowLimit)) {
            return false;
        }

        // Caso particolare: controllo su R
        if ((lambda >= 400) && ((lowerBoundR * overFlowLimit)
                / (Math.pow(10, 10) * (RR - LL)) < underFlowLimit)) {
            return false;
        }

        return true;
    }
}