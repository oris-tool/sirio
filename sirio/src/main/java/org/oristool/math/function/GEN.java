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

package org.oristool.math.function;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.oristool.math.Calculus;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Exmonomial;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.ExponentialTerm;
import org.oristool.math.expression.Variable;

/**
 * Multidimensional PDF on a support (non-piecewise).
 */
public class GEN implements Function {

    public static GEN getDETInstance(Variable v, BigDecimal value) {

        DBMZone domain = new DBMZone(v);
        domain.setCoefficient(v, Variable.TSTAR, new OmegaBigDecimal(value));
        domain.setCoefficient(Variable.TSTAR, v,
                new OmegaBigDecimal(value).negate());

        return new GEN(domain, Expolynomial.newOneInstance());
    }

    private DBMZone domain;
    private Expolynomial density;

    public GEN(DBMZone domain, Expolynomial density) {

        this.domain = domain;
        this.density = density;
    }

    public GEN(Function f) {
        this(new DBMZone(f.getDomain()), new Expolynomial(f.getDensity()));
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof GEN))
            return false;

        GEN other = (GEN) obj;

        return this.getDomain().equals(other.getDomain())
                && this.getDensity().equals(other.getDensity());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + this.getDomain().hashCode();
        result = 31 * result + this.getDensity().hashCode();

        return result;
    }

    public void normalize() {

        BigDecimal integral = integrateOverDomain().bigDecimalValue();
        if (integral.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalStateException(
                    "Division by zero in GEN normalization");

        density.divide(integral);
    }

    public GEN cartesianProduct(Function f) {
        Expolynomial product = new Expolynomial(density);
        product.multiply(f.getDensity());
        return new GEN(domain.cartesianProduct(f.getDomain()), product);
    }

    public OmegaBigDecimal integrateOverDomain() {

        domain.normalize();
        density.normalize();

        if (domain.getVariables().size() < 2)
            // if T_STAR is the only domain (!) variable, complains
            throw new IllegalArgumentException(
                    "Cannot integrate on a domain without variables");
        else if (!domain.isFullDimensional())
            // if one dimension has a deterministic value, the integral is zero
            return OmegaBigDecimal.ZERO;

        // selects the integration variable x_k != x_*
        Set<Variable> integrationVariables = new LinkedHashSet<Variable>(
                domain.getVariables());
        integrationVariables.remove(Variable.TSTAR);
        Variable integrationVar = integrationVariables.iterator().next(); // k

        OmegaBigDecimal result = OmegaBigDecimal.ZERO;

        for (DBMZone.Subzone s : Calculus.cpsz(this.domain, integrationVar)) {

            GEN subzoneIntegral = Calculus.ios(this, s, false);

            if (subzoneIntegral.getDomain().getVariables().size() == 1)
                // if the integration variable was the last one, left-right must
                // be the integral value
                result = result.add(subzoneIntegral.getDensity().getConstant());
            else
                // recursive invocation O(3**(n^2)?)
                result = result.add(subzoneIntegral.integrateOverDomain());
        }

        if (result.bigDecimalValue() == null)
            return OmegaBigDecimal.ZERO;

        return result;
    }

    public PartitionedGEN project(Variable v) {

        domain.normalize();
        density.normalize();

        // If the variable v is not present, complains
        List<Variable> domainVars = new ArrayList<Variable>(
                domain.getVariables());
        if (!domainVars.contains(v))
            throw new IllegalArgumentException("Variable " + v
                    + " not present in the domain");

        // Integrates wrt v on each subzone
        List<GEN> partition = new ArrayList<GEN>();
        for (DBMZone.Subzone s : Calculus.cpsz(this.domain, v))
            partition.add(Calculus.ios(this, s, false));

        return new PartitionedGEN(partition);
    }

    public PartitionedGEN shiftAndProject(Variable k) {

        domain.normalize();
        density.normalize();

        // If the variable v is not present, complains
        if (!domain.getVariables().contains(k))
            throw new IllegalArgumentException("Variable " + k
                    + " not present in the domain");

        // shifted GEN
        Expolynomial shiftedDensity = new Expolynomial(density);
        for (Variable otherVar : domain.getVariables())
            if (!otherVar.equals(Variable.TSTAR) && !otherVar.equals(k))
                shiftedDensity.shift(otherVar, k);
        GEN shiftedDensityGEN = new GEN(this.domain, shiftedDensity);

        // Integrates the shiftedGEN wrt v on each subzone
        List<GEN> partition = new ArrayList<GEN>();
        for (DBMZone.Subzone s : Calculus.cspsz(this.domain, k)) {
            partition.add(Calculus.ios(shiftedDensityGEN, s, true));
        }

        return new PartitionedGEN(partition);
    }

    public GEN computeNonIntersectingZones(GEN gen, List<GEN> finalFunctions) {
        DBMZone otherDBM = gen.getDomain();
        DBMZone intersectionDBM = new DBMZone(domain);

        for (Variable left : otherDBM.getVariables())
            for (Variable right : otherDBM.getVariables()) {

                if (!left.equals(right)
                        && otherDBM.getCoefficient(left, right).compareTo(
                                intersectionDBM.getCoefficient(left, right)) < 0) {
                    DBMZone differenceDBM = new DBMZone(intersectionDBM);
                    differenceDBM.imposeBound(right, left, otherDBM
                            .getCoefficient(left, right).negate());
                    differenceDBM.normalize();
                    finalFunctions.add(new GEN(differenceDBM, density));

                    intersectionDBM.imposeBound(left, right,
                            otherDBM.getCoefficient(left, right));
                    intersectionDBM.normalize();
                }
            }

        Expolynomial newDensity = new Expolynomial(density);
        newDensity.add(gen.getDensity());
        return new GEN(intersectionDBM, newDensity);
    }

    public List<GEN> getSubZonesInducted(GEN gen) {
        List<GEN> finalFunctions = new ArrayList<GEN>();
        GEN intersection = this
                .computeNonIntersectingZones(gen, finalFunctions);
        gen.computeNonIntersectingZones(this, finalFunctions);
        finalFunctions.add(intersection);

        return finalFunctions;
    }

    public void substitute(Variable oldVar, Variable newVar) {

        domain.substitute(oldVar, newVar);
        density.substitute(oldVar, newVar);
    }

    public void substitute(Variable oldVar, Variable newVar,
            BigDecimal coefficient) {

        domain.substitute(oldVar, newVar, coefficient);
        density = density.evaluate(oldVar, true, newVar, coefficient);
    }

    public void constantShift(BigDecimal constant) {

        domain.constantShift(constant);

        List<Variable> domainVars = new ArrayList<Variable>(
                domain.getVariables());
        domainVars.remove(Variable.TSTAR);

        density.normalize();

        for (int i = 0; i < domainVars.size(); i++)
            density = density.evaluate(domainVars.get(i), true,
                    domainVars.get(i), constant);

    }

    public void constantShift(BigDecimal constant,
            Collection<Variable> variables) {

        domain.constantShift(constant, variables);

        List<Variable> domainVars = new ArrayList<Variable>(
                domain.getVariables());
        domainVars.remove(Variable.TSTAR);

        // Devo shiftare solo le variabili corrispondenti a transizioni
        // progressing
        domainVars.retainAll(variables);

        density.normalize();

        for (int i = 0; i < domainVars.size(); i++)
            density = density.evaluate(domainVars.get(i), true,
                    domainVars.get(i), constant);
    }

    public void substituteAndShift(Variable oldVar, Variable newVar,
            BigDecimal constant) {

        List<Variable> domainVars = new ArrayList<Variable>(
                domain.getVariables());

        domain.substitute(Variable.TSTAR, newVar, constant.negate());
        domain.substitute(oldVar, Variable.TSTAR);

        density = density.evaluate(oldVar, false, newVar, constant);

        domainVars.remove(Variable.TSTAR);
        domainVars.remove(oldVar);

        for (int i = 0; i < domainVars.size(); i++)
            density = density.evaluate(domainVars.get(i), true,
                    domainVars.get(i), false, newVar, constant);

    }

    public BigDecimal conditionToMin(Variable v, OmegaBigDecimal min) {
        return conditionToBound(v, min, OmegaBigDecimal.POSITIVE_INFINITY);
    }

    public BigDecimal conditionToMax(Variable v, OmegaBigDecimal max) {
        return conditionToBound(v, OmegaBigDecimal.NEGATIVE_INFINITY, max);
    }

    public BigDecimal conditionToBound(Variable v, OmegaBigDecimal min,
            OmegaBigDecimal max) {

        domain.imposeBound(Variable.TSTAR, v, min.negate());
        domain.imposeBound(v, Variable.TSTAR, max);

        BigDecimal integral = this.integrateOverDomain().bigDecimalValue();
        // Integral is zero because its duration is end. To avoid a division by zero
        // error, GEN is replaced by an IMM.
        if (integral.compareTo(BigDecimal.ZERO) == 0){
            Variable x = Variable.X;
            OmegaBigDecimal omegaValue = OmegaBigDecimal.ZERO;
            DBMZone domain = new DBMZone();
            domain.addVariables(x);
            domain.setCoefficient(x, Variable.TSTAR, omegaValue);
            domain.setCoefficient(Variable.TSTAR, x, omegaValue.negate());
            this.domain = domain;
            this.density = Expolynomial.newOneInstance();
            return BigDecimal.ZERO;
        }

        this.getDensity().divide(integral);
        return integral;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Domain\n");
        b.append(domain);
        b.append("Density\n");
        b.append(density);
        b.append("\n");

        return b.toString();
    }

    @Override
    public String toMathematicaString() {

        ArrayList<Variable> variables = new ArrayList<>(domain.getVariables());
        variables.remove(Variable.TSTAR);

        String result = "f[";
        String prefix = "";
        for (Variable v : variables) {
            result += prefix + v;
            prefix = "_,";
        }
        result += "_";
        result = result + "] := ( " + density.toString() + " ) * "
                + domain.toUnitStepsString();

        return result;
    }

    @Override
    public DBMZone getDomain() {
        return domain;
    }

    @Override
    public Expolynomial getDensity() {
        return density;
    }

    public static GEN newTruncatedExp(Variable v, BigDecimal rate,
            OmegaBigDecimal eft, OmegaBigDecimal lft) {

        EXP e = new EXP(v, rate);
        GEN g = new GEN(e.getDomain(), e.getDensity());
        g.getDomain().imposeBound(Variable.TSTAR, v, eft.negate());
        g.getDomain().imposeBound(v, Variable.TSTAR, lft);

        return g;
    }

    public static GEN newDeterministic(BigDecimal value) {
        Variable x = Variable.X;
        OmegaBigDecimal omegaValue = new OmegaBigDecimal(value);

        DBMZone domain = new DBMZone();
        domain.addVariables(x);
        domain.setCoefficient(x, Variable.TSTAR, omegaValue);
        domain.setCoefficient(Variable.TSTAR, x, omegaValue.negate());

        return new GEN(domain, Expolynomial.newOneInstance());
    }

    public static GEN newUniform(OmegaBigDecimal eft, OmegaBigDecimal lft) {

        DBMZone domain = new DBMZone();
        domain.addVariables(Variable.X);
        domain.setCoefficient(Variable.X, Variable.TSTAR, lft);
        domain.setCoefficient(Variable.TSTAR, Variable.X, eft.negate());

        Expolynomial constant = Expolynomial
                .newConstantInstance(OmegaBigDecimal.ONE.divide(
                        lft.subtract(eft).bigDecimalValue(),
                        Expolynomial.mathContext));

        return new GEN(domain, constant);
    }

    public static GEN newExpolynomial(String density, OmegaBigDecimal eft,
            OmegaBigDecimal lft) {

        DBMZone domain = new DBMZone();
        domain.addVariables(Variable.X);
        domain.setCoefficient(Variable.X, Variable.TSTAR, lft);
        domain.setCoefficient(Variable.TSTAR, Variable.X, eft.negate());

        Expolynomial e = Expolynomial.fromString(density);

        return new GEN(domain, e);
    }

    public static GEN newHyperExp(List<BigDecimal> probs, List<BigDecimal> rates) {

        if (probs.size() != rates.size())
            throw new IllegalArgumentException("The number of initial probabilities should equal the number of rates");

        // prob1 * rate1 * e^{-rate1 * x} + prob2 * rate2 * e^{-rate2 * x}
        Expolynomial expol =  new Expolynomial();
        for(int i = 0; i < probs.size(); i++) {
            BigDecimal prob = probs.get(i);
            BigDecimal rate = rates.get(i);

            if (prob.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Initial probabilities must be positive");

            if (prob.compareTo(BigDecimal.ONE) >= 0)
                throw new IllegalArgumentException("Initial probabilities must be lower or equal to one");

            if (rate.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Rates must be positive");

            Exmonomial monomial = new Exmonomial(prob.multiply(rate));
            monomial.addAtomicTerm(new ExponentialTerm(Variable.X, rate));
            expol.addExmonomial(monomial);
        }

        DBMZone domain = new DBMZone(Variable.X);
        domain.setCoefficient(Variable.X, Variable.TSTAR, OmegaBigDecimal.POSITIVE_INFINITY);
        domain.setCoefficient(Variable.TSTAR, Variable.X, OmegaBigDecimal.ZERO);

        return new GEN(domain, expol);
    }

    public static GEN newHypoExp(BigDecimal rate1, BigDecimal rate2) {

        if (rate1.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Rate1 must be positive");

        if (rate2.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Rate2 must be positive");

        // (rate1 * rate2) / (rate1 - rate2) * [e^{-rate2 * x} - e^{-rate1 * x}
        BigDecimal c = rate1.multiply(rate2).divide(rate1.subtract(rate2), MathContext.DECIMAL128);

        Exmonomial monomial1 = new Exmonomial(c);
        monomial1.addAtomicTerm(new ExponentialTerm(Variable.X, rate2));

        Exmonomial monomial2 = new Exmonomial(c.negate());
        monomial2.addAtomicTerm(new ExponentialTerm(Variable.X, rate1));

        Expolynomial expol =  new Expolynomial();
        expol.addExmonomial(monomial1);
        expol.addExmonomial(monomial2);

        DBMZone domain = new DBMZone(Variable.X);
        domain.setCoefficient(Variable.X, Variable.TSTAR, OmegaBigDecimal.POSITIVE_INFINITY);
        domain.setCoefficient(Variable.TSTAR, Variable.X, OmegaBigDecimal.ZERO);

        return new GEN(domain, expol);
    }

    public static GEN newShiftedExp(BigDecimal shift, BigDecimal rate) {

        if (shift.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Shift must be non-negative");

        if (rate.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Rate must be positive");

        // rate * exp(rate*shift) * exp(-rate*X)
        Exmonomial monomial = new Exmonomial(rate);
        monomial.multiply(new ExponentialTerm(Variable.X, rate.negate()).evaluate(new OmegaBigDecimal(shift)));
        monomial.addAtomicTerm(new ExponentialTerm(Variable.X, rate));

        Expolynomial expol = new Expolynomial();
        expol.addExmonomial(monomial);

        DBMZone domain = new DBMZone(Variable.X);
        domain.setCoefficient(Variable.X, Variable.TSTAR, OmegaBigDecimal.POSITIVE_INFINITY);
        domain.setCoefficient(Variable.TSTAR, Variable.X, new OmegaBigDecimal(shift).negate());

        return new GEN(domain, expol);
    }
}
