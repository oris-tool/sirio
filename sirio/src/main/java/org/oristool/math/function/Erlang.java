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

package org.oristool.math.function;

import java.math.BigDecimal;
import java.math.MathContext;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Exmonomial;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.ExponentialTerm;
import org.oristool.math.expression.MathUtil;
import org.oristool.math.expression.MonomialTerm;
import org.oristool.math.expression.Variable;

/**
 * The Erlang PDF.
 */
public class Erlang extends GEN {

    private final Variable var;
    private final BigDecimal lambda;
    private final int shape;

    /**
     * Builds the function {@code lambda^k x^(k-1) e^(-lambda x) / (k-1)!} over {@code [0, +infty)}.
     *
     * @param x variable
     * @param k shape
     * @param lambda rate
     */
    public Erlang(Variable x, int k, BigDecimal lambda) {

        super(erlangDomain(x), erlangDensity(x, k, lambda));

        this.var = x;
        this.lambda = lambda;
        this.shape = k;
    }

    private static DBMZone erlangDomain(Variable x) {
        DBMZone domain = new DBMZone(x);
        domain.setCoefficient(x, Variable.TSTAR, OmegaBigDecimal.POSITIVE_INFINITY);
        domain.setCoefficient(Variable.TSTAR, x, OmegaBigDecimal.ZERO);
        return domain;
    }

    private static Expolynomial erlangDensity(Variable x, int k, BigDecimal lambda) {
        if (lambda.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("The lambda rate must be greater than zero");

        Expolynomial density = new Expolynomial();
        Exmonomial exmon = new Exmonomial(new OmegaBigDecimal(
                BigDecimal.valueOf(Math.pow(lambda.doubleValue(), k))) // lambda^k
                .divide(new BigDecimal(MathUtil.calculateFactorial(k - 1)),
                        MathContext.DECIMAL128));            // divided by (k-1)!

        exmon.addAtomicTerm(new MonomialTerm(x, k - 1));     // x^(k-1)
        exmon.addAtomicTerm(new ExponentialTerm(x, lambda)); // e^(-lambda x)
        density.addExmonomial(exmon);
        return density;
    }

    /**
     * Builds a copy of the input Erlang.
     * @param erlang input Erlang
     */
    public Erlang(Erlang erlang) {
        this(erlang.getVariable(), erlang.getShape(), erlang.getLambda());
    }

    /**
     * Returns the shape parameter.
     *
     * @return shape parameter
     */
    public int getShape() {
        return shape;
    }

    /**
     * Returns the rate.
     *
     * @return rate parameter
     */
    public BigDecimal getLambda() {
        return lambda;
    }

    /**
     * Returns the variable name.
     *
     * @return variable name
     */
    public Variable getVariable() {
        return var;
    }
}
