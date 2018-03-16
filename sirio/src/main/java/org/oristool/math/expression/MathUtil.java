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

package org.oristool.math.expression;

import java.math.BigInteger;

/**
 * Mathematical calculations.
 */
public final class MathUtil {
    /**
     * Computes the binomial coefficient "n choose k".
     *
     * @param n set size
     * @param k elements to choose
     * @return number of ways to choose k elements, disregarding their order
     */
    public static BigInteger calculateBinomialCoefficient(Integer n, Integer k) {
        if (k < 0 || n < 0 || k > n)
            throw new IllegalArgumentException(
                    "For the binomial coefficient should be k>=0 && n>=0 && k<=n");

        if (k == 0 || n == k)
            return new BigInteger("1");
        else
            return calculateFactorial(n).divide(
                    calculateFactorial(n - k).multiply(calculateFactorial(k)));
    }

    /**
     * Computes the factorial n!.
     *
     * @param n set size
     * @return distinct ordered permutations of n objects
     */
    public static BigInteger calculateFactorial(int n) {

        if (n < 0)
            throw new IllegalArgumentException("The argument of a factorial should be positive");

        if (n == 0 || n == 1)
            return new BigInteger("1");

        BigInteger factorial = new BigInteger("2");

        for (int i = 3; i <= n; i++)
            factorial = factorial.multiply(BigInteger.valueOf(i));

        return factorial;
    }
}
