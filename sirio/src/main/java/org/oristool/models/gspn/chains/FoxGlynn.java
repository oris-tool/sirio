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

package org.oristool.models.gspn.chains;

/**
 * Computation of Poisson probabilities using Fox-Glynn algorithm.
 *
 * <p>This implementation is based on:
 * <ul>
 * <li><i>Computing Poisson Probabilities</i> by Bennet L. Fox and Peter W.
 * Glynn (1988) for the "Weighter" algorithm.
 *
 * <li><i>Understanding Fox and Glynn's "Computing Poisson probabilities"</i> by
 * David N. Jansen (2011) for the "Finder" algorithm.
 * </ul>
 */
public final class FoxGlynn implements Interval {
    private static final double UF = Double.MIN_NORMAL;
    private static final double OF = Double.MAX_VALUE;
    private static final double LOG_UF = Math.log(UF);

    private static final double SQRT_2 = Math.sqrt(2.0);
    private static final double SQRT_2PI = Math.sqrt(2.0 * Math.PI);
    private static final double LOG_CM = -1.0 - 1.0 / (12.0 * 25.0) - Math.log(SQRT_2PI);

    private final int leftPoint;
    private final int rightPoint;
    private final double[] weights;
    private final double totalWeight;

    private FoxGlynn(int leftPoint, int rightPoint,
             double[] weights, double totalWeight) {

        this.leftPoint = leftPoint;
        this.rightPoint = rightPoint;
        this.weights = weights;
        this.totalWeight = totalWeight;
    }

    /**
     * Returns the point used to truncate the left tail of the Poisson distribution.
     *
     * @return first point of the Poisson distribution approximation
     */
    @Override
    public int leftPoint() {
        return leftPoint;
    }

    /**
     * Returns the point used to truncate the right tail of the Poisson distribution.
     *
     * @return last point of the Poisson distribution approximation
     */
    @Override
    public int rightPoint() {
        return rightPoint;
    }

    /**
     * Returns the weight of a point in the approximation.
     *
     * <p>The input index must belong to the range
     * {@code [leftPoint(), rightPoint()]}.
     *
     * @param i time point
     * @return weight for the given point
     * @throws IndexOutOfBoundsException unless
     *         {@code leftPoint() <= i <= rightPoint()}
     */
    public double weight(int i) {
        return weights[i - leftPoint];
    }

    /**
     * Returns a normalization constant for the Poisson approximation.
     *
     * @return normalization constant
     */
    public double totalWeight() {
        return totalWeight;
    }

    /**
     * Returns the Poisson probability for a point in the approximation.
     *
     * <p>The input index must belong to the range
     * {@code [leftPoint(), rightPoint()]}.
     *
     * @param i time point
     * @return Poisson probability for the given point
     * @throws IndexOutOfBoundsException unless
     *         {@code leftPoint() <= i <= rightPoint()}
     */
    public double poissonProb(int i) {

        return weights[i - leftPoint] / totalWeight;
    }

    /**
     * Computes a reduced Fox-Glynn approximation of Poisson probabilities.
     *
     * <p>After bounding tails and computing Poisson probabilities with Fox-Glynn
     * algorithm, a simple heuristic is applied to reduce the size of the
     * distribution (under the target error).
     *
     * @param lambda rate of the Poisson distribution
     * @param error the maximum allowed value of probabilities not included
     * @return truncation points and weights/probabilities between them
     * @throws IllegalStateException if underflow can occur or the tails cannot be
     *         bounded
     */
    public static FoxGlynn computeReduced(double lambda, double error) {

        FoxGlynn result = FoxGlynn.compute(lambda, error);

        int mode = (int) lambda;
        assert result.leftPoint <= mode && mode <= result.rightPoint;

        int left = mode;
        int right = mode;
        double totalProb = result.poissonProb(mode);

        // extend left or right until target error is reached
        while (totalProb < 1.0 - error / 2.0) {
            if (left > result.leftPoint) {
                if (right == result.rightPoint
                        || result.poissonProb(left - 1) >= result.poissonProb(right + 1)) {
                    totalProb += result.poissonProb(--left);
                } else {
                    totalProb += result.poissonProb(++right);
                }

            } else if (right < result.rightPoint) {
                totalProb += result.poissonProb(++right);

            } else {
                return result;
            }
        }

        int length = right - left + 1;
        double[] weights = new double[length];
        System.arraycopy(result.weights, left - result.leftPoint, weights, 0, length);
        return new FoxGlynn(left, right, weights, result.totalWeight);
    }

    /**
     * Computes the Fox-Glynn approximation of Poisson probabilities.
     *
     * @param lambda rate of the Poisson distribution
     * @param error the maximum allowed value of probabilities not included
     * @return truncation points and weights/probabilities between them
     * @throws IllegalStateException if underflow can occur or the tails cannot be
     *         bounded
     */
    public static FoxGlynn compute(double lambda, double error) {

        if (lambda == 0.0)
            throw new IllegalStateException("Possible underflow: lambda equal to 0");

        if (error < UF)
            throw new IllegalStateException("Allowed error must be >= Double.MIN_NORMAL");

        int mode = (int) lambda;
        int leftPoint = findLeftPoint(mode, lambda, error);
        int rightPoint = findRightPoint(mode, lambda, error);
        checkUnderflow(mode, lambda, leftPoint, rightPoint);

        double[] weights = computeWeights(mode, lambda, leftPoint, rightPoint);
        if (weights.length != rightPoint - leftPoint + 1)
            rightPoint = leftPoint + weights.length - 1;

        double totalWeight = accurateTotal(weights);

        return new FoxGlynn(leftPoint, rightPoint, weights, totalWeight);
    }

    private static int findLeftPoint(int mode, double lambda, double error) {

        if (mode < 25) {
            if (-lambda < LOG_UF)
                throw new IllegalStateException("Possible underflow on k=0");
            return 0;

        } else {
            double sqrt_lambda = Math.sqrt(lambda);
            double inv_lambda = 1.0 / lambda;
            double b = (1.0 + inv_lambda) * Math.exp(0.125 * inv_lambda);

            int leftPoint;
            double errorBound;
            int k = 3;  // try all values after 3
            do {
                k++;
                leftPoint = mode - (int) Math.ceil(k * sqrt_lambda + 0.5);
                if (leftPoint <= 0)
                    return 0;
                errorBound = b * (2 / SQRT_2PI) * Math.exp(-k * k / 2.0) / k;
            } while (errorBound > error);

            return leftPoint;
        }
    }

    private static int findRightPoint(int mode, double lambda, double error) {

        if (mode < 400) {
            mode = 400;
            lambda = 400.0;
        }

        double sqrt_2lambda = Math.sqrt(2.0 * lambda);
        double a = (1.0 + 1.0 / lambda) * Math.exp(1.0 / 16.0) * SQRT_2;

        double errorBound;
        int k = 3;  // try all values after 3
        do {
            k++;
            double inv_d = 1 - Math.exp(-266.0 / 401.0 * (k * sqrt_2lambda + 1.5));
            errorBound = a / inv_d * (2 / SQRT_2PI) * Math.exp(-k * k / 2.0) / k;
        } while (errorBound > error);

        int rightPoint = mode + (int) Math.ceil(k * sqrt_2lambda + 0.5);
        if (rightPoint > mode + (int) Math.ceil((lambda + 1.0) / 2.0))
            throw new IllegalStateException("Cannot bound the right tail at k=" + k);

        if (rightPoint <= 0)
            throw new IllegalStateException("Lambda too large: integer overflow of right point");

        return rightPoint;
    }

    private static double modeWeight(int leftPoint, int rightPoint) {
        return OF * 10e-10 / (rightPoint - leftPoint + 1);
    }

    private static void checkUnderflow(int mode, double lambda, int leftPoint, int rightPoint) {

        double baseValue = LOG_CM - Math.log(mode) / 2.0;
        double safeBound = LOG_UF - Math.log(modeWeight(leftPoint, rightPoint));

        if (mode >= 25) {
            int leftSamples = mode - leftPoint;
            double checkValue;
            if (mode <= 2 * leftPoint) {
                checkValue = baseValue
                        - leftSamples * (leftSamples + 1)
                        * (0.5 + (2 * leftSamples + 1) / (6 * lambda)) / lambda;
            } else {
                checkValue = Math.max(-lambda, baseValue
                        + leftSamples * Math.log(1 - leftSamples / (double)(mode + 1)));
            }

            if (checkValue < safeBound)
                throw new IllegalStateException("Possible underflow at left truncation point");
        }

        if (mode >= 400) {
            int rightSamples = rightPoint - mode;
            double checkValue = baseValue - (rightSamples + 1) * (rightSamples + 1) / (2 * lambda);
            if (checkValue < safeBound)
                throw new IllegalStateException("Possible underflow at right truncation point");
        }
    }

    private static double[] computeWeights(int mode, double lambda, int leftPoint, int rightPoint) {

        // set the weight of the mode arbitrarily
        double[] weights = new double[rightPoint - leftPoint + 1];
        int modePos = mode - leftPoint;
        weights[modePos] = modeWeight(leftPoint, rightPoint);

        // compute weights from (mode-1) to leftPoint
        for (int j = modePos - 1; j >= 0; j--) {
            weights[j] = (j + 1 + leftPoint) / lambda * weights[j + 1];
        }

        // compute weights from (mode+1) to rightPoint
        if (mode >= 400) {
            for (int j = modePos + 1; j < weights.length; j++) {
                weights[j] = lambda / (j + leftPoint) * weights[j - 1];
            }

        } else {
            if (rightPoint > 600)
                throw new IllegalStateException("Possible underflow");

            for (int j = modePos + 1; j < weights.length; j++) {
                double q = lambda / (j + leftPoint);
                if (weights[j - 1] > UF / q) {
                    weights[j] = q * weights[j - 1];
                } else {
                    int newRightPoint = j - 1 + leftPoint;
                    int newLength = newRightPoint - leftPoint + 1;
                    double[] newWeights = new double[newLength];
                    System.arraycopy(weights, 0, newWeights, 0, newLength);
                    return newWeights;
                }
            }
        }

        return weights;
    }

    private static double accurateTotal(double[] weights) {

        double totalWeight = 0.0;
        for (int l = 0, r = weights.length - 1; l <= r; ) {
            if (weights[l] <= weights[r])
                totalWeight += weights[l++];
            else
                totalWeight += weights[r--];
        }

        return totalWeight;
    }
}

