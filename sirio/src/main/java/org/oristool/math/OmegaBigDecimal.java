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

package org.oristool.math;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Immutable, arbitrary-precision signed decimal numbers with positive and
 * negative infinity. Every actual computation is delegated to a
 * {@code java.math.BigDecimal} instance.
 * 
 * <p>All methods and constructors for this class throw
 * {@code NullPointerException} when passed a {@code null} object reference for
 * any input parameter.
 *
 * @see java.math.BigDecimal
 */
@SuppressWarnings("serial")
public class OmegaBigDecimal extends Number implements
        Comparable<OmegaBigDecimal> {

    public static final OmegaBigDecimal POSITIVE_INFINITY = new OmegaBigDecimal(
            "+inf");
    public static final OmegaBigDecimal NEGATIVE_INFINITY = new OmegaBigDecimal(
            "-inf");
    public static final OmegaBigDecimal ZERO = new OmegaBigDecimal(
            BigDecimal.ZERO);
    public static final OmegaBigDecimal ONE = new OmegaBigDecimal(
            BigDecimal.ONE);
    public static final OmegaBigDecimal TEN = new OmegaBigDecimal(
            BigDecimal.TEN);

    private BigDecimal value;
    private boolean isPositiveInfinity = false;
    private boolean isNegativeInfinity = false;
    private boolean isLeftNeighborhood = false;
    private boolean isRightNeighborhood = false;
    private volatile int hashCode;

    public OmegaBigDecimal toLeftNeighborhood() {

        if (isLeftNeighborhood || isPositiveInfinity || isNegativeInfinity)
            return this;

        OmegaBigDecimal leftNeighborhood = new OmegaBigDecimal(this);
        leftNeighborhood.isRightNeighborhood = false;
        leftNeighborhood.isLeftNeighborhood = true;
        return leftNeighborhood;
    }

    public OmegaBigDecimal toRightNeighborhood() {

        if (isRightNeighborhood || isPositiveInfinity || isNegativeInfinity)
            return this;

        OmegaBigDecimal rightNeighborhood = new OmegaBigDecimal(this);
        rightNeighborhood.isLeftNeighborhood = false;
        rightNeighborhood.isRightNeighborhood = true;
        return rightNeighborhood;
    }

    public OmegaBigDecimal(String number) {

        if (number.equals("+inf") || number.equals("inf"))
            isPositiveInfinity = true;

        else if (number.equals("-inf"))
            isNegativeInfinity = true;

        else
            value = new BigDecimal(number);

    }

    public OmegaBigDecimal(OmegaBigDecimal number) {

        if (number.isPositiveInfinity)
            isPositiveInfinity = true;

        else if (number.isNegativeInfinity)
            isNegativeInfinity = true;

        else {
            value = number.value;
            isLeftNeighborhood = number.isLeftNeighborhood;
            isRightNeighborhood = number.isRightNeighborhood;
        }

    }

    public OmegaBigDecimal(BigDecimal number) {

        value = number;
    }

    public OmegaBigDecimal(long number) {

        value = BigDecimal.valueOf(number);
    }

    // from Object
    @Override
    public String toString() {
        if (isPositiveInfinity)
            return "+inf";
        else if (isNegativeInfinity)
            return "-inf";
        else
            return value.toString();
    }

    /**
     * Compares this {@code OmegaBigDecimal} with the specified {@code Object}
     * for equality. Unlike {@link #compareTo(OmegaBigDecimal) compareTo}, this
     * method considers two {@code BigDecimal} objects equal only if they are
     * equal in value and scale (thus 2.0 is not equal to 2.00 when compared by
     * this method). Positive (or negative) infinity is considered equal to
     * itself.
     *
     * @param x
     *            {@code Object} to which this {@code OmegaBigDecimal} is to be
     *            compared.
     * @return {@code true} if and only if the specified {@code Object} is an
     *         {@code OmegaBigDecimal} and either its value and scale are equal
     *         to this {@code OmegaBigDecimal}'s or they are both positive
     *         (negative) infinity.
     * @see #compareTo(OmegaBigDecimal)
     * @see #hashCode
     */
    @Override
    public boolean equals(Object x) {
        if (x == this)
            return true;
        if (!(x instanceof OmegaBigDecimal))
            return false;

        OmegaBigDecimal o = (OmegaBigDecimal) x;

        if (isPositiveInfinity)
            return o.isPositiveInfinity;
        else if (isNegativeInfinity)
            return o.isNegativeInfinity;
        else
            return (this.isLeftNeighborhood == o.isLeftNeighborhood)
                    && (this.isRightNeighborhood == o.isRightNeighborhood)
                    && this.value.equals(o.value);
    }

    /**
     * Returns the hash code for this {@code OmegaBigDecimal}. Note that two
     * {@code OmegaBigDecimal} objects that are numerically equal but differ in
     * scale (like 2.0 and 2.00) <i>will</i> have the same hash code (this is
     * different wrt BigDecimal). Positive (negative) infinity objects always
     * have the same hash code.
     *
     * @return hash code for this {@code OmegaBigDecimal}.
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {

        int result = hashCode;

        if (result == 0) {
            result = 17;

            if (isPositiveInfinity)
                result = 31 * result + Integer.MAX_VALUE;
            else if (isNegativeInfinity)
                result = 31 * result + Integer.MIN_VALUE;
            else {
                // workaround for bug #6480539: trailing zeros are not stripped
                // from "0.0"
                if (value.compareTo(BigDecimal.ZERO) != 0)
                    result = 31 * result
                            + value.stripTrailingZeros().hashCode();
                result = 31 * result + (isLeftNeighborhood ? 5 : 7);
                result = 31 * result + (isRightNeighborhood ? 5 : 7);
            }

            hashCode = result;
        }

        return result;
    }

    // from Number
    @Override
    public int intValue() {
        if (isPositiveInfinity)
            return Integer.MAX_VALUE;
        else if (isNegativeInfinity)
            return Integer.MIN_VALUE;
        else
            return value.intValue();
    }

    @Override
    public long longValue() {
        if (isPositiveInfinity)
            return Long.MAX_VALUE;
        else if (isNegativeInfinity)
            return Long.MIN_VALUE;
        else
            return value.longValue();
    }

    @Override
    public float floatValue() {
        if (isPositiveInfinity)
            return Float.POSITIVE_INFINITY;
        else if (isNegativeInfinity)
            return Float.NEGATIVE_INFINITY;
        else
            return value.floatValue();
    }

    @Override
    public double doubleValue() {
        if (isPositiveInfinity)
            return Double.POSITIVE_INFINITY;
        else if (isNegativeInfinity)
            return Double.NEGATIVE_INFINITY;
        else
            return value.doubleValue();
    }

    // from Comparable<OmegaBigDecimal>
    @Override
    public int compareTo(OmegaBigDecimal o) {

        if (o.isNegativeInfinity) {
            if (isNegativeInfinity)
                return 0;
            else
                return 1;
        } else if (o.isPositiveInfinity) {
            if (isPositiveInfinity)
                return 0;
            else
                return -1;
        } else {
            if (isPositiveInfinity)
                return 1;
            else if (isNegativeInfinity)
                return -1;
            else {
                int cmp = this.value.compareTo(o.value);
                if (cmp != 0)
                    return cmp;
                else {
                    if (this.isLeftNeighborhood) {
                        return o.isLeftNeighborhood ? 0 : -1;
                    } else if (this.isRightNeighborhood) {
                        return o.isRightNeighborhood ? 0 : 1;
                    } else {
                        if (o.isLeftNeighborhood)
                            return 1;
                        else if (o.isRightNeighborhood)
                            return -1;
                        else
                            return 0;
                    }
                }
            }
        }
    }

    // to BigDecimal
    public boolean isFinite() {
        return !isPositiveInfinity && !isNegativeInfinity;
    }

    public BigDecimal bigDecimalValue() {
        return value;
    }

    // Operations
    public OmegaBigDecimal abs() {

        if (this.compareTo(ZERO) < 0)
            return this.negate();
        else
            return this;
    }

    public OmegaBigDecimal negate() {

        if (isPositiveInfinity)
            return OmegaBigDecimal.NEGATIVE_INFINITY;

        else if (isNegativeInfinity)
            return OmegaBigDecimal.POSITIVE_INFINITY;

        else {
            OmegaBigDecimal negated = new OmegaBigDecimal(value.negate());
            if (isLeftNeighborhood)
                negated.isRightNeighborhood = true;
            else if (isRightNeighborhood)
                negated.isLeftNeighborhood = true;

            return negated;
        }
    }

    /**
     * Returns an {@code OmegaBigDecimal} whose value is {@code (this +
     * augend)}. In case of a +infinity -infinity indeterminate form, an
     * exception is thrown.
     *
     * @param augend
     *            value to be added to this {@code OmegaBigDecimal}.
     * @return {@code this + augend}
     * @throws IllegalStateException
     */
    public OmegaBigDecimal add(OmegaBigDecimal augend) {

        if (isPositiveInfinity)
            if (augend.isNegativeInfinity)
                // throw new
                // IllegalArgumentException("The sum of +inf and -inf is an indeterminate form");
                // TODO fix this
                return OmegaBigDecimal.ZERO;
            else
                return OmegaBigDecimal.POSITIVE_INFINITY;

        else if (isNegativeInfinity)
            if (augend.isPositiveInfinity)
                // throw new
                // IllegalArgumentException("The sum of -inf and +inf is an indeterminate form");
                // TODO fix this
                return OmegaBigDecimal.ZERO;
            else
                return OmegaBigDecimal.NEGATIVE_INFINITY;

        else if (augend.isPositiveInfinity)
            return OmegaBigDecimal.POSITIVE_INFINITY;
        else if (augend.isNegativeInfinity)
            return OmegaBigDecimal.NEGATIVE_INFINITY;
        else {
            OmegaBigDecimal sum = new OmegaBigDecimal(
                    this.value.add(augend.value));
            sum.isLeftNeighborhood = this.isLeftNeighborhood
                    && !augend.isRightNeighborhood || augend.isLeftNeighborhood
                    && !this.isRightNeighborhood;
            sum.isRightNeighborhood = this.isRightNeighborhood
                    && !augend.isLeftNeighborhood || augend.isRightNeighborhood
                    && !this.isLeftNeighborhood;
            return sum;
        }
    }

    /**
     * Returns an {@code OmegaBigDecimal} whose value is {@code (this -
     * subtrahend)}. In case of a +infinity -infinity indeterminate form, an
     * exception is thrown.
     *
     * @param subtrahend
     *            value to be subtracted from this {@code OmegaBigDecimal}.
     * @return {@code this - subtrahend}
     * @throws IllegalStateException
     */
    public OmegaBigDecimal subtract(OmegaBigDecimal subtrahend) {
        return add(subtrahend.negate());
    }

    /**
     * Returns an {@code OmegaBigDecimal} whose value is <tt>(this &times;
     * multiplicand)</tt>. In case of a <tt>(0 &times; +/- infinity) 
     * indeterminate form, an exception is thrown.
     *
     * @param multiplicand
     *            value to be multiplied by this {@code BigDecimal}.
     * @return {@code this * multiplicand}
     */
    public OmegaBigDecimal multiply(OmegaBigDecimal multiplicand) {
        // TODO handle left and right neighborhoods

        if (isPositiveInfinity) {
            if (multiplicand.isPositiveInfinity)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else if (multiplicand.isNegativeInfinity)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else if (multiplicand.value.compareTo(BigDecimal.ZERO) > 0)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else if (multiplicand.value.compareTo(BigDecimal.ZERO) < 0)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else
                // TODO fix this
                return OmegaBigDecimal.ZERO;
            // throw new
            // IllegalStateException("The product of +inf and 0 is an indeterminate form");

        } else if (isNegativeInfinity) {
            if (multiplicand.isPositiveInfinity)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else if (multiplicand.isNegativeInfinity)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else if (multiplicand.value.compareTo(BigDecimal.ZERO) > 0)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else if (multiplicand.value.compareTo(BigDecimal.ZERO) < 0)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else
                // TODO fix this
                return OmegaBigDecimal.ZERO;
            // throw new
            // IllegalStateException("The product of -inf and 0 is an indeterminate form");

        } else if (multiplicand.isPositiveInfinity) {
            if (value.compareTo(BigDecimal.ZERO) > 0)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else if (value.compareTo(BigDecimal.ZERO) < 0)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else
                // TODO fix this
                return OmegaBigDecimal.ZERO;
            // throw new
            // IllegalStateException("The product of +inf and 0 is an indeterminate form");

        } else if (multiplicand.isNegativeInfinity) {
            if (value.compareTo(BigDecimal.ZERO) > 0)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else if (value.compareTo(BigDecimal.ZERO) < 0)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else
                // TODO fix this
                return OmegaBigDecimal.ZERO;
            // throw new
            // IllegalStateException("The product of -inf and 0 is an indeterminate form");

        } else
            return new OmegaBigDecimal(this.value.multiply(multiplicand.value));
    }

    /**
     * Returns a {@code OmegaBigDecimal} whose value is {@code (this /
     * divisor)}, with rounding according to the context settings.
     *
     * @param divisor
     *            value by which this {@code OmegaBigDecimal} is to be divided.
     * @param mc
     *            the context to use.
     * @return {@code this / divisor}, rounded as necessary.
     * 
     * @throws IllegalArgumentException
     *             if the divisor is BigDecimal.ZERO
     * @throws ArithmeticException
     *             if the result is inexact but the rounding mode is
     *             {@code UNNECESSARY} or {@code mc.precision == 0} and the
     *             quotient has a non-terminating decimal expansion.
     * @see #divide(java.math.BigDecimal)
     */
    public OmegaBigDecimal divide(BigDecimal divisor, MathContext mc) {
        // TODO handle left and right neighborhoods

        if (divisor.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Division by zero");

        if (this.isPositiveInfinity)
            if (divisor.compareTo(BigDecimal.ZERO) > 0)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else
                return OmegaBigDecimal.NEGATIVE_INFINITY;

        else if (this.isNegativeInfinity)
            if (divisor.compareTo(BigDecimal.ZERO) > 0)
                return OmegaBigDecimal.NEGATIVE_INFINITY;
            else
                return OmegaBigDecimal.POSITIVE_INFINITY;

        else
            return new OmegaBigDecimal(this.value.divide(divisor, mc));
    }

    /**
     * Returns the minimum of this {@code OmegaBigDecimal} and {@code val}.
     *
     * @param val
     *            value with which the minimum is to be computed.
     * @return the {@code OmegaBigDecimal} whose value is the lesser of this
     *         {@code OmegaBigDecimal} and {@code val}. If they are equal, as
     *         defined by the {@link #compareTo(OmegaBigDecimal) compareTo}
     *         method, {@code this} is returned.
     * @see #compareTo(it.unifi.oris.sirio.math.OmegaBigDecimal)
     */
    public OmegaBigDecimal min(OmegaBigDecimal val) {
        return compareTo(val) <= 0 ? this : val;
    }

    /**
     * Returns the maximum of this {@code OmegaBigDecimal} and {@code val}.
     *
     * @param val
     *            value with which the maximum is to be computed.
     * @return the {@code OmegaBigDecimal} whose value is the greater of this
     *         {@code OmegaBigDecimal} and {@code val}. If they are equal, as
     *         defined by the {@link #compareTo(OmegaBigDecimal) compareTo}
     *         method, {@code this} is returned.
     * @see #compareTo(it.unifi.oris.sirio.math.OmegaBigDecimal)
     */
    public OmegaBigDecimal max(OmegaBigDecimal val) {
        return compareTo(val) >= 0 ? this : val;
    }

    /**
     * Returns an {@code OmegaBigDecimal} whose value is
     * <tt>(this<sup>n</sup>)</tt>, The power is computed exactly, to unlimited
     * precision.
     * 
     * <p>
     * The parameter {@code n} must be in the range 0 through 999999999,
     * inclusive. {@code ZERO.pow(0)} returns {@link #ONE}.
     *
     * Note that future releases may expand the allowable exponent range of this
     * method.
     *
     * @param n
     *            power to raise this {@code BigDecimal} to.
     * @return <tt>this<sup>n</sup></tt>
     * @throws ArithmeticException
     *             if {@code n} is out of range.
     * @since 1.5
     */
    public OmegaBigDecimal pow(int n) {
        if (n < 0 || n > 999999999)
            throw new ArithmeticException("Invalid operation");

        if (this.isPositiveInfinity) {
            if (n == 0)
                throw new IllegalArgumentException("+inf^0 is indeterminate");
            else
                return OmegaBigDecimal.POSITIVE_INFINITY;
        } else if (this.isNegativeInfinity) {
            if (n == 0)
                throw new IllegalArgumentException("-inf^0 is indeterminate");
            else if (n % 2 == 0)
                return OmegaBigDecimal.POSITIVE_INFINITY;
            else
                return OmegaBigDecimal.NEGATIVE_INFINITY;
        } else {
            return new OmegaBigDecimal(this.value.pow(n));
        }
    }

    public boolean isLeftNeighborhood() {
        return this.isLeftNeighborhood;
    }

    public boolean isRightNeighborhood() {
        return isRightNeighborhood;
    }
}
