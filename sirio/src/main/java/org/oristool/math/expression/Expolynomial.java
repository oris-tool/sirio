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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.oristool.math.OmegaBigDecimal;

/**
 * A sum of exmonomial terms.
 */
public class Expolynomial {

    public static Expolynomial newOneInstance() {
        Expolynomial one = new Expolynomial();
        one.addExmonomial(new Exmonomial(OmegaBigDecimal.ONE));

        return one;
    }

    public static Expolynomial newConstantInstance(OmegaBigDecimal value) {
        Expolynomial constant = new Expolynomial();
        constant.addExmonomial(new Exmonomial(value));

        return constant;
    }

    /**
     * Lista rappresentante la somma di prodotti, dove un prodotto è un
     * exmonomio
     */
    private List<Exmonomial> exmonomials;

    /**
     * Contesto matematico per indicare il numero di cifre significative da
     * usare nel calcolo
     */
    public static final MathContext mathContext = MathContext.DECIMAL128;

    /**
     * Costruisce un expolinomio nullo
     */
    public Expolynomial() {
        exmonomials = new ArrayList<Exmonomial>();
    }

    /**
     * Costruisce un expolinomio a partire da un altro expolinomio
     *
     * @param expol
     *            expolinomio da copiare
     * @throws NegativeAlphaException
     *             sollevata in caso di esponente negativo ritrovato all'interno
     *             di un termine monomiale
     */
    public Expolynomial(Expolynomial expol) {
        exmonomials = new ArrayList<Exmonomial>();
        for (int i = 0; i < expol.getExmonomials().size(); i++)
            exmonomials.add(new Exmonomial(expol.getExmonomials().get(i)));
    }

    /**
     * Restituisce tutti gli exmonomi
     *
     * @return lista di exmonomi componenti l'expolinomio
     */
    public List<Exmonomial> getExmonomials() {
        return exmonomials;
    }

    /**
     * Aggiunge un exmonomio a quelli già presenti
     *
     * @param exmon
     *            exmonomio da aggiungere
     */
    public void addExmonomial(Exmonomial exmon) {
        if (exmon != null)
            exmonomials.add(exmon);
    }

    public Collection<Variable> getVariables() {

        Set<Variable> variables = new LinkedHashSet<Variable>();
        for (Exmonomial e : exmonomials)
            variables.addAll(e.getVariables());

        return variables;

    }

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

    public void add(Expolynomial other) {

        for (Exmonomial e : other.exmonomials)
            this.exmonomials.add(new Exmonomial(e));

        this.normalize();
    }

    public void sub(Expolynomial other) {

        for (Exmonomial exmon : other.getExmonomials()) {
            Exmonomial m = new Exmonomial(exmon);
            m.setConstantTerm(m.getConstantTerm().negate());
            this.addExmonomial(m);
        }

        this.normalize();
    }

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

    public void multiply(BigDecimal k) {

        for (Exmonomial exmon : this.exmonomials)
            exmon.multiply(new OmegaBigDecimal(k));
    }

    public void divide(BigDecimal k) {

        if (k.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Division by zero");

        for (Exmonomial exmon : this.exmonomials)
            exmon.divide(k);
    }

    public void substitute(Variable oldVar, Variable newVar) {

        for (Exmonomial exmon : this.exmonomials)
            exmon.substitute(oldVar, newVar);

        this.normalize();
    }

    public OmegaBigDecimal evaluate(Map<Variable, OmegaBigDecimal> m) {

        OmegaBigDecimal result = OmegaBigDecimal.ZERO;

        for (int i = 0; i < exmonomials.size(); i++)
            result = result.add(exmonomials.get(i).evaluate(m));

        return result;
    }

    public Expolynomial evaluate(Variable var, OmegaBigDecimal value) {

        Expolynomial expol = new Expolynomial();
        for (int i = 0; i < exmonomials.size(); i++)
            expol.addExmonomial(exmonomials.get(i).evaluate(var, value));

        expol.normalize();
        return expol;
    }

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

    public void shift(Variable base, Variable offset) {

        List<Exmonomial> result = new ArrayList<Exmonomial>();
        for (Exmonomial exmon : this.exmonomials)
            result.addAll(exmon.shift(base, offset).getExmonomials());

        this.exmonomials = result;
        this.normalize();
    }

    public Expolynomial integrate(Variable var) {

        Expolynomial expol = new Expolynomial();

        for (int i = 0; i < exmonomials.size(); i++)
            expol.getExmonomials().addAll(
                    (exmonomials.get(i).integrate(var).getExmonomials()));

        expol.normalize();
        return expol;
    }

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

    public void pow(Integer n) {
        if (n < 0)
            throw new IllegalArgumentException("Negative exponentiation");
        else if (n == 0) {
            this.exmonomials.clear();
            this.exmonomials.add(new Exmonomial(OmegaBigDecimal.ONE));
        } else {
            Expolynomial base = new Expolynomial(this);
            for (int i = 1; i < n; i++)
                this.multiply(base);
            this.normalize();
        }
    }

    /**
     * Stringa rappresentante l'expolinomio
     */
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

    public Expolynomial limit(Map<Variable, OmegaBigDecimal> toValues) {

        // separates variables tending to positive or negative infinity
        Set<Variable> pInfVars = new LinkedHashSet<Variable>();
        Set<Variable> nInfVars = new LinkedHashSet<Variable>();
        Map<Variable, OmegaBigDecimal> finiteVars = new HashMap<Variable, OmegaBigDecimal>();

        for (Entry<Variable, OmegaBigDecimal> e : toValues.entrySet())
            if (e.getValue().equals(OmegaBigDecimal.POSITIVE_INFINITY))
                pInfVars.add(e.getKey());
            else if (e.getValue().equals(OmegaBigDecimal.NEGATIVE_INFINITY))
                nInfVars.add(e.getKey());
            else
                finiteVars.put(e.getKey(), e.getValue());

        // the minimum total exponential rate among all the exmonomials for
        // variables tending to +inf
        // (if negative, the expolynomial diverges to +inf or -inf, depending on
        // the sign of the sum of
        // constant terms)
        BigDecimal minPInfExponentialRate = null;

        // the maximum monomial degree among all the exmonomials with minimum
        // total exponential rate
        int maxInfMonomialDegree = 0;

        // builds the limiting expolynomial: this includes only non-null finite
        // terms
        Expolynomial result = new Expolynomial();

        for (int i = 0; i < this.exmonomials.size(); ++i) {
            // computes the limit of this exmonomial
            Exmonomial limitingExmonomial = new Exmonomial(this.exmonomials
                    .get(i).getConstantTerm());

            // total rate of exponential terms tending to +inf in this
            // exmonomial
            // (terms tending to -inf are counted with a negated rate)
            BigDecimal pInfExponentialRate = BigDecimal.ZERO;

            // total degree of monomial terms tending to +inf in this exmonomial
            // (terms tending to -inf with an odd degree change the sign)
            int infMonomialDegree = 0;
            BigDecimal infMonomialSign = BigDecimal.ONE;

            // total exponent of exponential terms of a variable tending to a
            // finite value
            // (avoids multiple exponentiations within the same exmonomial)
            BigDecimal totalExponent = BigDecimal.ZERO;

            for (AtomicTerm a : this.getExmonomials().get(i).getAtomicTerms()) {
                if (a instanceof ExponentialTerm) {
                    ExponentialTerm e = (ExponentialTerm) a;

                    if (pInfVars.contains(e.getVariable())) {
                        pInfExponentialRate = pInfExponentialRate.add(e
                                .getLambda());

                    } else if (nInfVars.contains(e.getVariable())) {
                        pInfExponentialRate = pInfExponentialRate.subtract(e
                                .getLambda());

                    } else if (finiteVars.containsKey(e.getVariable())) {
                        totalExponent = totalExponent.subtract(e.getLambda()
                                .multiply(
                                        finiteVars.get(e.getVariable())
                                                .bigDecimalValue()));

                    } else {
                        limitingExmonomial.addAtomicTerm(a);
                    }

                } else {
                    MonomialTerm m = (MonomialTerm) a;

                    if (pInfVars.contains(m.getVariable())) {
                        infMonomialDegree += m.getAlpha();

                    } else if (nInfVars.contains(m.getVariable())) {
                        infMonomialDegree += m.getAlpha();
                        if (m.getAlpha() % 2 == 1)
                            infMonomialSign = infMonomialSign.negate();

                    } else if (finiteVars.containsKey(m.getVariable())) {
                        limitingExmonomial.multiply(finiteVars.get(
                                m.getVariable()).pow(m.getAlpha()));

                    } else {
                        limitingExmonomial.addAtomicTerm(m);
                    }
                }
            }

            if (minPInfExponentialRate == null
                    || pInfExponentialRate.compareTo(minPInfExponentialRate) < 0
                    || (pInfExponentialRate.compareTo(minPInfExponentialRate) == 0 && infMonomialDegree < maxInfMonomialDegree)) {

                // updates the minimum rate / maximum degree
                minPInfExponentialRate = pInfExponentialRate;
                maxInfMonomialDegree = infMonomialDegree;

                // throws away the other terms (they diverge less)
                result = new Expolynomial();
            }

            if (pInfExponentialRate.compareTo(minPInfExponentialRate) == 0
                    && infMonomialDegree == maxInfMonomialDegree) {

                // adds this term (it diverges as much as the most divergent
                // ones)

                result.addExmonomial(limitingExmonomial);
            }
        }

        return result;

    }

    public boolean isConstant() {
        return exmonomials.size() == 1
                && exmonomials.get(0).getAtomicTerms().size() == 0;
    }

    public boolean isExponential() {
        return exmonomials.size() == 1
                && exmonomials.get(0).getAtomicTerms().size() == 1
                && exmonomials.get(0).getAtomicTerms().get(0) instanceof ExponentialTerm;
    }

    public BigDecimal getExponentialRate() {
        return ((ExponentialTerm) exmonomials.get(0).getAtomicTerms().get(0))
                .getLambda();
    }

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
