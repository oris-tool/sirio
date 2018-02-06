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
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.oristool.math.Calculus;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.domain.DBMZone.Subzone;
import org.oristool.math.expression.Exmonomial;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.ExponentialTerm;
import org.oristool.math.expression.MonomialTerm;
import org.oristool.math.expression.Variable;

public class Erlang implements Function {

    private DBMZone domain;
    private Expolynomial density;

    private Variable x;
    private BigDecimal lambda;
    private int k;

    /**
     * lambda^k x^(k-1) e^(-lambda x) / (k-1)! function on [0, +infty) domain
     */
    public Erlang(Variable x, int k, BigDecimal lambda) {

        this.x = x;
        this.lambda = lambda;
        this.k = k;

        OmegaBigDecimal eft;
        OmegaBigDecimal lft;
        if (lambda.compareTo(BigDecimal.ZERO) > 0) {
            eft = OmegaBigDecimal.ZERO;
            lft = OmegaBigDecimal.POSITIVE_INFINITY;
        } else
            throw new IllegalArgumentException(
                    "The lambda rate must be greater than zero");

        domain = new DBMZone(x);
        domain.setCoefficient(x, Variable.TSTAR, lft);
        domain.setCoefficient(Variable.TSTAR, x, eft.negate());

        density = new Expolynomial();
        Exmonomial exmon = new Exmonomial(new OmegaBigDecimal(
                BigDecimal.valueOf(Math.pow(lambda.doubleValue(), k))) // lambda^k
                .divide(new BigDecimal(factorial(k - 1)),
                        MathContext.DECIMAL128)); // divided by (k-1)!

        exmon.addAtomicTerm(new MonomialTerm(x, k - 1)); // x^(k-1)
        exmon.addAtomicTerm(new ExponentialTerm(x, lambda)); // e^(-lambda x)
        density.addExmonomial(exmon);
    }

    private static BigInteger factorial(int n) {
        BigInteger r = BigInteger.ONE;
        for (int i = 1; i <= n; ++i)
            r = r.multiply(BigInteger.valueOf(i));
        return r;
    }

    public Erlang(Erlang e) {
        this(e.getVariable(), e.getShape(), e.getLambda());
    }

    public int getShape() {
        return k;
    }

    public BigDecimal getLambda() {
        return lambda;
    }

    public Variable getVariable() {

        return x;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof Erlang))
            return false;

        Erlang other = (Erlang) obj;

        return this.getVariable().equals(other.getVariable())
                && this.getLambda().compareTo(other.getLambda()) == 0;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.getLambda().hashCode();
        result = 31 * result + this.getVariable().hashCode();

        return result;
    }

    @Override
    public String toString() {
        // if(domain.getVariables().size().getConstraints().size()==0)
        // return "<empty function>\n";

        String result = "Domain\n";
        result = result + domain.toString();
        result = result + "Density\n";
        result = result + density.toString() + "\n";
        return result;
    }

    @Override
    public String toMathematicaString() {

        Collection<Variable> variables = domain.getVariables();
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
    public Expolynomial getDensity() {
        return density;
    }

    @Override
    public DBMZone getDomain() {
        return domain;
    }

    //XXX All methods under this line are duplicated
    public void conditionToMin(Variable v, OmegaBigDecimal min) {
        conditionToBound(v, min, OmegaBigDecimal.POSITIVE_INFINITY);
    }

    public void conditionToBound(Variable v, OmegaBigDecimal min,
            OmegaBigDecimal max) {

        domain.imposeBound(Variable.TSTAR, v, min.negate());
        domain.imposeBound(v, Variable.TSTAR, max);

        BigDecimal integral = this.integrateOverDomain().bigDecimalValue();
        //Integral is zero because its duration is end. To avoid a division by zero error, GEN is replaced by an IMM.
        if(integral.compareTo(BigDecimal.ZERO) == 0){
            Variable x = Variable.X;
            OmegaBigDecimal omegaValue = OmegaBigDecimal.ZERO;
            DBMZone domain = new DBMZone();
            domain.addVariables(x);
            domain.setCoefficient(x, Variable.TSTAR, omegaValue);
            domain.setCoefficient(Variable.TSTAR, x, omegaValue.negate());
            this.domain = domain;
            this.density = Expolynomial.newOneInstance();
            return;
        }

        this.getDensity().divide(integral);
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

            Erlang subzoneIntegral = this.integrateOverSubzone(s, false);

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

    public Erlang integrateOverSubzone(Subzone s, boolean shifted) {

        if (this.getDomain().getVariables().size() != s.getDomain()
                .getVariables().size() + 1
                || !this.getDomain().getVariables()
                        .containsAll(s.getDomain().getVariables())
                || !this.getDomain().getVariables()
                        .contains(s.getProjectedVar()))
            throw new IllegalArgumentException(
                    "The subzone refers to a different set of variables");

        Expolynomial primitive = density.integrate(s.getProjectedVar());
        Expolynomial upper, lower;

        if (s.getMaxVar().equals(Variable.TSTAR)
                || s.getMaxVarAdvance().equals(
                        OmegaBigDecimal.POSITIVE_INFINITY))
            upper = primitive.evaluate(s.getProjectedVar(),
                    s.getMaxVarAdvance());
        else
            upper = primitive.evaluate(s.getProjectedVar(), !shifted,
                    s.getMaxVar(), s.getMaxVarAdvance().bigDecimalValue());

        if (s.getMinVar().equals(Variable.TSTAR)
                || s.getMinVarDelay().equals(OmegaBigDecimal.POSITIVE_INFINITY))
            lower = primitive.evaluate(s.getProjectedVar(), s.getMinVarDelay()
                    .negate());
        else
            lower = primitive.evaluate(s.getProjectedVar(), !shifted,
                    s.getMinVar(), s.getMinVarDelay().negate()
                            .bigDecimalValue());

        upper.sub(lower);

        return new Erlang(new DBMZone(s.getDomain()), upper, x, k, lambda);
    }

    /**
     * var -> var + coefficient
     */
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

    /**
     * var -> var + constant for var in variables (in the domain and density)
     */
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


    private Erlang(DBMZone domain, Expolynomial density, Variable x, int k, BigDecimal lambda) {
        this.domain = domain;
        this.density = density;
        this.x = x;
        this.k = k;
        this.lambda = lambda;
    }
}
