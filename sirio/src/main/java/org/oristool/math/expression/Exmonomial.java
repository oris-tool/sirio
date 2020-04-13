/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oristool.math.OmegaBigDecimal;

/**
 * A constant term multiplied by a set of {@link AtomicTerm}.
 */
public final class Exmonomial {

    private OmegaBigDecimal constantTerm;
    private List<AtomicTerm> atomicTerms = new ArrayList<>();

    /**
     * Builds a constant instance.
     *
     * @param constantTerm the constant value
     */
    public Exmonomial(OmegaBigDecimal constantTerm) {
        this.constantTerm = constantTerm;
    }

    public Exmonomial(BigDecimal constantTerm) {
        this.constantTerm = new OmegaBigDecimal(constantTerm);
    }

    /**
     * Builds a copy of the input instance.
     *
     * @param exmon input instance
     */
    public Exmonomial(Exmonomial exmon) {

        constantTerm = exmon.constantTerm;
        for (AtomicTerm a : exmon.atomicTerms)
            atomicTerms.add(a.duplicate());
    }

    /**
     * Gets the constant multiplier of the term.
     *
     * @return constant multiplier
     */
    public OmegaBigDecimal getConstantTerm() {
        return this.constantTerm;
    }

    /**
     * Sets the constant multiplier of the term.
     *
     * @param constantTerm constant multiplier to set
     */
    public void setConstantTerm(OmegaBigDecimal constantTerm) {
        this.constantTerm = constantTerm;
    }

    /**
     * Gets the variable atomic terms of the exmonomial.
     *
     * @return the atomic terms
     */
    public List<AtomicTerm> getAtomicTerms() {
        // return Collections.unmodifiableList(atomicTerms);
        return atomicTerms;
    }

    /**
     * Adds a new atomic term to the product.
     *
     * @param term the atomic term to be added
     */
    public void addAtomicTerm(AtomicTerm term) {
        if (term != null)
            atomicTerms.add(term);
    }

    /**
     * Collects the variable names of the exmonomial.
     *
     * @return variable names of all terms
     */
    public Collection<Variable> getVariables() {

        Set<Variable> variables = new LinkedHashSet<>();
        for (AtomicTerm a : atomicTerms)
            variables.add(a.getVariable());
        return variables;
    }

    /**
     * Normalizes the exmonomial to avoid multiple powers (or exponentials) for the
     * same variable.
     */
    public void normalize() {

        // creates a constant exmonomial
        Exmonomial mon = new Exmonomial(this.constantTerm);

        boolean[] pos = new boolean[atomicTerms.size()];

        for (int i = 0; i < atomicTerms.size(); i++) {
            if (pos[i] == false) {
                AtomicTerm term1 = atomicTerms.get(i).duplicate();
                for (int j = i + 1; j < atomicTerms.size(); j++) {
                    AtomicTerm term2 = atomicTerms.get(j);
                    if (term1.getVariable().equals(term2.getVariable())
                            && term1.getClass().equals(term2.getClass())) {

                        term1.multiply(term2);
                        pos[j] = true;
                    }
                }

                // lambda can become zero simplyfying exponential terms
                if (!term1.isOne())
                    mon.addAtomicTerm(term1);
            }
        }

        this.atomicTerms = mon.atomicTerms;
        this.constantTerm = mon.constantTerm;
    }

    /**
     * Multiplies this term by an input exmonomial.
     *
     * @param exmon input exmonomial
     */
    public void multiply(Exmonomial exmon) {

        this.constantTerm = this.constantTerm.multiply(exmon.getConstantTerm());

        for (AtomicTerm a : exmon.getAtomicTerms())
            this.atomicTerms.add(a.duplicate());
        this.normalize();
    }

    /**
     * Multiplies this exmonomial by a constant.
     *
     * @param c constant
     */
    public void multiply(OmegaBigDecimal c) {

        this.constantTerm = this.constantTerm.multiply(c);
    }

    /**
     * Checks whether the input exmonomial has the same set of terms.
     *
     * <p>Both exmonomials must be normalized first.
     *
     * @param exmon input exmonomial
     * @return true if both exmonomials have the same set of terms.
     */
    public boolean isSameForm(Exmonomial exmon) {
        if (atomicTerms.size() != exmon.getAtomicTerms().size())
            return false;

        for (int i = 0; i < atomicTerms.size(); i++) {
            boolean found = false;
            for (int j = 0; j < exmon.getAtomicTerms().size() && !found; j++)
                if (atomicTerms.get(i).equals(exmon.getAtomicTerms().get(j)))
                    found = true;
            if (!found)
                return false;
        }
        return true;
    }

    /**
     * Divides by a constant and normalizes.
     *
     * @param k constant
     */
    public void divide(BigDecimal k) {

        this.normalize();
        this.constantTerm = this.constantTerm.divide(k,
                Expolynomial.mathContext);
    }

    /**
     * Replaces <code>oldVar</code> with <code>newVar</code>.
     *
     * @param oldVar variable to be replaced
     * @param newVar new variable
     */
    public void substitute(Variable oldVar, Variable newVar) {

        for (AtomicTerm a : this.atomicTerms)
            a.substitute(oldVar, newVar);
        this.normalize();
    }

    /**
     * Computes the primitive function of this exmonomial.
     *
     * @param var integration variable
     * @return primitive function
     */
    public Expolynomial integrate(Variable var) {

        Exmonomial finalexmon = new Exmonomial(constantTerm);
        this.normalize();

        int indexMonTerm = -1;
        int indexExpTerm = -1;
        for (int i = 0; i < this.getAtomicTerms().size(); i++) {
            AtomicTerm term = this.getAtomicTerms().get(i);
            if (term.getVariable().equals(var)) {
                if (term instanceof MonomialTerm)
                    indexMonTerm = i;
                else
                    indexExpTerm = i;
            } else
                finalexmon.addAtomicTerm(term);
        }

        Expolynomial integralExpol = null;

        if (indexMonTerm != -1 && indexExpTerm != -1) {
            MonomialTerm monterm = (MonomialTerm) this.getAtomicTerms().get(
                    indexMonTerm);
            ExponentialTerm expterm = (ExponentialTerm) this.getAtomicTerms()
                    .get(indexExpTerm);
            integralExpol = new Expolynomial();

            for (int k = 0; k <= monterm.getAlpha(); k++) {
                BigDecimal alpha_fatt = new BigDecimal(
                        MathUtil.calculateFactorial(monterm.getAlpha()));
                BigDecimal k_fatt = new BigDecimal(MathUtil.calculateFactorial(k));
                BigDecimal lambda_power = expterm.getLambda().pow(
                        1 + monterm.getAlpha() - k);

                Exmonomial integ_mon = new Exmonomial(new OmegaBigDecimal(
                        alpha_fatt.divide(k_fatt.multiply(lambda_power),
                                Expolynomial.mathContext)));
                integ_mon.addAtomicTerm(new MonomialTerm(var, k));
                integralExpol.addExmonomial(integ_mon);
            }
            finalexmon.addAtomicTerm(expterm);
            finalexmon.setConstantTerm(finalexmon.getConstantTerm().negate());
        } else if (indexMonTerm != -1) {
            MonomialTerm term = (MonomialTerm) this.getAtomicTerms().get(
                    indexMonTerm);
            finalexmon
                    .addAtomicTerm(new MonomialTerm(var, term.getAlpha() + 1));
            finalexmon.setConstantTerm(finalexmon.getConstantTerm().divide(
                    BigDecimal.valueOf(term.getAlpha() + 1),
                    Expolynomial.mathContext));
        } else if (indexExpTerm != -1) {
            ExponentialTerm term = (ExponentialTerm) this.getAtomicTerms().get(
                    indexExpTerm);
            finalexmon.addAtomicTerm(term);
            finalexmon.setConstantTerm(finalexmon.getConstantTerm()
                    .divide(term.getLambda(), Expolynomial.mathContext)
                    .negate());
        } else
            finalexmon.addAtomicTerm(new MonomialTerm(var, 1));

        Expolynomial others = new Expolynomial();
        others.addExmonomial(finalexmon);

        if (integralExpol != null) {
            integralExpol.multiply(others);
            return integralExpol;
        } else
            return others;
    }

    /**
     * Integrates this exmonomial over an interval.
     *
     * @param var integration variable
     * @param lower lower bound
     * @param upper upper bound
     * @return definite integral value
     */
    public Expolynomial integrate(Variable var, OmegaBigDecimal lower,
            OmegaBigDecimal upper) {

        Expolynomial indefiniteInt = integrate(var);

        Expolynomial exp1 = indefiniteInt.evaluate(var, upper);
        Expolynomial exp2 = indefiniteInt.evaluate(var, lower);

        exp1.sub(exp2);
        return exp1;
    }

    /**
     * Replaces <code>base</code> with <code>base</code> + <code>offset</code>.
     *
     * @param base variable to be replaced
     * @param offset offset to be applied
     * @return resulting expolynomial
     */
    public Expolynomial shift(Variable base, Variable offset) {

        Exmonomial finalexmon = new Exmonomial(constantTerm);
        Expolynomial newton = null;
        this.normalize();
        Exmonomial exmon = new Exmonomial(this);

        for (int i = 0; i < exmon.getAtomicTerms().size(); i++) {
            AtomicTerm term = exmon.getAtomicTerms().get(i);
            if (term.getVariable().equals(base)) {
                if (term instanceof MonomialTerm) {
                    newton = new Expolynomial();
                    for (int k = 0; k <= ((MonomialTerm) term).getAlpha(); k++) {
                        Exmonomial newton_mon = new Exmonomial(
                                new OmegaBigDecimal(new BigDecimal(
                                        (MathUtil.calculateBinomialCoefficient(
                                                ((MonomialTerm) term)
                                                        .getAlpha(), k)))));
                        newton_mon.addAtomicTerm(new MonomialTerm(base,
                                ((MonomialTerm) term).getAlpha() - k));
                        newton_mon.addAtomicTerm(new MonomialTerm(offset, k));

                        newton.addExmonomial(newton_mon);
                    }
                } else {
                    finalexmon.addAtomicTerm(term);
                    finalexmon.addAtomicTerm(new ExponentialTerm(offset,
                            ((ExponentialTerm) term).getLambda()));
                }
            } else
                finalexmon.addAtomicTerm(term);
        }

        Expolynomial others = new Expolynomial();
        finalexmon.normalize();
        others.addExmonomial(finalexmon);
        if (newton != null) {
            newton.multiply(others);
            return newton;
        } else
            return others;
    }

    /**
     * Evaluates the exmonomial.
     *
     * @param m map with values for all the variables
     * @return exmonomial value
     */
    public OmegaBigDecimal evaluate(Map<Variable, OmegaBigDecimal> m) {

        OmegaBigDecimal result = constantTerm;
        for (int i = 0; i < atomicTerms.size(); i++) {
            Variable v = atomicTerms.get(i).getVariable();
            OmegaBigDecimal value = m.get(v);

            if (value == null)
                throw new IllegalArgumentException(
                        "A value must be specified for variable " + v);

            result = result.multiply(atomicTerms.get(i).evaluate(value));
        }
        return result;
    }

    /**
     * Replaces a variable with its value.
     *
     * @param var variable to be replaced
     * @param value value of the variable
     * @return exmonomial after the substitution
     */
    public Exmonomial evaluate(Variable var, OmegaBigDecimal value) {
        Exmonomial exmon = new Exmonomial(constantTerm);
        for (int i = 0; i < atomicTerms.size(); i++) {
            AtomicTerm term = atomicTerms.get(i);
            if (term.getVariable().equals(var))
                exmon.setConstantTerm(exmon.getConstantTerm().multiply(
                        term.evaluate(value)));
            else
                exmon.getAtomicTerms().add(term);
        }
        exmon.normalize();
        return exmon;
    }

    /**
     * Replaces <code>base</code> with +/- <code>offset</code> +
     * <code>constant</code>.
     *
     * @param base variable to be replaced
     * @param sign true if sign of the offset is positive
     * @param offset offset to be added
     * @param constant constant to be added
     * @return resulting {@link Expolynomial}
     */
    public Expolynomial evaluate(Variable base, Boolean sign, Variable offset,
            BigDecimal constant) {
        Exmonomial finalexmon = new Exmonomial(constantTerm);
        Expolynomial newton = null;
        this.normalize();
        Exmonomial exmon = new Exmonomial(this);
        for (int i = 0; i < exmon.getAtomicTerms().size(); i++) {
            AtomicTerm term = exmon.getAtomicTerms().get(i);
            if (term.getVariable().equals(base)) {
                if (term instanceof MonomialTerm) {
                    newton = new Expolynomial();
                    for (int k = 0; k <= ((MonomialTerm) term).getAlpha(); k++) {
                        Variable xapp = new Variable("xapp");
                        MonomialTerm power = new MonomialTerm(xapp, k);
                        Exmonomial newton_mon = new Exmonomial(
                                new OmegaBigDecimal(new BigDecimal((MathUtil
                                        .calculateBinomialCoefficient(
                                                ((MonomialTerm) term)
                                                        .getAlpha(), k)))
                                        .multiply(power.evaluate(
                                                new OmegaBigDecimal(constant))
                                                .bigDecimalValue())));

                        newton_mon.addAtomicTerm(new MonomialTerm(offset,
                                ((MonomialTerm) term).getAlpha() - k));
                        if ((((MonomialTerm) term).getAlpha() - k) % 2 != 0
                                && !sign)
                            newton_mon.setConstantTerm(newton_mon
                                    .getConstantTerm().negate());

                        newton.addExmonomial(newton_mon);
                    }
                } else {
                    Variable xapp = new Variable("xapp");
                    ExponentialTerm exp = new ExponentialTerm(xapp,
                            ((ExponentialTerm) term).getLambda());
                    finalexmon.setConstantTerm(
                            finalexmon.getConstantTerm()
                                .multiply(
                                        exp.evaluate(
                                            new OmegaBigDecimal(constant))));

                    if (sign)
                        finalexmon.addAtomicTerm(new ExponentialTerm(offset,
                                ((ExponentialTerm) term).getLambda()));
                    else
                        finalexmon.addAtomicTerm(new ExponentialTerm(offset,
                                ((ExponentialTerm) term).getLambda().negate()));
                }
            } else
                finalexmon.addAtomicTerm(term);
        }

        Expolynomial others = new Expolynomial();
        finalexmon.normalize();
        others.addExmonomial(finalexmon);
        if (newton != null) {
            newton.multiply(others);
            return newton;
        } else
            return others;
    }

    /**
     * Replaces <code>base</code> with +/- <code>offset1</code> +/-
     * <code>offset2</code> + <code>constant</code>.
     *
     * @param base variable to be replaced
     * @param sign1 true if sign of the first offset is positive
     * @param offset1 first offset to be added
     * @param sign2 true if sign of the second offset is positive
     * @param offset2 second offset to be added
     * @param constant constant to be added
     * @return resulting {@link Expolynomial}
     */
    public Expolynomial evaluate(Variable base, Boolean sign1,
            Variable offset1, Boolean sign2, Variable offset2,
            BigDecimal constant) {
        Exmonomial finalexmon = new Exmonomial(constantTerm);
        Expolynomial newton = null;
        this.normalize();
        Exmonomial exmon = new Exmonomial(this);
        for (int i = 0; i < exmon.getAtomicTerms().size(); i++) {
            AtomicTerm term = exmon.getAtomicTerms().get(i);
            if (term.getVariable().equals(base)) {
                if (term instanceof MonomialTerm) {

                    Exmonomial exmon1 = new Exmonomial(new OmegaBigDecimal(1));
                    exmon1.addAtomicTerm(new MonomialTerm(offset1, 1));
                    if (!sign1)
                        exmon1.setConstantTerm(new OmegaBigDecimal(-1));

                    Exmonomial exmon2 = new Exmonomial(new OmegaBigDecimal(1));
                    exmon2.addAtomicTerm(new MonomialTerm(offset2, 1));
                    if (!sign2)
                        exmon2.setConstantTerm(new OmegaBigDecimal(-1));

                    Exmonomial exmon3 = new Exmonomial(new OmegaBigDecimal(
                            constant));

                    Expolynomial trinomial = new Expolynomial();
                    trinomial.addExmonomial(exmon1);
                    trinomial.addExmonomial(exmon2);
                    trinomial.addExmonomial(exmon3);

                    newton = new Expolynomial(trinomial);

                    for (int k = 1; k < ((MonomialTerm) term).getAlpha(); k++)
                        newton.multiply(trinomial);

                } else {
                    Variable xapp = new Variable("xapp");
                    ExponentialTerm exp = new ExponentialTerm(xapp,
                            ((ExponentialTerm) term).getLambda());

                    finalexmon.setConstantTerm(finalexmon.getConstantTerm()
                            .multiply(
                                    new OmegaBigDecimal(exp.evaluate(
                                            new OmegaBigDecimal(constant))
                                            .bigDecimalValue())));

                    if (sign1)
                        finalexmon.addAtomicTerm(new ExponentialTerm(offset1,
                                ((ExponentialTerm) term).getLambda()));
                    else
                        finalexmon.addAtomicTerm(new ExponentialTerm(offset1,
                                ((ExponentialTerm) term).getLambda().negate()));

                    if (sign2)
                        finalexmon.addAtomicTerm(new ExponentialTerm(offset2,
                                ((ExponentialTerm) term).getLambda()));
                    else
                        finalexmon.addAtomicTerm(new ExponentialTerm(offset2,
                                ((ExponentialTerm) term).getLambda().negate()));
                }
            } else
                finalexmon.addAtomicTerm(term);
        }

        Expolynomial others = new Expolynomial();
        finalexmon.normalize();
        others.addExmonomial(finalexmon);
        if (newton != null) {
            newton.multiply(others);
            return newton;
        } else
            return others;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();

        b.append(constantTerm);
        String join = " * ";
        for (int i = 0; i < atomicTerms.size(); i++) {
            b.append(join);
            b.append(atomicTerms.get(i).toString());
        }

        return b.toString();
    }
}
