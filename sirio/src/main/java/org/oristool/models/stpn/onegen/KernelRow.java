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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.models.gspn.CTMCTransientSolution;

class KernelRow {
    class KernelRowEvaluator {
        private PrecalculatedTickEvaluations evaluations;

        private KernelRowEvaluator(PrecalculatedTickEvaluations evaluations) {
            super();
            this.evaluations = evaluations;
        }

        public double[] evaluateLocalKernel(State destinationState) {
            if (localKernelEntries.containsKey(destinationState)) {
                return localKernelEntries.get(destinationState).compute(evaluations);
            }
            return new double[evaluations.getTicks().getNumKernelTicks()];
        }

        public double[] evaluateGlobalKernel(State destinationState) {
            if (globalKernelEntries.containsKey(destinationState)) {
                return globalKernelEntries.get(destinationState).compute(evaluations);
            }
            return new double[evaluations.getTicks().getNumKernelTicks()];
        }
    }

    private BoundedExpolynomial pdf;
    private BoundedExpolynomial cdf;
    private CTMCTransientSolution transientSolution;
    private Map<State, KernelFormula> localKernelEntries;
    private Map<State, KernelFormula> globalKernelEntries;

    public KernelRow() {
        super();
        setNullPdf();
    }

    private void setNullPdf() {
        Variable pdfVariable = Variable.X;
        this.pdf = new BoundedExpolynomial(Expolynomial.newConstantInstance(OmegaBigDecimal.ZERO),
                pdfVariable, OmegaBigDecimal.ZERO, OmegaBigDecimal.ZERO, OmegaBigDecimal.ZERO,
                OmegaBigDecimal.ZERO, false, false);
        this.cdf = new BoundedExpolynomial(Expolynomial.newConstantInstance(OmegaBigDecimal.ZERO),
                pdfVariable, OmegaBigDecimal.ZERO, OmegaBigDecimal.ZERO, OmegaBigDecimal.ZERO,
                OmegaBigDecimal.ZERO, true, true);
    }

    public void setPdf(PartitionedFunction pdf) {
        if (pdf == null) {
            setNullPdf();
        } else if (pdf.getDensities().size() != 1) {
            throw new UnsupportedOperationException(
                    "Analysis under enabling restriction with piecewise GEN not yet supported");
        } else {
            Expolynomial law = pdf.getDensities().get(0);
            Collection<Variable> vars = law.getVariables();
            Variable pdfVariable;
            if (vars.size() > 1) {
                throw new IllegalStateException("Multivariate PDF");
            } else if (vars.size() < 1) {
                pdfVariable = Variable.X;
            } else {
                pdfVariable = vars.iterator().next();
            }

            OmegaBigDecimal domainUpperBound = pdf.getDomains().get(0).getCoefficient(pdfVariable,
                    Variable.TSTAR);
            OmegaBigDecimal domainLowerBound = pdf.getDomains().get(0)
                    .getCoefficient(Variable.TSTAR, pdfVariable).negate();
            this.pdf = new BoundedExpolynomial(law, pdfVariable, domainLowerBound,
                    OmegaBigDecimal.ZERO, domainUpperBound, OmegaBigDecimal.ZERO, false, false);
            this.cdf = new BoundedExpolynomial(law.integrate(pdfVariable), pdfVariable,
                    domainLowerBound, OmegaBigDecimal.ZERO, domainUpperBound, OmegaBigDecimal.ONE,
                    true, true);
        }
    }

    public void setLocalKernelEntries(Map<State, KernelFormula> localKernelEntries) {
        this.localKernelEntries = localKernelEntries;
    }

    public void setGlobalKernelEntries(Map<State, KernelFormula> globalKernelEntries) {
        this.globalKernelEntries = globalKernelEntries;
    }

    public void setTransientSolution(CTMCTransientSolution transientSolution) {
        this.transientSolution = transientSolution;
    }

    public KernelRowEvaluator getEvaluator(Ticks ticks, BigDecimal step, BigDecimal error) {

        List<OmegaBigDecimal> pdfEvals = new ArrayList<>();
        List<OmegaBigDecimal> cdfEvals = new ArrayList<>();
        ticks.getIntegralTicks().forEach(tick -> {
            pdfEvals.add(pdf.evaluate(new OmegaBigDecimal(tick)));
            cdfEvals.add(cdf.evaluate(new OmegaBigDecimal(tick)));
        });

        if (pdf.getLowerBound().equals(pdf.getUpperBound())
                && pdf.getUpperBound().doubleValue() != 0) {
            // Is a DET
            // DET is a special case since its PDF is a Dirac delta, for this reason its
            // duration is infinitely small
            int integralTick = OmegaBigDecimal.ONE
                    .divide(ticks.getIntegralTickStep(), MathContext.DECIMAL128).intValue();
            for (int i = 0; i < pdfEvals.size(); i++) {
                if (pdfEvals.get(i).doubleValue() != 0)
                    // This condition is true only for one sample, since it is a DET
                    pdfEvals.set(i, pdfEvals.get(i).multiply(new OmegaBigDecimal(integralTick)));
            }
        }

        Map<State, double[]> transientsPerState = transientSolution
                .computeTransients(ticks.getIntegralTicks(), step, error);

        KernelRowEvaluator evaluator = new KernelRowEvaluator(
                new PrecalculatedTickEvaluations(ticks, transientsPerState, pdfEvals, cdfEvals));
        return evaluator;
    }
}
