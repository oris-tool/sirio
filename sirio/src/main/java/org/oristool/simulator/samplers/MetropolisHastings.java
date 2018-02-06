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

package org.oristool.simulator.samplers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.Map;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.Function;

/**
 * Sampler for general PDFs using the Metropolis-Hastings algorithm.
 */
public final class MetropolisHastings implements Sampler {

    // Boundaries of the density support
    private final OmegaBigDecimal eft;
    private final OmegaBigDecimal lft;

    // Density function to sample
    private final Function pdf;

    // Parameter of the Metropolis-Hastings algorithm
    private BigDecimal sigma = BigDecimal.ONE;

    // Data used for sampling
    private BigDecimal lastSample;

    /**
     * Builds a sampler for the given function.
     *
     * @param f a PDF function
     */
    public MetropolisHastings(Function f) {

        this.pdf = f;
        this.lft = f.getDomain().getCoefficient(Variable.X, Variable.TSTAR);
        this.eft = f.getDomain().getCoefficient(Variable.TSTAR, Variable.X).negate();

        // Initialize the parameters of the Metropolis-Hastings algorithm
        burnIn();
    }

    @Override
    public BigDecimal getSample() {
        // FIXME: The under-sampling step must be adapted to the specific distribution !
        // take a sample every 100 (to have independent samples)
        for (int t = 0; t < 99; t++) {
            computeSample();
        }
        return computeSample();
    }

    /**
     * Computes the next sample according to the Metropolis-Hastings algorithm.
     *
     * @return the next sample
     */
    public BigDecimal computeSample() {

        // Evaluate the value of the density function in the last sampled point
        Map<Variable, OmegaBigDecimal> lastPoint = new LinkedHashMap<Variable, OmegaBigDecimal>();
        lastPoint.put(Variable.X, new OmegaBigDecimal(lastSample));
        BigDecimal oldPointValue = pdf.getDensity().evaluate(lastPoint).bigDecimalValue();

        // Sample a candidate point
        BigDecimal candidatePoint = new BigDecimal(
                boxMuller(lastSample.doubleValue(), sigma.doubleValue()));

        // Evaluate the value of the density function in the candidate point
        BigDecimal candidateValue;
        if (candidatePoint.doubleValue() > lft.doubleValue()
                || candidatePoint.doubleValue() < eft.doubleValue())
            candidateValue = BigDecimal.ZERO;
        else {
            Map<Variable, OmegaBigDecimal> v = new LinkedHashMap<Variable, OmegaBigDecimal>();
            v.put(Variable.X, new OmegaBigDecimal(candidatePoint));
            candidateValue = pdf.getDensity().evaluate(v).bigDecimalValue();
        }

        // Compare the values of the last point and the candidate point
        BigDecimal w = candidateValue.divide(oldPointValue, MathContext.DECIMAL128);
        BigDecimal r = new BigDecimal(Math.random());
        if (r.doubleValue() <= w.doubleValue()) {
            lastSample = candidatePoint;
        }

        return lastSample;
    }

    /**
     * Implements the burn-in phase of the Metropolis-Hastings algorithm, discarding
     * a significantly large number of samples until the chain has passed the
     * transient stage (this procedure actually positions the starting point in a
     * sufficiently probable domain region).
     */
    private void burnIn() {

        // Sample a point within the support of the density function
        lastSample = new BigDecimal(
                (eft.doubleValue() + Math.random() * (lft.doubleValue() - eft.doubleValue())));
        BigDecimal actualPoint = lastSample;
        BigDecimal lastPoint;
        BigDecimal refusedSamples = BigDecimal.ZERO;
        BigDecimal refusalRate = BigDecimal.ZERO;

        // Sample the density function 10.000 times, possibly modifying the standard
        // deviations
        // every 100 samples (depending on the refusal rate computed on the last 100
        // samples).
        for (int i = 0; i < 100; i++) {
            refusedSamples = BigDecimal.ZERO;
            for (int j = 0; j < 100; j++) {
                lastPoint = actualPoint;
                actualPoint = computeSample();

                if (actualPoint.equals(lastPoint))
                    refusedSamples = refusedSamples.add(BigDecimal.ONE);
            }
            refusalRate = refusedSamples.divide(new BigDecimal(100), MathContext.DECIMAL128);

            if (refusalRate.doubleValue() < 0.70) { // 0.55
                sigma = sigma.multiply(BigDecimal.TEN, MathContext.DECIMAL128);
            } else if (refusalRate.doubleValue() > 0.80) { // 0.65
                sigma = sigma.divide(BigDecimal.TEN, MathContext.DECIMAL128);
            }
        }
    }

    private double boxMuller(double mean, double stdDev) {
        double n2 = 0;
        int n2_cached = 0;
        if (n2_cached == 0) {
            double x;
            double y;
            double r;

            do {
                x = 2.0 * Math.random() - 1;
                y = 2.0 * Math.random() - 1;

                r = x * x + y * y;
            } while (r == 0.0 || r > 1.0);

            double d = Math.sqrt(-2.0 * Math.log(r) / r);
            double n1 = x * d;
            n2 = y * d;

            double result = n1 * stdDev + mean;

            n2_cached = 1;
            return result;

        } else {
            n2_cached = 0;
            return n2 * stdDev + mean;
        }
    }
}
