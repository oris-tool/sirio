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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Utility class for mathematical calculations.
 */
public class Util {
    /**
     * Calcola il coefficiente binomiale di <code>n</code> su <code>k</code>
     * (con <code>n</code> e <code>k</code> positivi, e <code>n>=k</code>)
     * 
     * @param n
     *            primo parametro del coefficiente
     * @param k
     *            secondo parametro del coefficiente
     * @return coefficiente binomiale "<code>n</code> su <code>k</code>"
     * @throws FactorialException
     *             errore nel calcolo del fattoriale
     * @throws BinomialException
     *             parametri in ingresso non validi
     */
    public static BigInteger calculateBinomialCoefficient(Integer n, Integer k) {
        if (k < 0 || n < 0 || k > n)
            throw new IllegalArgumentException(
                    "For the binomial coefficient should be k>=0 && n>=0 && k<=n");

        if (k == 0 || n == k)
            return new BigInteger("1");
        else
            return calculateFactorial(n)
                    .divide((calculateFactorial(n - k)
                            .multiply(calculateFactorial(k))));
    }

    /**
     * Calcola il fattoriale di un numero non negativo
     * 
     * @param n
     *            numero di cui calcolare il fattoriale
     * @return <code>n!</code>, ovvero il fattoriale di <code>n</code>
     * @throws FactorialException
     *             numero in ingresso negativo
     */
    public static BigInteger calculateFactorial(Integer n) {

        if (n < 0)
            throw new IllegalArgumentException(
                    "The argument of a factorial should be positive");

        if (n == 0 || n == 1)
            return new BigInteger("1");

        BigInteger factorial = new BigInteger("2");

        for (int i = 3; i <= n; i++)
            factorial = factorial.multiply(new BigInteger("" + i));

        return factorial;
    }

    /*********** FUNZIONI DI TEST PER CALCOLARE exp(lambda) sui BigDecimal **********/

    /**
     * Compute x^exponent to a given scale. Uses the same algorithm as class
     * numbercruncher.mathutils.IntPower.
     * 
     * @param x
     *            the value x
     * @param exponent
     *            the exponent value
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal intPower(BigDecimal x, long exponent, int scale) {
        // If the exponent is negative, compute 1/(x^-exponent).
        if (exponent < 0) {
            return BigDecimal.valueOf(1).divide(intPower(x, -exponent, scale),
                    scale, RoundingMode.HALF_EVEN);
        }

        BigDecimal power = BigDecimal.valueOf(1);

        // Loop to compute value^exponent.
        while (exponent > 0) {

            // Is the rightmost bit a 1?
            if ((exponent & 1) == 1) {
                power = power.multiply(x).setScale(scale,
                        RoundingMode.HALF_EVEN);
            }

            // Square x and shift exponent 1 bit to the right.
            x = x.multiply(x).setScale(scale, RoundingMode.HALF_EVEN);
            exponent >>= 1;

            Thread.yield();
        }

        return power;
    }

    /**
     * Compute the integral root of x to a given scale, x >= 0. Use Newton's
     * algorithm.
     * 
     * @param x
     *            the value of x
     * @param index
     *            the integral root value
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal intRoot(BigDecimal x, long index, int scale) {
        // Check that x >= 0.
        if (x.signum() < 0) {
            throw new IllegalArgumentException("x < 0");
        }

        int sp1 = scale + 1;
        BigDecimal n = x;
        BigDecimal i = BigDecimal.valueOf(index);
        BigDecimal im1 = BigDecimal.valueOf(index - 1);
        BigDecimal tolerance = BigDecimal.valueOf(5).movePointLeft(sp1);
        BigDecimal xPrev;

        // The initial approximation is x/index.
        x = x.divide(i, scale, RoundingMode.HALF_EVEN);

        // Loop until the approximations converge
        // (two successive approximations are equal after rounding).
        do {
            // x^(index-1)
            BigDecimal xToIm1 = intPower(x, index - 1, sp1);

            // x^index
            BigDecimal xToI = x.multiply(xToIm1).setScale(sp1,
                    RoundingMode.HALF_EVEN);

            // n + (index-1)*(x^index)
            BigDecimal numerator = n.add(im1.multiply(xToI)).setScale(sp1,
                    RoundingMode.HALF_EVEN);

            // (index*(x^(index-1))
            BigDecimal denominator = i.multiply(xToIm1).setScale(sp1,
                    RoundingMode.HALF_EVEN);

            // x = (n + (index-1)*(x^index)) / (index*(x^(index-1)))
            xPrev = x;
            x = numerator.divide(denominator, sp1, RoundingMode.DOWN);

            Thread.yield();
        } while (x.subtract(xPrev).abs().compareTo(tolerance) > 0);

        return x;
    }

    /**
     * Compute e^x to a given scale. Break x into its whole and fraction parts
     * and compute (e^(1 + fraction/whole))^whole using Taylor's formula.
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal exp(BigDecimal x, int scale) {
        // e^0 = 1
        if (x.signum() == 0) {
            return BigDecimal.valueOf(1);
        }

        // If x is negative, return 1/(e^-x).
        else if (x.signum() == -1) {
            return BigDecimal.valueOf(1).divide(exp(x.negate(), scale), scale,
                    RoundingMode.HALF_EVEN);
        }

        // Compute the whole part of x.
        BigDecimal xWhole = x.setScale(0, RoundingMode.DOWN);

        // If there isn't a whole part, compute and return e^x.
        if (xWhole.signum() == 0)
            return expTaylor(x, scale);

        // Compute the fraction part of x.
        BigDecimal xFraction = x.subtract(xWhole);

        // z = 1 + fraction/whole
        BigDecimal z = BigDecimal.valueOf(1).add(
                xFraction.divide(xWhole, scale, RoundingMode.HALF_EVEN));

        // t = e^z
        BigDecimal t = expTaylor(z, scale);

        BigDecimal maxLong = BigDecimal.valueOf(Long.MAX_VALUE);
        BigDecimal result = BigDecimal.valueOf(1);

        // Compute and return t^whole using intPower().
        // If whole > Long.MAX_VALUE, then first compute products
        // of e^Long.MAX_VALUE.
        while (xWhole.compareTo(maxLong) >= 0) {
            result = result.multiply(intPower(t, Long.MAX_VALUE, scale))
                    .setScale(scale, RoundingMode.HALF_EVEN);
            xWhole = xWhole.subtract(maxLong);

            Thread.yield();
        }
        return result.multiply(intPower(t, xWhole.longValue(), scale))
                .setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Compute e^x to a given scale by the Taylor series.
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    private static BigDecimal expTaylor(BigDecimal x, int scale) {
        BigDecimal factorial = BigDecimal.valueOf(1);
        BigDecimal xPower = x;
        BigDecimal sumPrev;

        // 1 + x
        BigDecimal sum = x.add(BigDecimal.valueOf(1));

        // Loop until the sums converge
        // (two successive sums are equal after rounding).
        int i = 2;
        do {
            // x^i
            xPower = xPower.multiply(x).setScale(scale,
                    RoundingMode.HALF_EVEN);

            // i!
            factorial = factorial.multiply(BigDecimal.valueOf(i));

            // x^i/i!
            BigDecimal term = xPower.divide(factorial, scale,
                    RoundingMode.HALF_EVEN);

            // sum = sum + x^i/i!
            sumPrev = sum;
            sum = sum.add(term);

            ++i;
            Thread.yield();
        } while (sum.compareTo(sumPrev) != 0);

        return sum;
    }

    /**
     * Compute the natural logarithm of x to a given scale, x > 0.
     */
    public static BigDecimal ln(BigDecimal x, int scale) {
        // Check that x > 0.
        if (x.signum() <= 0) {
            throw new IllegalArgumentException("x <= 0");
        }

        // The number of digits to the left of the decimal point.
        int magnitude = x.toString().length() - x.scale() - 1;

        if (magnitude < 3) {
            return lnNewton(x, scale);
        }

        // Compute magnitude*ln(x^(1/magnitude)).
        else {

            // x^(1/magnitude)
            BigDecimal root = intRoot(x, magnitude, scale);

            // ln(x^(1/magnitude))
            BigDecimal lnRoot = lnNewton(root, scale);

            // magnitude*ln(x^(1/magnitude))
            return BigDecimal.valueOf(magnitude).multiply(lnRoot)
                    .setScale(scale, RoundingMode.HALF_EVEN);
        }
    }

    /**
     * Compute the natural logarithm of x to a given scale, x > 0. Use Newton's
     * algorithm.
     */
    private static BigDecimal lnNewton(BigDecimal x, int scale) {
        int sp1 = scale + 1;
        BigDecimal n = x;
        BigDecimal term;

        // Convergence tolerance = 5*(10^-(scale+1))
        BigDecimal tolerance = BigDecimal.valueOf(5).movePointLeft(sp1);

        // Loop until the approximations converge
        // (two successive approximations are within the tolerance).
        do {

            // e^x
            BigDecimal eToX = exp(x, sp1);

            // (e^x - n)/e^x
            term = eToX.subtract(n).divide(eToX, sp1, RoundingMode.DOWN);

            // x - (e^x - n)/e^x
            x = x.subtract(term);

            Thread.yield();
        } while (term.compareTo(tolerance) > 0);
        return x.setScale(scale, RoundingMode.HALF_EVEN);

    }

    /**
     * Compute the arctangent of x to a given scale, |x| < 1
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal arctan(BigDecimal x, int scale) {
        // Check that |x| < 1.
        if (x.abs().compareTo(BigDecimal.valueOf(1)) >= 0) {
            throw new IllegalArgumentException("|x| >= 1");
        }

        // If x is negative, return -arctan(-x).
        if (x.signum() == -1) {
            return arctan(x.negate(), scale).negate();
        } else {
            return arctanTaylor(x, scale);
        }
    }

    /**
     * Compute the arctangent of x to a given scale by the Taylor series, |x| <
     * 1
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    private static BigDecimal arctanTaylor(BigDecimal x, int scale) {
        int sp1 = scale + 1;
        int i = 3;
        boolean addFlag = false;

        BigDecimal power = x;
        BigDecimal sum = x;
        BigDecimal term;

        // Convergence tolerance = 5*(10^-(scale+1))
        BigDecimal tolerance = BigDecimal.valueOf(5)

        .movePointLeft(sp1);
        // Loop until the approximations converge
        // (two successive approximations are within the tolerance).
        do {
            // x^i
            power = power.multiply(x).multiply(x)
                    .setScale(sp1, RoundingMode.HALF_EVEN);

            // (x^i)/i
            term = power.divide(BigDecimal.valueOf(i), sp1,
                    RoundingMode.HALF_EVEN);

            // sum = sum +- (x^i)/i
            sum = addFlag ? sum.add(term) : sum.subtract(term);

            i += 2;
            addFlag = !addFlag;

            Thread.yield();
        } while (term.compareTo(tolerance) > 0);

        return sum;
    }

    /**
     * Compute the square root of x to a given scale, x >= 0. Use Newton's
     * algorithm.
     * 
     * @param x
     *            the value of x
     * @param scale
     *            the desired scale of the result
     * @return the result value
     */
    public static BigDecimal sqrt(BigDecimal x, int scale) {
        // Check that x >= 0.
        if (x.signum() < 0) {
            throw new IllegalArgumentException("x < 0");
        }

        // n = x*(10^(2*scale))
        BigInteger n = x.movePointRight(scale << 1).toBigInteger();

        // The first approximation is the upper half of n.
        int bits = (n.bitLength() + 1) >> 1;
        BigInteger ix = n.shiftRight(bits);
        BigInteger ixPrev;

        // Loop until the approximations converge
        // (two successive approximations are equal after rounding).
        do {
            ixPrev = ix;

            // x = (x + n/x)/2
            ix = ix.add(n.divide(ix)).shiftRight(1);

            Thread.yield();
        } while (ix.compareTo(ixPrev) != 0);

        return new BigDecimal(ix, scale);
    }

    class Big_pi {
        BigDecimal epsilon; // based on desired precision
        BigDecimal natural_e;
        BigDecimal pi;
        int prec = 301; // precision in digits
        int bits = 1000; // precision in bits = about 3.32 precision in digits

        public Big_pi() // constructor
        {
            // BigDecimal a = new BigDecimal("0.12345678901234567890");
            // BigDecimal b = new BigDecimal("0.2345678901234567890");
            // BigDecimal c;
            // BigDecimal npi;

            // "constants" needed by other functions
            natural_e = naturalE(prec); /* precision */
            epsilon = new BigDecimal("1");
            for (int i = 0; i < bits; i++) {
                epsilon = epsilon.multiply(new BigDecimal("0.5"));
            }
        }

        public BigDecimal exp_series(BigDecimal x) // abs(x)<=0.5, prec digits
        { // prec digits returned
            BigDecimal fact = new BigDecimal("1"); // factorial
            BigDecimal xp = new BigDecimal("1"); // power of x
            BigDecimal y = new BigDecimal("1"); // sum of series on x
            int n;

            n = (2 * prec) / 3;
            for (int i = 1; i < n; i++) {
                fact = fact.multiply(new BigDecimal(i));
                fact = fact.setScale(prec, RoundingMode.DOWN);
                xp = xp.multiply(x);
                xp = xp.setScale(prec, RoundingMode.DOWN);
                y = y.add(xp.divide(fact, RoundingMode.DOWN));
            }
            y = y.setScale(prec, RoundingMode.DOWN);
            return y;
        }

        public BigDecimal exp(BigDecimal x) {
            BigDecimal y = new BigDecimal("1.0"); // sum of series on xc
            BigDecimal xc; // x - j
            BigDecimal ep = natural_e; // e^j
            BigDecimal j = new BigDecimal("1");
            BigDecimal one = new BigDecimal("1.0");
            BigDecimal half = new BigDecimal("0.5");
            BigDecimal xp; // positive, then invert

            if (x.abs().compareTo(half) < 0)
                return exp_series(x);
            if (x.compareTo(new BigDecimal("0")) > 0) // positive
            {
                while (x.compareTo(j.add(one)) > 0) {
                    ep = ep.multiply(natural_e);
                    j = j.add(one);
                }
                xc = x.subtract(j);
                y = ep.multiply(exp_series(xc));
                y = y.setScale(prec, RoundingMode.DOWN);
                return y;
            } else // negative
            {
                xp = x.negate();
                while (xp.compareTo(j.add(one)) > 0) {
                    ep = ep.multiply(natural_e);
                    j = j.add(one);
                }
                xc = xp.subtract(j);
                y = ep.multiply(exp_series(xc));
                y = y.setScale(prec, RoundingMode.DOWN);
                return (one.add(epsilon)).divide(y, RoundingMode.DOWN);
            }
        } // end exp

        public BigDecimal naturalE(int prec_dig) {
            BigDecimal sum = new BigDecimal("1");
            BigDecimal fact = new BigDecimal("1");
            BigDecimal del = new BigDecimal("1");
            BigDecimal one = new BigDecimal("1");
            BigDecimal ten = new BigDecimal("10");
            int prec_bits = (prec_dig * 332) / 100;

            one = one.setScale(prec_dig, RoundingMode.DOWN);
            for (int i = 0; i < prec_dig; i++)
                del = del.multiply(ten);
            for (int i = 1; i < prec_bits; i++) {
                fact = fact.multiply(new BigDecimal(i));
                fact = fact.setScale(prec_dig, RoundingMode.DOWN);
                sum = sum.add(one.divide(fact, RoundingMode.DOWN));
                if (del.compareTo(fact) < 0)
                    break;
            }
            return sum.setScale(prec_dig, RoundingMode.DOWN);
        }
    }
}
