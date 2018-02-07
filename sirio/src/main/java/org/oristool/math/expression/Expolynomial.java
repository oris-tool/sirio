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
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.oristool.math.OmegaBigDecimal;

/**
 * A sum of {@link Exmonomial} terms.
 */
public class Expolynomial {

    /**
     * Builds a new instance equal to the constant 1.
     *
     * @return a constant expolynomial equal to 1
     */
    public static Expolynomial newOneInstance() {
        Expolynomial one = new Expolynomial();
        one.addExmonomial(new Exmonomial(OmegaBigDecimal.ONE));

        return one;
    }

    /**
     * Builds a new instance equal to a given constant.
     *
     * @param value input value
     * @return a constant expolynomial equal to {@code value}
     */
    public static Expolynomial newConstantInstance(OmegaBigDecimal value) {
        Expolynomial constant = new Expolynomial();
        constant.addExmonomial(new Exmonomial(value));

        return constant;
    }

    private List<Exmonomial> exmonomials;
    public static final MathContext mathContext = MathContext.DECIMAL128;

    /**
     * Builds an empty expolynomial.
     */
    public Expolynomial() {
        exmonomials = new ArrayList<Exmonomial>();
    }

    /**
     * Builds the copy of an expolynomial.
     *
     * @param expol input expolynomial
     */
    public Expolynomial(Expolynomial expol) {
        exmonomials = new ArrayList<Exmonomial>();
        for (int i = 0; i < expol.getExmonomials().size(); i++)
            exmonomials.add(new Exmonomial(expol.getExmonomials().get(i)));
    }

    public List<Exmonomial> getExmonomials() {
        return exmonomials;
    }

    /**
     * Adds an exmonomial to the sum.
     *
     * @param exmon input exmonomial
     */
    public void addExmonomial(Exmonomial exmon) {
        if (exmon != null)
            exmonomials.add(exmon);
    }

    /**
     * Returns all the variables used in this expolynomial.
     *
     * @return set of variables
     */
    public Collection<Variable> getVariables() {

        Set<Variable> variables = new LinkedHashSet<>();
        for (Exmonomial e : exmonomials)
            variables.addAll(e.getVariables());

        return variables;

    }

    /**
     * Returns the constant value of this expolynomial of throws an
     * {@code IllegalStateException} exception if the expolynomial is not constant.
     *
     * @return constant value of the expolynomial
     * @throws IllegalStateException if the expolynomial is not constant
     */
    public OmegaBigDecimal getConstant() {

        this.normalize();
        if (this.exmonomials.size() == 0)
            return OmegaBigDecimal.ZERO;
        else if (this.exmonomials.size() > 1
                || this.exmonomials.get(0).getAtomicTerms().size() > 0)
            throw new IllegalStateException("The expolynomial is not constant");
        else
            return this.exmonomials.get(0).getConstantTerm();
    }

    /**
     * Normalizes the expolynomial, expanding all products and sums.
     */
    public void normalize() {
        Expolynomial expol = new Expolynomial();
        for (int i = 0; i < exmonomials.size(); i++) {
            exmonomials.get(i).normalize();
            if (exmonomials.get(i).getConstantTerm()
                    .compareTo(OmegaBigDecimal.ZERO) != 0)
                expol.addExmonomial(new Exmonomial(exmonomials.get(i)));
        }

        Expolynomial finalExpol = new Expolynomial();

        boolean[] pos = new boolean[expol.getExmonomials().size()];

        for (int i = 0; i < expol.getExmonomials().size(); i++) {
            if (pos[i] == false) {
                Exmonomial exmon1 = expol.getExmonomials().get(i);
                for (int j = i + 1; j < expol.getExmonomials().size(); j++) {
                    Exmonomial exmon2 = expol.getExmonomials().get(j);
                    if (exmon1.isSameForm(exmon2)) {
                        exmon1.setConstantTerm(exmon1.getConstantTerm().add(
                                exmon2.getConstantTerm()));
                        pos[j] = true;
                    }
                }
                if (exmon1.getConstantTerm().compareTo(OmegaBigDecimal.ZERO) != 0)
                    finalExpol.addExmonomial(exmon1);
            }
        }

        this.exmonomials = finalExpol.exmonomials;
    }

    /**
     * Adds another expolynomial.
     *
     * @param other input expolynomial
     */
    public void add(Expolynomial other) {

        for (Exmonomial e : other.exmonomials)
            this.exmonomials.add(new Exmonomial(e));

        this.normalize();
    }

    /**
     * Subtracts another expolynomial.
     *
     * @param other input expolynomial
     */
    public void sub(Expolynomial other) {

        for (Exmonomial exmon : other.getExmonomials()) {
            Exmonomial m = new Exmonomial(exmon);
            m.setConstantTerm(m.getConstantTerm().negate());
            this.addExmonomial(m);
        }

        this.normalize();
    }

    /**
     * Multiplies by an expolynomial.
     *
     * @param other input expolynomial
     */
    public void multiply(Expolynomial other) {

        List<Exmonomial> result = new ArrayList<Exmonomial>();
        for (Exmonomial exmon : this.exmonomials)
            for (Exmonomial exmonOther : other.exmonomials) {
                Exmonomial product = new Exmonomial(exmon);
                product.multiply(exmonOther);
                result.add(product);
            }

        this.exmonomials = result;
        this.normalize();
    }

    /**
     * Multiplies by a constant.
     *
     * @param k constant
     */
    public void multiply(BigDecimal k) {

        for (Exmonomial exmon : this.exmonomials)
            exmon.multiply(new OmegaBigDecimal(k));
    }

    /**
     * Divides by a constant.
     *
     * @param k constant
     */
    public void divide(BigDecimal k) {

        if (k.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Division by zero");

        for (Exmonomial exmon : this.exmonomials)
            exmon.divide(k);
    }

    /**
     * Replaces {@code oldVar} with {@code newVar}.
     *
     * @param oldVar variable to be replaced
     * @param newVar new variable
     */
    public void substitute(Variable oldVar, Variable newVar) {

        for (Exmonomial exmon : this.exmonomials)
            exmon.substitute(oldVar, newVar);

        this.normalize();
    }

    /**
     * Evaluates the expolynomial.
     *
     * @param m map with values for all the variables
     * @return expolynomial value
     */
    public OmegaBigDecimal evaluate(Map<Variable, OmegaBigDecimal> m) {

        OmegaBigDecimal result = OmegaBigDecimal.ZERO;

        for (int i = 0; i < exmonomials.size(); i++)
            result = result.add(exmonomials.get(i).evaluate(m));

        return result;
    }

    /**
     * Replaces a variable with its value.
     *
     * @param var variable to be replaced
     * @param value value of the variable
     * @return expolynomial after the substitution
     */
    public Expolynomial evaluate(Variable var, OmegaBigDecimal value) {

        Expolynomial expol = new Expolynomial();
        for (int i = 0; i < exmonomials.size(); i++)
            expol.addExmonomial(exmonomials.get(i).evaluate(var, value));

        expol.normalize();
        return expol;
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
    public Expolynomial evaluate(Variable base, boolean sign, Variable offset,
            BigDecimal constant) {

        Expolynomial expol = new Expolynomial();

        for (int i = 0; i < exmonomials.size(); i++)
            expol.getExmonomials().addAll(
                    (exmonomials.get(i).evaluate(base, sign, offset, constant)
                            .getExmonomials()));

        expol.normalize();
        return expol;
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
    public Expolynomial evaluate(Variable base, boolean sign1,
            Variable offset1, boolean sign2, Variable offset2,
            BigDecimal constant) {

        Expolynomial expol = new Expolynomial();

        for (int i = 0; i < exmonomials.size(); i++)
            expol.getExmonomials().addAll(
                    (exmonomials.get(i).evaluate(base, sign1, offset1, sign2,
                            offset2, constant).getExmonomials()));

        expol.normalize();
        return expol;
    }


    /**
     * Replaces <code>base</code> with <code>base</code> + <code>offset</code>.
     *
     * @param base variable to be replaced
     * @param offset offset to be applied
     */
    public void shift(Variable base, Variable offset) {

        List<Exmonomial> result = new ArrayList<Exmonomial>();
        for (Exmonomial exmon : this.exmonomials)
            result.addAll(exmon.shift(base, offset).getExmonomials());

        this.exmonomials = result;
        this.normalize();
    }

    /**
     * Computes the primitive function of this expolynomial.
     *
     * @param var integration variable
     * @return primitive function
     */
    public Expolynomial integrate(Variable var) {

        Expolynomial expol = new Expolynomial();

        for (int i = 0; i < exmonomials.size(); i++)
            expol.getExmonomials().addAll(
                    (exmonomials.get(i).integrate(var).getExmonomials()));

        expol.normalize();
        return expol;
    }

    /**
     * Integrates this expolynomial over an interval.
     *
     * @param var integration variable
     * @param lower lower bound
     * @param upper upper bound
     * @return resulting expolynomial
     */
    public Expolynomial integrate(Variable var, OmegaBigDecimal lower,
            OmegaBigDecimal upper) {

        Expolynomial expol = new Expolynomial();

        for (int i = 0; i < exmonomials.size(); i++)
            expol.getExmonomials().addAll(
                    (exmonomials.get(i).integrate(var, lower, upper)
                            .getExmonomials()));

        expol.normalize();
        return expol;
    }

    /**
     * Takes the n-th power of this expolynomial.
     *
     * @param n exponent
     */
    public void pow(int n) {

        if (n < 0) {
            throw new IllegalArgumentException("Negative exponentiation");

        } else if (n == 0) {
            this.exmonomials.clear();
            this.exmonomials.add(new Exmonomial(OmegaBigDecimal.ONE));

        } else {
            Expolynomial base = new Expolynomial(this);
            for (int i = 1; i < n; i++)
                this.multiply(base);
            this.normalize();
        }
    }

    @Override
    public String toString() {

        if (exmonomials.size() == 0)
            return "0";

        StringBuilder b = new StringBuilder();

        String join = "";
        for (int i = 0; i < exmonomials.size(); i++) {
            b.append(join);
            b.append(exmonomials.get(i).toString());
            join = " + ";
        }

        return b.toString();
    }

    /**
     * Checks if this expolynomial is constant.
     *
     * @return true if this expolynomial is constant.
     */
    public boolean isConstant() {
        return exmonomials.size() == 1
                && exmonomials.get(0).getAtomicTerms().size() == 0;
    }

    /**
     * Checks if this expolynomial contains a single exponential term.
     *
     * @return true if this expolynomial contains a single exponential term
     */
    public boolean isExponential() {
        return exmonomials.size() == 1
                && exmonomials.get(0).getAtomicTerms().size() == 1
                && exmonomials.get(0).getAtomicTerms().get(0) instanceof ExponentialTerm;
    }

    /**
     * The exponential rate of the single exponential term contained in this
     * expolynomial. If other terms are present, throws an
     * {@code IllegalStateException}.
     *
     * @return true if this expolynomial contains a single exponential term
     * @throws IllegalStateException if this is not a single exponential term
     */
    public BigDecimal getExponentialRate() {
        if (!isExponential())
            throw new IllegalStateException();

        return ((ExponentialTerm) exmonomials.get(0).getAtomicTerms().get(0))
                .getLambda();
    }

    /**
     * Builds an exponential from a string.
     *
     * @param string input string
     * @return exponential instantance
     */
    public static Expolynomial fromString(String string) {
        Expolynomial expol = new Expolynomial();

        StringTokenizer exmonomialsString = new StringTokenizer(string, "+");

        while (exmonomialsString.hasMoreTokens()) {

            String exmonomialString = exmonomialsString.nextToken().trim();
            Exmonomial exmonomial = new Exmonomial(BigDecimal.ONE);

            StringTokenizer terms = new StringTokenizer(exmonomialString, "*");
            while (terms.hasMoreTokens()) {
                String term = terms.nextToken().trim();

                try {
                    exmonomial.setConstantTerm(exmonomial.getConstantTerm()
                            .multiply(new OmegaBigDecimal(term)));

                } catch (NumberFormatException e) {
                    // not a BigDecimal value
                    if (term.indexOf("x") == 0) {
                        if (term.length() == 1) {
                            exmonomial.addAtomicTerm(new MonomialTerm(
                                    Variable.X, 1));

                        } else if (term.indexOf("^") == 1) {
                            Integer alpha = Integer.parseInt(term.substring(2));
                            exmonomial.addAtomicTerm(new MonomialTerm(
                                    Variable.X, alpha));

                        } else
                            throw new IllegalArgumentException();

                    } else if (term.indexOf("Exp[") == 0
                            && term.indexOf("x]") == (term.length() - 2)) {

                        exmonomial.addAtomicTerm(new ExponentialTerm(
                                Variable.X, new BigDecimal(term.substring(4,
                                        term.length() - 2).trim()).negate()));

                    } else
                        throw new IllegalArgumentException();
                }
            }
            expol.addExmonomial(exmonomial);
        }

        expol.normalize();
        return expol;
    }

    /**
     * Checks if the input string is a valid expolynomial expression.
     *
     * @param string input string
     * @return true if the input string is a valid expolynomial expression.
     */
    public static boolean isValid(String string) {

        try {
            StringTokenizer exmonomialsString = new StringTokenizer(string, "+");
            while (exmonomialsString.hasMoreTokens()) {

                String exmonomialString = exmonomialsString.nextToken().trim();
                StringTokenizer terms = new StringTokenizer(exmonomialString,
                        "*");
                while (terms.hasMoreTokens()) {
                    String term = terms.nextToken().trim();

                    try {
                        new BigDecimal(term);

                    } catch (NumberFormatException e) {
                        // not a BigDecimal value
                        if (term.indexOf("x") == 0) {
                            if (term.length() == 1) {
                                // good term

                            } else if (term.indexOf("^") == 1) {
                                Integer.parseInt(term.substring(2));

                            } else
                                throw new IllegalArgumentException();

                        } else if (term.indexOf("Exp[") == 0
                                && term.indexOf("x]") == (term.length() - 2)) {
                            new BigDecimal(term.substring(4, term.length() - 2)
                                    .trim()).negate();

                        } else
                            throw new IllegalArgumentException();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }
}
