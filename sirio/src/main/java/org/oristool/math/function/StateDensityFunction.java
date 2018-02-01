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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;

public class StateDensityFunction {

    private Map<Variable, BigDecimal> deterministicVariables;
    private Map<Variable, Synchronization> synchronizedVariables;
    private PartitionedGEN partitionedGEN;

    public StateDensityFunction() {

        this.deterministicVariables = new LinkedHashMap<Variable, BigDecimal>();
        this.synchronizedVariables = new LinkedHashMap<Variable, Synchronization>();
        this.partitionedGEN = PartitionedGEN.newOneInstance();
    }

    public StateDensityFunction(StateDensityFunction other) {

        this.deterministicVariables = new LinkedHashMap<Variable, BigDecimal>(
                other.deterministicVariables);
        this.synchronizedVariables = new LinkedHashMap<Variable, Synchronization>(
                other.synchronizedVariables);
        this.partitionedGEN = new PartitionedGEN(other.partitionedGEN);
    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof StateDensityFunction))
            return false;

        StateDensityFunction o = (StateDensityFunction) other;

        if (!deterministicVariables.equals(o.deterministicVariables))
            return false;

        if (!synchronizedVariables.equals(o.synchronizedVariables))
            return false;

        if (!partitionedGEN.equals(o.partitionedGEN))
            return false;

        return true;
    }

    public boolean equals(StateDensityFunction o, int numSamples,
            BigDecimal epsilon) {

        if (this == o)
            return true;

        if (!deterministicVariables.equals(o.deterministicVariables))
            return false;

        if (!synchronizedVariables.equals(o.synchronizedVariables))
            return false;

        return partitionedGEN.equals(o.partitionedGEN, numSamples, epsilon);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + this.deterministicVariables.hashCode();
        result = 31 * result + this.synchronizedVariables.hashCode();
        result = 31 * result + this.partitionedGEN.getFunctions().size();

        return result;
    }

    public PartitionedGEN getContinuosVariablesDensity() {
        return new PartitionedGEN(partitionedGEN);
    }

    // variable adders
    public void addDeterministicVariable(Variable v, BigDecimal value) {

        if (deterministicVariables.keySet().contains(v)
                || synchronizedVariables.keySet().contains(v)
                || partitionedGEN.getVariables().contains(v))
            throw new IllegalArgumentException("The variable " + v
                    + " is already present");

        deterministicVariables.put(v, value);
    }

    public void addSynchronizedVariable(Variable v, Variable distributed,
            BigDecimal delay) {

        if (distributed.equals(v))
            throw new IllegalArgumentException("The variable " + v
                    + " cannot be synchronized with itself");

        if (!this.getContinuousVariables().contains(distributed))
            throw new IllegalArgumentException("The variable " + distributed
                    + " should be a GEN variable");

        if (deterministicVariables.keySet().contains(v)
                || synchronizedVariables.keySet().contains(v)
                || partitionedGEN.getVariables().contains(v))
            throw new IllegalArgumentException("The variable " + v
                    + " is already present");

        synchronizedVariables.put(v, new Synchronization(distributed, delay));
    }

    public void addContinuousVariable(Variable v, PartitionedFunction f) {

        for (DBMZone d: f.getDomains()) {
            if (d.getVariables().size() != 2)
                throw new IllegalArgumentException("The variable " + v
                        + " is not the only variable of " + f);
        }
        
        if (deterministicVariables.keySet().contains(v)
                || synchronizedVariables.keySet().contains(v)
                || partitionedGEN.getVariables().contains(v))
            throw new IllegalArgumentException("The variable " + v
                    + " is already present");

        List<GEN> piecesRenamed = new ArrayList<>(f.getFunctions().size());
        for (Function piece: f.getFunctions()) {
            GEN g = new GEN(piece);

            g.getDomain().substitute(Variable.X, v);
            g.getDensity().substitute(Variable.X, v);
            piecesRenamed.add(g);
        }
        
        this.partitionedGEN = this.partitionedGEN.cartesianProduct(new PartitionedGEN(piecesRenamed));
    }

    // variable getters
    public Set<Variable> getVariables() {

        Set<Variable> variables = new LinkedHashSet<Variable>();
        variables.addAll(this.getDeterministicVariables());
        variables.addAll(this.getSynchronizedVariables());
        variables.addAll(this.getContinuousVariables());

        return variables;
    }

    public Set<Variable> getContinuousVariables() {

        return this.partitionedGEN.getVariables();
    }

    public Set<Variable> getDeterministicVariables() {

        return deterministicVariables.keySet();
    }

    public Set<Entry<Variable, BigDecimal>> getDeterministicValues() {

        return deterministicVariables.entrySet();
    }

    public Entry<Variable, BigDecimal> getLowestDeterministicValue() {

        Entry<Variable, BigDecimal> lowest = null;

        for (Entry<Variable, BigDecimal> e : deterministicVariables.entrySet())
            if (lowest == null || e.getValue().compareTo(lowest.getValue()) < 0)
                lowest = e;

        return lowest;
    }

    public Entry<Variable, BigDecimal> getLowestDeterministicValue(
            Set<Variable> detVariables) {

        Entry<Variable, BigDecimal> lowest = null;

        for (Entry<Variable, BigDecimal> e : deterministicVariables.entrySet())
            if (detVariables.contains(e.getKey()) && lowest == null
                    || e.getValue().compareTo(lowest.getValue()) < 0)
                lowest = e;

        return lowest;
    }

    public BigDecimal getDeterministicValue(Variable v) {

        return deterministicVariables.get(v);
    }

    public Set<Variable> getSynchronizedVariables() {

        return synchronizedVariables.keySet();
    }

    public Synchronization getSynchronization(Variable v) {

        return synchronizedVariables.get(v);
    }

    // variable removers
    public void marginalizeVariable(Variable v) {

        if (deterministicVariables.containsKey(v)) {
            this.deterministicVariables.remove(v);

        } else if (synchronizedVariables.containsKey(v)) {
            this.synchronizedVariables.remove(v);

        } else if (this.partitionedGEN.getVariables().contains(v)) {

            Variable lowestSynchronized = this.getLowestSynchronizedWrt(v);
            if (lowestSynchronized != null) {
                this.swap(lowestSynchronized);
                this.synchronizedVariables.remove(v);
            } else
                this.partitionedGEN.project(v);
        } else
            throw new IllegalArgumentException("The variable " + v
                    + " is not present");

    }

    public void swap(Variable synch) {

        Synchronization s = synchronizedVariables.get(synch);
        if (s == null)
            throw new IllegalArgumentException("The variable " + synch
                    + " is not synchronized");

        synchronizedVariables.remove(synch);
        synchronizedVariables.put(s.getDistributed(), new Synchronization(
                synch, s.getDelay().negate()));

        for (Entry<Variable, Synchronization> e : synchronizedVariables
                .entrySet())
            if (e.getValue().getDistributed().equals(s.getDistributed()))
                synchronizedVariables.put(e.getKey(), new Synchronization(
                        synch, e.getValue().getDelay().subtract(s.getDelay())));

        // synchronization with exponentials cannot happen
        partitionedGEN.substitute(s.getDistributed(), synch, s.getDelay()
                .negate());
    }

    public Variable getLowestSynchronizedWrt(Variable distributedVariable) {

        Variable lowestSynchronizedVariable = null;
        BigDecimal lowestSynchronizedDelay = null;

        for (Entry<Variable, Synchronization> e : synchronizedVariables
                .entrySet())
            if (e.getValue().getDistributed().equals(distributedVariable))
                if (lowestSynchronizedDelay == null
                        || lowestSynchronizedDelay.compareTo(e.getValue()
                                .getDelay()) > 0) {

                    lowestSynchronizedDelay = e.getValue().getDelay();
                    lowestSynchronizedVariable = e.getKey();
                }

        return lowestSynchronizedVariable;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();

        b.append("Deterministic: ");
        boolean first = true;
        for (Entry<Variable, BigDecimal> e : deterministicVariables.entrySet()) {
            if (first)
                first = false;
            else
                b.append(", ");

            b.append(e.getKey());
            b.append(" = ");
            b.append(e.getValue());
        }
        b.append("\n");

        b.append("Synchronized: ");
        first = true;
        for (Entry<Variable, Synchronization> e : synchronizedVariables
                .entrySet()) {
            if (first)
                first = false;
            else
                b.append(", ");
            b.append(e.getKey());
            b.append(" = ");
            b.append(e.getValue().getDistributed());
            b.append(" + ");
            b.append(e.getValue().getDelay());
        }
        b.append("\n");

        b.append(partitionedGEN.toString());

        return b.toString();
    }

    public Map<Variable, BigDecimal> getSynchDelaysWrt(Variable v) {

        // FIXME Da modificare quando saranno presenti synch suspended
        Variable distributedVariable = null;
        BigDecimal vDelayWrtDistributedVariable = BigDecimal.ZERO;
        for (Entry<Variable, Synchronization> e : synchronizedVariables
                .entrySet())
            if (e.getKey().equals(v)) {
                // v is synchronized with some distributed variable
                vDelayWrtDistributedVariable = e.getValue().getDelay();
                distributedVariable = e.getValue().getDistributed();
                break;
            } else if (e.getValue().getDistributed().equals(v)) {
                // v is distributed and has variables synchronized with it
                distributedVariable = v;
            }

        Map<Variable, BigDecimal> result = new LinkedHashMap<Variable, BigDecimal>();

        // return an empty map if v is not in a synch group
        if (distributedVariable == null)
            return result;

        // add the distributed variable if it is not v
        if (!distributedVariable.equals(v))
            result.put(distributedVariable,
                    vDelayWrtDistributedVariable.negate());

        // add any variable (other than v) synchronized with the same as v
        for (Entry<Variable, Synchronization> e : synchronizedVariables
                .entrySet())
            if (!e.getKey().equals(v)
                    && e.getValue().getDistributed()
                            .equals(distributedVariable))
                // vDelayWrtDistributedVariable equals 0 if v is the distributed
                // variable
                result.put(
                        e.getKey(),
                        e.getValue().getDelay()
                                .subtract(vDelayWrtDistributedVariable));

        return result;
    }

    public BigDecimal getLowestSynchronizedDelayWrt(Variable distributedVariable) {

        Variable lowestSynchronized = this
                .getLowestSynchronizedWrt(distributedVariable);

        if (lowestSynchronized == null)
            return null;
        else
            return synchronizedVariables.get(lowestSynchronized).getDelay();
    }

    public Set<Variable> getNullDelayVariables(Variable v) {

        Set<Variable> nullDelayVariables = new LinkedHashSet<Variable>();

        if (deterministicVariables.containsKey(v)) {
            BigDecimal value = deterministicVariables.get(v);

            // adds other deterministic variables with same value
            for (Entry<Variable, BigDecimal> e : deterministicVariables
                    .entrySet())
                if (!e.getKey().equals(v) && e.getValue().compareTo(value) == 0)
                    nullDelayVariables.add(e.getKey());

        } else if (synchronizedVariables.containsKey(v)) {
            Synchronization s = synchronizedVariables.get(v);

            // adds other synchronized variables with same delay to the same
            // distributed
            for (Entry<Variable, Synchronization> e : synchronizedVariables
                    .entrySet())
                if (!e.getKey().equals(v) && e.getValue().equals(s))
                    nullDelayVariables.add(e.getKey());

            // adds the distributed variable if the delay is zero
            if (s.getDelay().compareTo(BigDecimal.ZERO) == 0)
                nullDelayVariables.add(s.getDistributed());

        } else if (partitionedGEN.getVariables().contains(v)) {

            // adds synchronized variables with null delay
            for (Entry<Variable, Synchronization> e : synchronizedVariables
                    .entrySet())
                if (e.getValue().getDistributed().equals(v)
                        && e.getValue().getDelay().compareTo(BigDecimal.ZERO) == 0)
                    nullDelayVariables.add(e.getKey());

        } else
            throw new IllegalArgumentException("The variable " + v
                    + " is not present");

        return nullDelayVariables;
    }

    public void shiftAndProject(Variable firedVar) {

        if (deterministicVariables.containsKey(firedVar)) {
            // If the fired transition is deterministic, performs a constant
            // shift
            this.constantShift(deterministicVariables.get(firedVar));
            this.marginalizeVariable(firedVar);

        } else { // PartitionedGEN firing
            if (synchronizedVariables.containsKey(firedVar)) {
                // turns a SYNCH firing into a GEN firing (by a swapping)
                swap(firedVar);
            }

            // Updates PartitionedGEN, eventually adding a group of synchronized
            // variables
            if (deterministicVariables.size() > 0) {
                // The lowest deterministic transition becomes a PartitionedGEN
                // (through a variable substitution and a constant shift)
                Entry<Variable, BigDecimal> lowestDeterministicValue = this
                        .getLowestDeterministicValue();
                partitionedGEN.substituteAndShift(firedVar,
                        lowestDeterministicValue.getKey(),
                        lowestDeterministicValue.getValue());

                // Other deterministic transitions become synchronized with the
                // lowest DET
                for (Entry<Variable, BigDecimal> e : deterministicVariables
                        .entrySet())
                    if (!e.getKey().equals(lowestDeterministicValue.getKey()))
                        synchronizedVariables.put(
                                e.getKey(),
                                new Synchronization(lowestDeterministicValue
                                        .getKey(), e.getValue().subtract(
                                        lowestDeterministicValue.getValue())));
            } else {
                // No DETs: partitionedGEN is updated with a shift and project
                // of fired
                // (or set to 1 if fired was the only GEN variable)
                partitionedGEN.shiftAndProject(firedVar);
            }

            // Transitions synchronized with the fired PartitionedGEN become the
            // only DETs
            deterministicVariables.clear();
            for (Entry<Variable, BigDecimal> e : this.getSynchDelaysWrt(
                    firedVar).entrySet()) {
                synchronizedVariables.remove(e.getKey());
                deterministicVariables.put(e.getKey(), e.getValue());
            }
        }
    }

    public void constantShift(BigDecimal constant) {

        // decreases each deterministic variable
        for (Entry<Variable, BigDecimal> e : deterministicVariables.entrySet())
            deterministicVariables.put(e.getKey(),
                    e.getValue().subtract(constant));

        partitionedGEN.constantShift(constant);
    }

    public void constantShift(BigDecimal constant, Set<Variable> progressing) {

        // decreases each deterministic variable
        for (Entry<Variable, BigDecimal> e : deterministicVariables.entrySet())
            if (progressing.contains(e.getKey()))
                deterministicVariables.put(e.getKey(),
                        e.getValue().subtract(constant));

        Set<Variable> genProgressing = new LinkedHashSet<Variable>(
                partitionedGEN.getVariables());
        genProgressing.retainAll(progressing);
        partitionedGEN.constantShift(constant, genProgressing);

        // updates the coefficients of progressing/suspended synchronized
        // variables
        for (Entry<Variable, Synchronization> e : synchronizedVariables
                .entrySet())
            if (progressing.contains(e.getKey())) {
                // the synchronized variable is progressing
                if (!progressing.contains(e.getValue().getDistributed()))
                    // the distributed variable is suspended
                    synchronizedVariables.put(e.getKey(), new Synchronization(e
                            .getValue().getDistributed(), e.getValue()
                            .getDelay().subtract(constant)));
            } else {
                // the synchronized variable is suspended
                if (progressing.contains(e.getValue().getDistributed()))
                    // the distributed variable is progressing
                    synchronizedVariables.put(e.getKey(), new Synchronization(e
                            .getValue().getDistributed(), e.getValue()
                            .getDelay().add(constant)));
            }

    }

    /*
     * Impose the bound without normalizing densities, keep only subzones with
     * non-null measure
     */
    public void imposeBound(Variable leftVar, Variable rightVar,
            OmegaBigDecimal bound) {
        imposeBound(leftVar, Collections.singleton(rightVar), bound);
    }

    /*
     * Impose the bound without normalizing densities, keep only subzones with
     * non-null measure
     */
    public void imposeBound(Variable leftVar, Set<Variable> rightVars,
            OmegaBigDecimal bound) {

        // reduce the case of a synchronized variable to that of a distributed
        // one
        OmegaBigDecimal leftCons = OmegaBigDecimal.ZERO;
        if (deterministicVariables.containsKey(leftVar)) {
            leftCons = new OmegaBigDecimal(deterministicVariables.get(leftVar));
            leftVar = Variable.TSTAR;

        } else if (synchronizedVariables.containsKey(leftVar)) {
            Synchronization s = synchronizedVariables.get(leftVar);
            leftCons = new OmegaBigDecimal(s.getDelay());
            leftVar = s.getDistributed();
        }

        // impose the bound for each rightVar
        for (Variable rightVar : rightVars) {

            // reduce the case of a synchronized variable to that of a
            // distributed one
            OmegaBigDecimal rightCons = OmegaBigDecimal.ZERO;

            if (deterministicVariables.containsKey(rightVar)) {
                rightCons = new OmegaBigDecimal(
                        deterministicVariables.get(rightVar));
                rightVar = Variable.TSTAR;

            } else if (synchronizedVariables.containsKey(rightVar)) {
                Synchronization s = synchronizedVariables.get(rightVar);
                rightCons = new OmegaBigDecimal(s.getDelay());
                rightVar = s.getDistributed();
            }

            if (leftVar.equals(rightVar)) {
                // the bound is a between deterministic values or
                // between variables synchronized with the same one
                if (leftCons.subtract(rightCons).compareTo(bound) > 0) {
                    // the resulting state density function should have null
                    // measure
                    partitionedGEN = new PartitionedGEN();
                    return;
                }
            } else {
                // impose the bound on each subzone
                for (GEN f : partitionedGEN.getFunctions())
                    f.getDomain().imposeBound(leftVar, rightVar,
                            bound.subtract(leftCons).add(rightCons));
                // TODO add equivalent of the check for b_{i0} >= 0
                // for any transition t_i other than tauAge or firedVar
            }
        }

        // keep only subzones with non-null measure
        List<GEN> nonNullFunctions = new ArrayList<GEN>(partitionedGEN
                .getFunctions().size());
        for (GEN f : partitionedGEN.getFunctions())
            if (f.getDomain().isFullDimensional())
                nonNullFunctions.add(f);

        partitionedGEN = new PartitionedGEN(nonNullFunctions);
    }

    /*
     * Returns the probability of conditionement (zero if the resulting state
     * density has null measure)
     */
    public BigDecimal conditionAllToBound(Variable leftVar,
            Set<Variable> rightVars, OmegaBigDecimal bound) {

        // impose that leftVar - rightVar <= bound for any `rightVar` in
        // rightVars
        // aborting in case of an empty state set (unsatisfied deterministic
        // constraints)
        this.imposeBound(leftVar, rightVars, bound);

        if (partitionedGEN.getFunctions().size() == 0)
            return BigDecimal.ZERO;

        if (partitionedGEN.getFunctions().size() == 1
                && partitionedGEN.getFunctions().get(0).getDomain()
                        .getVariables().size() == 1)
            return BigDecimal.ONE;

        // Discard subzones with negligible measure, and
        // normalize the other ones by the /total/ measure of the class
        BigDecimal totalProbability = BigDecimal.ZERO;
        List<GEN> nonNullFunctions = new ArrayList<GEN>(partitionedGEN
                .getFunctions().size());

        for (GEN f : partitionedGEN.getFunctions()) {
            BigDecimal integralOverDomain = f.integrateOverDomain()
                    .bigDecimalValue();
            if (integralOverDomain.compareTo(new BigDecimal("0.0000001")) > 0) {
                // keeps the subzone (density to be conditioned)
                totalProbability = totalProbability.add(integralOverDomain);
                nonNullFunctions.add(f);
            }
        }

        // Condition every density by totalProbability
        // (the integral over the whole PartitionedGEN must be one)
        for (GEN f : nonNullFunctions)
            f.getDensity().divide(totalProbability);

        // Create a new partitioned GEN and sets it as the new state density
        partitionedGEN = new PartitionedGEN(nonNullFunctions);

        return totalProbability;
    }

    public BigDecimal measure() {
        return partitionedGEN.integrateOverDomain().bigDecimalValue();
    }

    public StateDensityFunction getMarginalDensity(Variable v) {

        if (!this.getVariables().contains(v))
            throw new IllegalArgumentException("The variable " + v
                    + " is not present");

        if (this.getDeterministicVariables().contains(v)) {
            // optimization: no need to project variables one at a time if v is
            // deterministic
            StateDensityFunction diracDelta = new StateDensityFunction();
            diracDelta.addDeterministicVariable(v,
                    this.getDeterministicValue(v));
            return diracDelta;

        } else {
            StateDensityFunction marginalDensity = new StateDensityFunction(
                    this);

            for (Variable other : marginalDensity.getVariables())
                if (!other.equals(v))
                    marginalDensity.marginalizeVariable(other);

            return marginalDensity;
        }
    }

    public OmegaBigDecimal getMaxBound(Variable left, Variable right) {
        return getMaxBound(Collections.singleton(left),
                Collections.singleton(right));
    }

    public OmegaBigDecimal getMaxBound(Set<Variable> leftVars,
            Set<Variable> rightVars) {

        OmegaBigDecimal maxBound = OmegaBigDecimal.NEGATIVE_INFINITY;

        for (Variable left : leftVars) {
            OmegaBigDecimal leftCons = OmegaBigDecimal.ZERO;

            if (deterministicVariables.containsKey(left)) {
                leftCons = new OmegaBigDecimal(deterministicVariables.get(left));
                left = Variable.TSTAR;
            } else if (synchronizedVariables.containsKey(left)) {
                Synchronization s = synchronizedVariables.get(left);
                leftCons = new OmegaBigDecimal(s.getDelay());
                left = s.getDistributed();
            }

            for (Variable right : rightVars) {
                OmegaBigDecimal rightCons = OmegaBigDecimal.ZERO;

                if (deterministicVariables.containsKey(right)) {
                    rightCons = new OmegaBigDecimal(
                            deterministicVariables.get(right));
                    right = Variable.TSTAR;
                } else if (synchronizedVariables.containsKey(right)) {
                    Synchronization s = synchronizedVariables.get(right);
                    rightCons = new OmegaBigDecimal(s.getDelay());
                    right = s.getDistributed();
                }

                if (left.equals(right)) {
                    // left and right are both deterministic or both
                    // synchronized
                    // with the same: hence, their difference is a constant
                    OmegaBigDecimal newBound = leftCons.subtract(rightCons);
                    if (newBound.compareTo(maxBound) > 0)
                        maxBound = newBound;
                } else {
                    // checks the domain of every subzone (we are looking for
                    // the maximum bound,
                    // so the maximum bound among subzones is fine)
                    for (GEN f : partitionedGEN.getFunctions()) {
                        OmegaBigDecimal newBound = f.getDomain()
                                .getBound(left, right).add(leftCons)
                                .subtract(rightCons);
                        if (newBound.compareTo(maxBound) > 0)
                            maxBound = newBound;
                    }
                }
            }
        }

        return maxBound;
    }

    public OmegaBigDecimal getMinBound(Variable left, Variable right) {
        return getMinBound(Collections.singleton(left),
                Collections.singleton(right));
    }

    public OmegaBigDecimal getMinBound(Set<Variable> leftVars,
            Set<Variable> rightVars) {

        OmegaBigDecimal minBound = OmegaBigDecimal.POSITIVE_INFINITY;

        for (Variable left : leftVars) {
            OmegaBigDecimal leftCons = OmegaBigDecimal.ZERO;

            if (deterministicVariables.containsKey(left)) {
                leftCons = new OmegaBigDecimal(deterministicVariables.get(left));
                left = Variable.TSTAR;
            } else if (synchronizedVariables.containsKey(left)) {
                Synchronization s = synchronizedVariables.get(left);
                leftCons = new OmegaBigDecimal(s.getDelay());
                left = s.getDistributed();
            }

            for (Variable right : rightVars) {
                OmegaBigDecimal rightCons = OmegaBigDecimal.ZERO;

                if (deterministicVariables.containsKey(right)) {
                    rightCons = new OmegaBigDecimal(
                            deterministicVariables.get(right));
                    right = Variable.TSTAR;
                } else if (synchronizedVariables.containsKey(right)) {
                    Synchronization s = synchronizedVariables.get(right);
                    rightCons = new OmegaBigDecimal(s.getDelay());
                    right = s.getDistributed();
                }

                if (left.equals(right)) {
                    // left and right are both deterministic or both
                    // synchronized
                    // with the same: hence, their difference is a constant
                    OmegaBigDecimal newBound = leftCons.subtract(rightCons);
                    if (newBound.compareTo(minBound) < 0)
                        minBound = newBound;
                } else {
                    // checks the domain of every subzone to find the
                    // maximum allowed bound in the zone
                    OmegaBigDecimal zoneBound = OmegaBigDecimal.NEGATIVE_INFINITY;
                    for (GEN f : partitionedGEN.getFunctions()) {
                        OmegaBigDecimal subzoneBound = f.getDomain()
                                .getBound(left, right).add(leftCons)
                                .subtract(rightCons);
                        if (subzoneBound.compareTo(zoneBound) > 0)
                            zoneBound = subzoneBound;
                    }

                    if (zoneBound.compareTo(minBound) < 0)
                        minBound = zoneBound;
                }
            }
        }

        return minBound;
    }

    /*
     * Returns the probability of conditionement (zero if the resulting state
     * density has null measure)
     */
    public BigDecimal conditionToInterval(Variable v, OmegaBigDecimal x,
            OmegaBigDecimal y) {

        // impose that x <= v <= y
        this.imposeBound(v, Variable.TSTAR, y);
        this.imposeBound(Variable.TSTAR, v, x.negate());

        // empty partitionedGEN in case of unsatisfied deterministic constraints
        if (partitionedGEN.getFunctions().size() == 0)
            return BigDecimal.ZERO;

        // unitary partitionedGEN in case of satisfied deterministic constraints
        if (partitionedGEN.getFunctions().size() == 1
                && partitionedGEN.getFunctions().get(0).getDomain()
                        .getVariables().size() == 1)
            return BigDecimal.ONE;

        // Discard subzones with negligible measure, and
        // normalize the other ones by the /total/ measure of the class
        BigDecimal totalProbability = BigDecimal.ZERO;
        List<GEN> nonNullFunctions = new ArrayList<GEN>(partitionedGEN
                .getFunctions().size());

        for (GEN f : partitionedGEN.getFunctions()) {
            BigDecimal integralOverDomain = f.integrateOverDomain()
                    .bigDecimalValue();
            if (integralOverDomain.compareTo(new BigDecimal("0.0000001")) > 0) {
                // keeps the subzone (density to be conditioned)
                totalProbability = totalProbability.add(integralOverDomain);
                nonNullFunctions.add(f);
            }
        }

        // Condition every density by totalProbability
        // (the integral over the whole PartitionedGEN must be one)
        for (GEN f : nonNullFunctions)
            f.getDensity().divide(totalProbability);

        // Create a new partitioned GEN and sets it as the new state density
        partitionedGEN = new PartitionedGEN(nonNullFunctions);

        return totalProbability;
    }

    public PartitionedGEN getPartitionedGen() {
        return this.partitionedGEN;
    }

    public BigDecimal conditionToZone(DBMZone zone) {
        this.imposeZone(zone);
        if (partitionedGEN.getFunctions().size() == 0)
            return BigDecimal.ZERO;

        if (partitionedGEN.getFunctions().size() == 1
                && partitionedGEN.getFunctions().get(0).getDomain()
                        .getVariables().size() == 1) {
            return BigDecimal.ONE;

        }

        // Discard subzones with negligible measure, and
        // normalize the other ones by the /total/ measure of the class
        BigDecimal totalProbability = BigDecimal.ZERO;
        List<GEN> nonNullFunctions = new ArrayList<GEN>(partitionedGEN
                .getFunctions().size());

        for (GEN f : partitionedGEN.getFunctions()) {
            BigDecimal integralOverDomain = f.integrateOverDomain()
                    .bigDecimalValue();
            if (integralOverDomain.compareTo(new BigDecimal("0.0000001")) > 0) {
                // keeps the subzone (density to be conditioned)
                totalProbability = totalProbability.add(integralOverDomain);
                nonNullFunctions.add(f);
            }
        }

        // Condition every density by totalProbability
        // (the integral over the whole PartitionedGEN must be one)
        for (GEN f : nonNullFunctions)
            f.getDensity().divide(totalProbability);

        // Create a new partitioned GEN and sets it as the new state density
        partitionedGEN = new PartitionedGEN(nonNullFunctions);

        return totalProbability;
    }

    /*
     * Impose the bound without normalizing densities, keep only subzones with
     * non-null measure
     */
    public void imposeZone(DBMZone zone) {

        zone.normalize();
        Variable leftVar, rightVar;
        for (Variable left : zone.getVariables()) {
            OmegaBigDecimal leftCons = OmegaBigDecimal.ZERO;
            leftVar = (left == Variable.TSTAR) ? Variable.TSTAR : new Variable(
                    left.toString());
            if (deterministicVariables.containsKey(leftVar)) {
                leftCons = new OmegaBigDecimal(
                        deterministicVariables.get(leftVar));
                leftVar = Variable.TSTAR;

            } else if (synchronizedVariables.containsKey(leftVar)) {
                Synchronization s = synchronizedVariables.get(leftVar);
                leftCons = new OmegaBigDecimal(s.getDelay());
                leftVar = s.getDistributed();
            }
            for (Variable right : zone.getVariables()) {
                rightVar = (right == Variable.TSTAR) ? Variable.TSTAR
                        : new Variable(right.toString());
                if (right != left) {
                    OmegaBigDecimal bound = zone.getBound(left, right);
                    OmegaBigDecimal rightCons = OmegaBigDecimal.ZERO;
                    if (deterministicVariables.containsKey(rightVar)) {
                        rightCons = new OmegaBigDecimal(
                                deterministicVariables.get(rightVar));
                        rightVar = Variable.TSTAR;

                    } else if (synchronizedVariables.containsKey(rightVar)) {
                        Synchronization s = synchronizedVariables.get(rightVar);
                        rightCons = new OmegaBigDecimal(s.getDelay());
                        rightVar = s.getDistributed();
                    }
                    if (leftVar.equals(rightVar)) {
                        if (leftCons.subtract(rightCons).compareTo(bound) > 0) {
                            partitionedGEN = new PartitionedGEN();
                            return;
                        }
                    } else {
                        for (GEN f : partitionedGEN.getFunctions()) {
                            f.getDomain().imposeBound(leftVar, rightVar,
                                    bound.subtract(leftCons).add(rightCons));
                        }
                    }
                }
            }
        }
        List<GEN> nonNullFunctions = new ArrayList<GEN>(partitionedGEN
                .getFunctions().size());
        for (GEN f : partitionedGEN.getFunctions())
            if (f.getDomain().isFullDimensional())
                nonNullFunctions.add(f);

        partitionedGEN = new PartitionedGEN(nonNullFunctions);
    }

    public BigDecimal computeMeanValue(Variable v) {
        
        if (deterministicVariables.containsKey(v)) {
            return deterministicVariables.get(v);
            
        } else if (synchronizedVariables.containsKey(v)) {
            Synchronization s = synchronizedVariables.get(v);
            return computeMeanValue(s.getDistributed()).add(s.getDelay());
            
        } else {
            PartitionedGEN f = new PartitionedGEN(this.partitionedGEN);
            for (GEN g: f.getFunctions()) {
                Expolynomial meanArgument = Expolynomial.fromString(Variable.X.toString());
                meanArgument.substitute(Variable.X, v);
                g.getDensity().multiply(meanArgument);
            }
            
            return f.integrateOverDomain().bigDecimalValue();
        }
    }

}
