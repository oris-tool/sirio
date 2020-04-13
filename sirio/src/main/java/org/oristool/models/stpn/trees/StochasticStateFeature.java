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

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.analyzer.state.StateFeature;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.EXP;
import org.oristool.math.function.Erlang;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.math.function.StateDensityFunction;

/**
 * A state feature encoding the support and PDF of enabled timers.
 */
public class StochasticStateFeature implements StateFeature {

    private boolean vanishing;
    private boolean absorbing;

    private StateDensityFunction stateDensity = new StateDensityFunction();
    private Map<Variable, BigDecimal> exponentials = new LinkedHashMap<Variable, BigDecimal>();
    private Set<Variable> ageVariables = new LinkedHashSet<Variable>();

    private BigDecimal epsilon;
    private int numSamples;

    /**
     * Builds an empty stochastic state feature.
     *
     * <p>With epsilon == null, no approximated comparison is performed (e.g. in
     * transient analysis).
     *
     * @param epsilon allowed error in comparisons between states
     * @param numSamples samples used to compare states
     */
    public StochasticStateFeature(BigDecimal epsilon, int numSamples) {

        this.epsilon = epsilon;
        this.numSamples = numSamples;
    }

    /**
     * Builds the copy of a stochastic state feature.
     *
     * @param other another stochastic state feature
     */
    public StochasticStateFeature(StochasticStateFeature other) {

        this.vanishing = other.vanishing;
        this.absorbing = other.absorbing;

        this.stateDensity = new StateDensityFunction(other.getStateDensity());
        this.exponentials = new LinkedHashMap<Variable, BigDecimal>(
                other.exponentials);
        this.ageVariables = new LinkedHashSet<Variable>(other.ageVariables);

        this.epsilon = other.epsilon;
        this.numSamples = other.numSamples;
    }

    /**
     * Returns the set of age variables.
     *
     * @return age variables
     */
    public Set<Variable> getAgeVariables() {
        return Collections.unmodifiableSet(ageVariables);
    }

    /**
     * Returns the set of general, non-age variables.
     *
     * @return set of general, non-age variables
     */
    public Set<Variable> getFiringVariables() {
        Set<Variable> firingVariables = new LinkedHashSet<Variable>(
                stateDensity.getVariables());
        firingVariables.removeAll(ageVariables);
        return firingVariables;
    }

    /**
     * Adds an age variable.
     *
     * @param v input variable
     */
    public void addAgeVariable(Variable v) {
        addVariable(v, GEN.newDeterministic(BigDecimal.ZERO));
        ageVariables.add(v);
    }

    /**
     * Adds a deterministic age variable.
     *
     * @param v variable
     * @param value value of the variable
     */
    public void addAgeVariable(Variable v, BigDecimal value) {
        addVariable(v, GEN.newDeterministic(value));
        ageVariables.add(v);
    }

    /**
     * Adds a variable with the input PDF, reduced of given amount.
     *
     * @param v variable
     * @param f PDF
     * @param amount reduction
     */
    public void addVariableReduced(Variable v, PartitionedFunction f, BigDecimal amount) {

        // adds a new variable to the stochastic feature
        OmegaBigDecimal lft = f.getDomainsLFT();
        OmegaBigDecimal eft = f.getDomainsEFT();

        if (eft.equals(lft)) {
            BigDecimal value = lft.bigDecimalValue().subtract(amount);
            if (value.compareTo(BigDecimal.ZERO) >= 0)
                stateDensity.addDeterministicVariable(v, value);
            else
                throw new IllegalArgumentException(
                        "Cannot add deterministic negative variable " + v + "="
                                + value);

        } else if (f instanceof EXP) {
            this.addExpVariable(v, ((EXP) f).getLambda());

        } else if (f instanceof GEN) {
            GEN reduced = new GEN((GEN)f);
            reduced.conditionToMin(Variable.X, new OmegaBigDecimal(amount));
            reduced.constantShift(amount);
            stateDensity.addContinuousVariable(v, reduced);
        } else if (f instanceof PartitionedGEN) {
            PartitionedGEN reduced = new PartitionedGEN((PartitionedGEN)f);
            reduced.conditionToMin(Variable.X, new OmegaBigDecimal(amount));
            reduced.constantShift(amount);
            stateDensity.addContinuousVariable(v, reduced);
        } else if (f instanceof Erlang) {
            GEN reduced = new GEN((Erlang)f);
            reduced.conditionToMin(Variable.X, new OmegaBigDecimal(amount));
            reduced.constantShift(amount);
            stateDensity.addContinuousVariable(v, reduced);
        } else
            throw new IllegalArgumentException("Unknown function type");

    }

    /**
     * Adds a variable with the given PDF.
     *
     * @param v variable
     * @param f PDF
     */
    public void addVariable(Variable v, PartitionedFunction f) {

        // adds a new variable to the stochastic feature
        OmegaBigDecimal lft = f.getDomainsLFT();
        OmegaBigDecimal eft = f.getDomainsEFT();

        if (eft.equals(lft))
            stateDensity.addDeterministicVariable(v, lft.bigDecimalValue());

        else if (f instanceof EXP)
            this.addExpVariable(v, ((EXP) f).getLambda());

        else
            stateDensity.addContinuousVariable(v, f);
    }

    /**
     * Adds an exponentially-distributed variable.
     *
     * @param v variable
     * @param rate rate
     */
    public void addExpVariable(Variable v, BigDecimal rate) {

        if (exponentials.keySet().contains(v)
                || stateDensity.getVariables().contains(v))
            throw new IllegalArgumentException("The variable " + v
                    + " is already present");

        this.exponentials.put(v, rate);
    }

    public void removeExpVariable(Variable v) {

        this.exponentials.remove(v);
    }

    public Set<Variable> getEXPVariables() {

        return this.exponentials.keySet();
    }

    public BigDecimal getEXPRate(Variable v) {

        return this.exponentials.get(v);
    }

    public void setEXPRate(Variable v, BigDecimal rate) {

        this.exponentials.put(v, rate);
    }

    public Set<Entry<Variable, BigDecimal>> getEXPRates() {

        return this.exponentials.entrySet();
    }

    /**
     * Returns the sum of rates of all exponential variables.
     *
     * @return total exponential rate
     */
    public BigDecimal getTotalExpRate() {

        // Computes the total lambda
        BigDecimal totalLambda = BigDecimal.ZERO;
        for (BigDecimal rate : exponentials.values())
            totalLambda = totalLambda.add(rate);

        return totalLambda;
    }

    /**
     * Returns the sum of rates of a subset of exponential variables.
     *
     * @param expVariables input variables
     * @return total exponential rate of the input variables
     */
    public BigDecimal getTotalExpRate(Set<Variable> expVariables) {

        // Computes the total lambda
        BigDecimal totalLambda = BigDecimal.ZERO;
        for (Variable v : expVariables)
            totalLambda = totalLambda.add(exponentials.get(v));

        return totalLambda;
    }

    /**
     * Adds a truncated EXP [0, lft] to the state density.
     *
     * @param v variable of the EXP to be added
     * @param rate rate of the EXP to be added
     * @param lft truncation threshold
     */
    public void addTruncatedExp(Variable v, BigDecimal rate, OmegaBigDecimal lft) {

        if (this.getEXPVariables().contains(v)
                || this.stateDensity.getVariables().contains(v))
            throw new IllegalArgumentException("The variable " + v
                    + " is already present");

        GEN truncateExp = GEN.newTruncatedExp(Variable.X, rate,
                OmegaBigDecimal.ZERO, lft);
        this.getStateDensity().addContinuousVariable(v, truncateExp);
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();

        b.append("Flags: ");
        String separator = "";

        if (vanishing) {
            b.append(separator);
            b.append("Vanishing");
            separator = " ";
        }

        if (absorbing) {
            b.append(separator);
            b.append("Absorbing");
            separator = " ";
        }

        b.append("\n");

        b.append("Age variables: ");
        boolean first = true;
        for (Variable v : ageVariables) {
            if (first)
                first = false;
            else
                b.append(", ");
            b.append(v);
        }
        b.append("\n");

        b.append("Exponentials: ");
        first = true;
        for (Entry<Variable, BigDecimal> e : exponentials.entrySet()) {
            if (first)
                first = false;
            else
                b.append(", ");
            b.append(e.getKey());
            b.append("~");
            b.append("EXP(");
            b.append(e.getValue());
            b.append(")");
        }
        b.append("\n");

        b.append(stateDensity.toString());

        return b.toString();
    }

    public boolean isVanishing() {
        return vanishing;
    }

    public void setVanishing(boolean isVanishing) {
        this.vanishing = isVanishing;
    }

    public boolean isAbsorbing() {
        return absorbing;
    }

    public void setAbsorbing(boolean isAbsorbing) {
        this.absorbing = isAbsorbing;
    }

    public StateDensityFunction getStateDensity() {
        return stateDensity;
    }

    public void setStateDensity(StateDensityFunction stateDensity) {
        this.stateDensity = stateDensity;
    }

    @Override
    public boolean equals(Object obj) {

        if (epsilon == null) {
            // if an approximated comparison is not
            // required (epsilon==null) uses Object's equals
            return super.equals(obj);

        } else {

            if (obj == this)
                return true;

            if (!(obj instanceof StochasticStateFeature))
                return false;

            StochasticStateFeature o = (StochasticStateFeature) obj;
            return exponentials.equals(o.exponentials)
                    && ageVariables.equals(o.ageVariables)
                    && stateDensity.equals(o.getStateDensity());
        }

    }

    @Override
    public int hashCode() {

        if (epsilon == null) {
            // if an approximated comparison is not
            // required (epsilon==null) uses Object's equals
            return super.hashCode();

        } else {

            int hashCode = 17;

            hashCode = hashCode * 31 + exponentials.hashCode();
            hashCode = hashCode * 31 + ageVariables.hashCode();
            hashCode = hashCode * 31 + stateDensity.hashCode();
            return hashCode;
        }
    }

    /**
     * Computes the mean value of a variable.
     *
     * @param v input variable
     * @return mean value
     */
    public BigDecimal computeMeanValue(Variable v) {

        if (exponentials.containsKey(v)) {
            return BigDecimal.ONE.divide(exponentials.get(v), MathContext.DECIMAL128);
        } else {
            return stateDensity.computeMeanValue(v);
        }
    }

    /**
     * Conditions this PDF to the event where the input variable is minimum.
     *
     * @param v minimum variable
     * @return probability that the input variable is the minimum
     */
    public BigDecimal conditionToMinimum(Variable v) {

        Map<Variable, BigDecimal> exponentials = new LinkedHashMap<>(this.exponentials);

        // adds EXP variables to the partitionedGEN
        for (Variable exp: exponentials.keySet()) {
            this.removeExpVariable(exp);
            this.addTruncatedExp(exp, exponentials.get(exp), OmegaBigDecimal.POSITIVE_INFINITY);
        }

        Set<Variable> otherVars = this.getFiringVariables();
        otherVars.remove(v);

        // conditioning the state density
        BigDecimal minProbability =
                this.stateDensity.conditionAllToBound(v, otherVars, OmegaBigDecimal.ZERO);

        return minProbability;
    }
}
