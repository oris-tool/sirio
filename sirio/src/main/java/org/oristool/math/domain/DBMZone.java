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

package org.oristool.math.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Variable;

/**
 * A DBM zone encoding pairwise constraints between variables.
 */
public final class DBMZone {
    private OmegaBigDecimal[][] matrix;
    private Map<Variable, Integer> index;
    private boolean isNormalized;
    private boolean isDiagonalNormalized;

    public DBMZone() {
        this(Collections.<Variable>emptySet());
    }

    public DBMZone(Variable... vars) {
        this(Arrays.asList(vars));
    }

    /**
     * Creates a zone for the given variables, without constraints.
     *
     * @param vars zone variables
     */
    public DBMZone(Collection<Variable> vars) {

        isNormalized = true;
        isDiagonalNormalized = true;

        matrix = new OmegaBigDecimal[vars.size() + 1][vars.size() + 1];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix.length; j++)
                matrix[i][j] = OmegaBigDecimal.POSITIVE_INFINITY;

        index = new HashMap<Variable, Integer>();
        index.put(Variable.TSTAR, 0);
        int i = 1;
        for (Variable v : vars) {
            index.put(v, i);
            i++;
        }
    }

    /**
     * Creates a copy of a DBM zone.
     *
     * @param dbm input zone
     */
    public DBMZone(DBMZone dbm) {

        isNormalized = dbm.isNormalized;
        isDiagonalNormalized = dbm.isDiagonalNormalized;

        matrix = new OmegaBigDecimal[dbm.matrix.length][dbm.matrix.length];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix.length; j++)
                matrix[i][j] = dbm.matrix[i][j];

        index = new HashMap<Variable, Integer>(dbm.index);
    }

    /**
     * Returns the constant {@code c} for the constraint {@code left-right <= c}.
     *
     * <p>Use {@code getBound} if the DBM is not normalized.
     *
     * @param left first variable
     * @param right second variable
     * @return upper bound
     */
    public OmegaBigDecimal getCoefficient(Variable left, Variable right) {
        return matrix[index.get(left)][index.get(right)];
    }

    /**
     * Returns the upper bound for the difference {@code left-right} between two
     * variables, which is the constant {@code c} of a constraint
     * {@code left-right <= c}.
     *
     * @param left first variable
     * @param right second variable
     * @return upper bound
     */
    public OmegaBigDecimal getBound(Variable left, Variable right) {
        this.normalize();
        return matrix[index.get(left)][index.get(right)];
    }

    /**
     * Returns the minimum value that any of the input variables can take inside
     * this zone.
     *
     * @param vars input variables
     * @return minimum allowed value for the input variables
     */
    public OmegaBigDecimal getMinLFT(Set<Variable> vars) {

        OmegaBigDecimal minLFT = OmegaBigDecimal.POSITIVE_INFINITY;

        for (Variable v : vars)
            if (!v.equals(Variable.TSTAR)
                    && this.getBound(v, Variable.TSTAR).compareTo(minLFT) < 0)
                minLFT = this.getBound(v, Variable.TSTAR);

        return minLFT;
    }

    /**
     * Sets the constant {@code c} for the constraints {@code left-right}.
     *
     * @param left first variable
     * @param right second variable
     * @param coefficient upper bound for {@code left-right}
     * @throws IllegalArgumentException if {@code left==right} or
     *         {@code coefficient} is negative infinity
     */
    public void setCoefficient(Variable left, Variable right,
            OmegaBigDecimal coefficient) {

        if (coefficient.equals(OmegaBigDecimal.NEGATIVE_INFINITY))
            throw new IllegalArgumentException("-inf is not an allowed value");

        if (left == right)
            throw new IllegalArgumentException(
                    "Two distinct variables must be specified");

        matrix[index.get(left)][index.get(right)] = coefficient;
        isNormalized = false;
        isDiagonalNormalized = false;
    }

    public void addVariables(Variable... vars) {
        this.addVariables(Arrays.asList(vars));
    }

    /** Introduces new variables in this zone.
     *
     * @param vars input variables
     */
    public void addVariables(Collection<Variable> vars) {

        if (vars.size() == 0)
            return;

        // index update
        for (Variable v : vars)
            if (index.keySet().contains(v))
                throw new IllegalArgumentException("Variable " + v
                        + " already present in the DBM zone");

        int k = matrix.length;
        for (Variable v : vars) {
            index.put(v, k);
            k++;
        }

        // matrix update
        OmegaBigDecimal[][] m = new OmegaBigDecimal[matrix.length + vars.size()][matrix.length
                + vars.size()];

        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m.length; j++)
                if (i < matrix.length && j < matrix.length)
                    m[i][j] = matrix[i][j];
                else
                    m[i][j] = OmegaBigDecimal.POSITIVE_INFINITY;

        this.matrix = m;
    }

    public Set<Variable> getVariables() {

        return Collections.unmodifiableSet(index.keySet());
    }

    /**
     * Normalizes this zone, so that upper bounds satisfy the triangular inequality.
     */
    public void normalize() {

        if (isNormalized)
            return;

        // sets diagonal coefficients to +infinity
        for (int i = 0; i < matrix.length; i++)
            matrix[i][i] = OmegaBigDecimal.POSITIVE_INFINITY;

        for (int k = 0; k < matrix.length; k++)
            for (int i = 0; i < matrix.length; i++)
                for (int j = 0; j < matrix.length; j++) {
                    // ruling out the case k==i || k==j to avoid the
                    // propagation of negative diagonal elements
                    if (k != i && k != j) {
                        OmegaBigDecimal ikkj = matrix[i][k].add(matrix[k][j]);
                        if (ikkj.compareTo(matrix[i][j]) < 0)
                            matrix[i][j] = ikkj;
                    }
                }

        isNormalized = true;
        isDiagonalNormalized = true;
    }

    private void normalizeDiagonal() {

        if (isDiagonalNormalized)
            return;

        if (!isNormalized)
            this.normalize();
        else {
            // the matrix is normalized but the diagonal is not,
            // i.e. the diagonal coefficients must be recomputed

            // sets diagonal coefficients to +infinity
            for (int i = 0; i < matrix.length; i++)
                matrix[i][i] = OmegaBigDecimal.POSITIVE_INFINITY;

            // sets matrix[i][i] = min_{j!=i} matrix[i][j]+matrix[j][i]
            for (int i = 0; i < matrix.length; i++)
                for (int j = 0; j < matrix.length; j++)
                    if (j != i) {
                        OmegaBigDecimal ijji = matrix[i][j].add(matrix[j][i]);
                        if (ijji.compareTo(matrix[i][i]) < 0)
                            matrix[i][i] = ijji;
                    }

            isDiagonalNormalized = true;
        }
    }

    /**
     * Computes the Cartesian product with another zone (with distinct set of
     * variables).
     *
     * @param other another zone
     * @return the Cartesian product with this zone
     */
    public DBMZone cartesianProduct(DBMZone other) {

        // check for disjoint variable sets
        for (Variable v : other.index.keySet())
            if (index.keySet().contains(v))
                if (!v.equals(Variable.TSTAR))
                    throw new IllegalArgumentException("Variable " + v
                            + " already present in the DBM zone");

        DBMZone product = new DBMZone();
        product.matrix = new OmegaBigDecimal[this.matrix.length
                + other.matrix.length - 1][this.matrix.length
                + other.matrix.length - 1];

        int otherTStarPos = other.index.get(Variable.TSTAR);
        int thisTStarPos = this.index.get(Variable.TSTAR);

        // adds variables and coefficients of this dbm
        product.index.putAll(this.index);

        // adds variables and coefficients of the other dbm (in order, excluding
        // tstar)
        for (Entry<Variable, Integer> e : other.index.entrySet())
            if (!e.getKey().equals(Variable.TSTAR))
                if (e.getValue() < otherTStarPos)
                    product.index.put(e.getKey(),
                            this.matrix.length + e.getValue());
                else
                    product.index.put(e.getKey(),
                            this.matrix.length + e.getValue() - 1);

        // copying coefficients, setting non-diagonal blocks to a TSTAR one-step
        // update
        for (int i = 0; i < product.matrix.length; i++)
            for (int j = 0; j < product.matrix.length; j++)
                if (i < this.matrix.length && j < this.matrix.length)
                    // tstar and variables from this matrix
                    product.matrix[i][j] = this.matrix[i][j];
                else if (i >= this.matrix.length && j >= this.matrix.length)
                    // variables from the other matrix (excluding the tstar)
                    product.matrix[i][j] = other.matrix[i - this.matrix.length
                            + (i < otherTStarPos ? 0 : 1)][j
                            - this.matrix.length + (j < otherTStarPos ? 0 : 1)];
                else if (i == thisTStarPos && j >= this.matrix.length)
                    // ground row from the other matrix
                    product.matrix[i][j] = other.matrix[otherTStarPos][j
                            - this.matrix.length + (j < otherTStarPos ? 0 : 1)];
                else if (j == thisTStarPos && i >= this.matrix.length)
                    // ground column from the other matrix
                    product.matrix[i][j] = other.matrix[i - this.matrix.length
                            + (i < otherTStarPos ? 0 : 1)][otherTStarPos];
                else if (i < j)
                    // i is a node from this matrix, j from the other one
                    product.matrix[i][j] = this.matrix[i][thisTStarPos]
                            .add(other.matrix[otherTStarPos][j
                                    - this.matrix.length
                                    + (j < otherTStarPos ? 0 : 1)]);
                else if (i > j)
                    // i is a node from the other matrix, j from the other one
                    product.matrix[i][j] = other.matrix[i - this.matrix.length
                            + (i < otherTStarPos ? 0 : 1)][otherTStarPos]
                            .add(this.matrix[thisTStarPos][j]);

        // In case of empty DBMs, negative cycles would result in new negative
        // cycles through the ground in the Cartesian product
        product.isNormalized = false;
        product.isDiagonalNormalized = false;

        return product;
    }

    /**
     * Checks if this zone is fully-dimensional in its variable space.
     *
     * @return true if the zone is fully dimensional, false otherwise
     */
    public boolean isFullDimensional() {

        if (index.size() == 1)
            return true;

        this.normalizeDiagonal();

        for (int i = 0; i < matrix.length; i++)
            // toRightNeighborhood() for NSTA models
            if (matrix[i][i].compareTo(OmegaBigDecimal.ZERO
                    .toRightNeighborhood()) <= 0)
                return false;

        return true;
    }

    /**
     * Checks if this zone has unsatisfiable constraints.
     *
     * @return true if the zone is empty, false otherwise
     */
    public boolean isEmpty() {

        if (index.size() == 1)
            return false;

        this.normalizeDiagonal();

        for (int i = 0; i < matrix.length; i++)
            if (matrix[i][i].compareTo(OmegaBigDecimal.ZERO) < 0)
                return true;

        return false;
    }

    /**
     * Imposes an upper bound for the difference {@code left-right}. If a stricter
     * constraint already exists, no change is applied.
     *
     * @param left first variable
     * @param right second variable
     * @param coefficient upper bound
     */
    public void imposeBound(Variable left, Variable right,
            OmegaBigDecimal coefficient) {

        if (coefficient.equals(OmegaBigDecimal.NEGATIVE_INFINITY))
            throw new IllegalArgumentException("-inf is not an allowed value");

        if (left.equals(right))
            throw new IllegalArgumentException(
                    "Two distinct variables must be specified");

        int i = index.get(left);
        int j = index.get(right);
        if (this.matrix[i][j].compareTo(coefficient) > 0) {
            matrix[i][j] = coefficient;
            isNormalized = false;
            isDiagonalNormalized = false;
        }
    }

    /**
     * Imposes the bound {@code var-x <= 0} for each x in {@code others}.
     *
     * @param var a variable
     * @param others a set of variables
     */
    public void imposeVarLower(Variable var, Collection<Variable> others) {

        for (Variable v : others) {
            int i = index.get(var);
            int j = index.get(v);

            if (this.matrix[i][j].compareTo(OmegaBigDecimal.ZERO) > 0) {
                this.matrix[i][j] = OmegaBigDecimal.ZERO;
                isNormalized = false;
                isDiagonalNormalized = false;
            }
        }

    }

    /**
     * Imposes the bound {@code var-x <= 0} for each x in {@code others}. This
     * method may result in a faster computation time if normalization is required.
     *
     * @param var a variable
     * @param others a set of variables
     */
    public void imposeVarLowerAndNormalize(Variable var,
            Collection<Variable> others) {

        // TODO implement O(n^2) normalization
        this.imposeVarLower(var, others);
    }

    /**
     * Checks if a variable can be the minimum in the zone.
     *
     * @param var a variable
     * @return true if the variable can be the minimum
     */
    public boolean canVariableBeLowestOrEqual(Variable var) {

        this.normalize();

        Integer pos = index.get(var);
        if (pos == null)
            throw new IllegalArgumentException("Unknown variable " + var);

        int j = pos;
        int starPos = index.get(Variable.TSTAR);

        for (int i = 0; i < matrix.length; i++)
            if (i != starPos && i != j
                    && matrix[i][j].compareTo(OmegaBigDecimal.ZERO) < 0)
                return false;

        return true;
    }

    /**
     * Checks if a variable can be the minimum among a set of variables.
     *
     * @param var a variable
     * @param others a set of variables
     * @return true if the variable can be the minimum
     */
    public boolean canVariableBeLowerOrEqual(Variable var,
            Collection<Variable> others) {

        this.normalize();

        Integer pos = index.get(var);
        if (pos == null || !this.getVariables().containsAll(others))
            throw new IllegalArgumentException("Unknown variable");

        int j = pos;

        for (Variable v : others)
            if (matrix[index.get(v)][j].compareTo(OmegaBigDecimal.ZERO) < 0)
                return false;

        return true;
    }

    /**
     * Discards the constraints of the input variable and uses it as the new ground.
     *
     * @param v a variable
     */
    public void setNewGround(Variable v) {

        this.project(Variable.TSTAR);
        this.substitute(v, Variable.TSTAR);
    }

    /**
     * Discards the constraints of the input variable.
     *
     * @param v a variable
     */
    public void projectVariable(Variable v) {

        if (v.equals(Variable.TSTAR))
            throw new IllegalArgumentException(
                    "The ground variable cannobe be projected");

        this.normalize();
        this.project(v);
    }

    private void project(Variable var) {

        this.normalize();

        // finds the position of the variable to be projected
        Integer pos = index.get(var);
        if (pos == null)
            throw new IllegalArgumentException("Variable " + var
                    + " not present");
        int k = pos;

        // matrix update
        OmegaBigDecimal[][] m = new OmegaBigDecimal[matrix.length - 1][matrix.length - 1];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m.length; j++)
                m[i][j] = matrix[i < k ? i : i + 1][j < k ? j : j + 1];

        this.matrix = m;

        // index update
        index.remove(var);
        for (Entry<Variable, Integer> e : index.entrySet())
            index.put(e.getKey(),
                    e.getValue() < k ? e.getValue() : e.getValue() - 1);

        // normalization is preserved but variable projection
        // can alter emptiness and full-dimensionality
        isDiagonalNormalized = false;
    }

    public void projectVariables(Variable... vars) {
        this.projectVariables(Arrays.asList(vars));
    }

    /**
     * Discards the constraints of a set of variables.
     *
     * @param vars a set of variables
     */
    public void projectVariables(Collection<Variable> vars) {

        if (vars.size() == 0)
            return;

        this.normalize();

        // finds the positions of variables to be projected
        int[] positions = new int[vars.size() + 2];

        // positions always include the boundaries (-1) and (matrix.length)
        positions[0] = -1;
        positions[positions.length - 1] = matrix.length;

        int k = 1;
        for (Variable v : vars) {
            if (v.equals(Variable.TSTAR))
                throw new IllegalArgumentException(
                        "The ground variable cannot be be projected");

            Integer p = index.get(v);
            if (p == null)
                throw new IllegalArgumentException("Variable " + v
                        + " not present");

            positions[k] = p;
            k++;
        }
        Arrays.sort(positions);

        // matrix update
        OmegaBigDecimal[][] m = new OmegaBigDecimal[matrix.length - vars.size()][matrix.length
                - vars.size()];

        for (int k1 = 0; k1 <= positions.length - 2; k1++)
            // copies the elements between the k1-th and (k1+1)-th rows
            // (excluded)
            for (int i = positions[k1] + 1; i < positions[k1 + 1]; i++)
                for (int k2 = 0; k2 <= positions.length - 2; k2++)
                    // copies the elements between the k1-th and (k1+1)-th
                    // columns (excluded)
                    for (int j = positions[k2] + 1; j < positions[k2 + 1]; j++)
                        m[i - k1][j - k2] = matrix[i][j];

        this.matrix = m;

        // index update
        for (Variable v : vars)
            index.remove(v);

        // binarySearch returns -(index of the first greater element)-1
        for (Entry<Variable, Integer> e : index.entrySet())
            index.put(e.getKey(),
                    e.getValue() + Arrays.binarySearch(positions, e.getValue())
                            + 2);

        // normalization is preserved but variable projection
        // can alter emptiness and full-dimensionality
        isDiagonalNormalized = false;
    }

    /**
     * Replaces the name of a variable in all of its constraints.
     *
     * @param oldVar the old name
     * @param newVar the new name
     */
    public void substitute(Variable oldVar, Variable newVar) {

        if (!oldVar.equals(newVar)) {
            if (index.get(newVar) != null)
                throw new IllegalArgumentException("The new variable " + newVar
                        + " is already present");

            Integer pos = index.remove(oldVar);
            if (pos == null)
                throw new IllegalArgumentException("The old variable " + oldVar
                        + " is not present");

            index.put(newVar, pos);
        }
    }

    /**
     * Replaces the name of a variable and applies a constant shift.
     *
     * @param oldVar the old name
     * @param newVar the new name
     * @param coefficient shift value
     */
    public void substitute(Variable oldVar, Variable newVar,
            BigDecimal coefficient) {

        this.substitute(oldVar, newVar);

        int i = index.get(newVar);
        for (int j = 0; j < matrix.length; j++) {
            matrix[i][j] = matrix[i][j].subtract(new OmegaBigDecimal(
                    coefficient));
            matrix[j][i] = matrix[j][i].add(new OmegaBigDecimal(coefficient));
        }

        isNormalized = false;
        isDiagonalNormalized = false;
    }

    /**
     * Applies a constant shift to all variables.
     *
     * @param constant shift value
     */
    public void constantShift(BigDecimal constant) {

        this.substitute(Variable.TSTAR, Variable.TSTAR, constant.negate());
    }

    /**
     * Applies a constant shift to a set of variables.
     *
     * @param constant shift amount
     * @param variables target variables
     */
    public void constantShift(BigDecimal constant,
            Collection<Variable> variables) {

        if (!index.keySet().containsAll(variables))
            throw new IllegalArgumentException(
                    "The shifted variables must be contained in the variables set");

        if (variables.contains(Variable.TSTAR))
            throw new IllegalArgumentException(
                    "The ground variable must always be suspended");

        List<Integer> progressingIndexes = new LinkedList<Integer>();
        List<Integer> suspendedIndexes = new LinkedList<Integer>();

        for (Variable v : index.keySet())
            if (variables.contains(v))
                progressingIndexes.add(index.get(v));
            else
                suspendedIndexes.add(index.get(v));

        for (Integer i : progressingIndexes) {
            for (Integer j : suspendedIndexes) {
                matrix[i][j] = matrix[i][j].subtract(new OmegaBigDecimal(
                        constant));
                matrix[j][i] = matrix[j][i].add(new OmegaBigDecimal(constant));
            }
        }

        isNormalized = false;
        isDiagonalNormalized = false;
    }

    public boolean isSyncronized(Variable x, Variable y) {
        return this.getBound(x, y).add(this.getBound(y, x))
                .compareTo(OmegaBigDecimal.ZERO) == 0;
    }

    /**
     * Finds the variables synchronized with the input one.
     *
     * @param v input variable
     * @return set of synchronized variables
     */
    public Set<Variable> getNullDelayVariables(Variable v) {

        Set<Variable> nullDelayVariables = new LinkedHashSet<Variable>();

        for (Variable o : index.keySet())
            if (!o.equals(v)
                    && this.getBound(v, o).compareTo(OmegaBigDecimal.ZERO) == 0
                    && this.getBound(o, v).compareTo(OmegaBigDecimal.ZERO) == 0)

                nullDelayVariables.add(o);

        return nullDelayVariables;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof DBMZone))
            return false;

        DBMZone other = (DBMZone) obj;

        // fast size check
        if (this.matrix.length != other.matrix.length)
            return false;

        // checks that the DBMs are defined over the same variables
        if (!this.index.keySet().equals(other.index.keySet()))
            return false;

        // computes the permutation from this variable ordering to other one
        int[] p = new int[matrix.length];

        for (Entry<Variable, Integer> e : this.index.entrySet())
            p[e.getValue()] = other.index.get(e.getKey());

        // normalize both
        this.normalize();
        other.normalize();

        // checks that the variables have non-diagonal constraints with the same
        // coefficients
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix.length; j++)
                // Coefficients must be equal just in value, not scale (i.e. 2.0
                // and 2.00 are equal)!
                if (i != j
                        && this.matrix[i][j]
                                .compareTo(other.matrix[p[i]][p[j]]) != 0)
                    return false;

        return true;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.index.keySet().hashCode();

        // computes the permutation from this variable ordering to lexicographic
        // one
        List<Variable> lexOrder = new ArrayList<Variable>(this.index.keySet());
        Collections.sort(lexOrder);

        int[] p = new int[matrix.length];
        for (int i = 0; i < lexOrder.size(); i++)
            p[i] = index.get(lexOrder.get(i));

        // normalize
        this.normalize();

        // hash in all the elements, in lexicographic order
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix.length; j++)
                if (i != j)
                    // OmegaBigDecimal objects that are numerically equal but
                    // differ in scale
                    // (like 2.0 and 2.00) have the same hash code (this is
                    // different wrt BigDecimal)
                    result = 31 * result + this.matrix[p[i]][p[j]].hashCode();

        // TODO consider caching this value (and deleting it when the object is
        // modified)
        return result;
    }

    /**
     * Intersects this zone with the input one.
     *
     * @param other a zone
     */
    public void intersect(DBMZone other) {

        if (this.equals(other))
            return;

        this.normalize();
        other.normalize();

        // checks that the DBMs are defined over the same variables
        if (!this.index.keySet().equals(other.index.keySet()))
            throw new IllegalArgumentException(
                    "The DBMs are defined over different varible sets");

        // computes the permutation from this variable ordering to other one
        int[] p = new int[matrix.length];

        for (Entry<Variable, Integer> e : this.index.entrySet())
            p[e.getValue()] = other.index.get(e.getKey());

        // tightens constraints in this DBM that are higher than those in the
        // other one
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix.length; j++)
                if (this.matrix[i][j].compareTo(other.matrix[p[i]][p[j]]) > 0) {
                    this.matrix[i][j] = other.matrix[p[i]][p[j]];
                    isNormalized = false;
                    isDiagonalNormalized = false;
                }
    }

    /**
     * Checks whether this zone fully contains an input one.
     *
     * @param other a zone
     * @return true if the input zone is fully contained
     */
    public boolean contains(DBMZone other) {

        if (this.equals(other))
            return true;

        this.normalize();
        other.normalize();

        // checks that the DBMs are defined over the same variables
        if (!this.index.keySet().equals(other.index.keySet()))
            throw new IllegalArgumentException(
                    "The DBMs are defined over different varible sets");

        // computes the permutation from this variable ordering to other one
        int[] p = new int[matrix.length];

        for (Entry<Variable, Integer> e : this.index.entrySet())
            p[e.getValue()] = other.index.get(e.getKey());

        // checks that contraints in this DBM are not lower than those in the
        // other one
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix.length; j++)
                if (this.matrix[i][j].compareTo(other.matrix[p[i]][p[j]]) < 0)
                    return false;

        return true;
    }

    /**
     * Checks if this zone contains the input point.
     *
     * @param point a point
     * @return true if this zone contains the point
     */
    public boolean contains(Map<Variable, OmegaBigDecimal> point) {

        this.normalize();

        if (point.keySet().size() != (index.keySet().size() - 1)
                && index.keySet().stream()
                .allMatch(v -> v != Variable.TSTAR || point.containsKey(v)))
            throw new IllegalArgumentException(
                    "A point must specify a value for each variable");

        for (Entry<Variable, Integer> ei : this.index.entrySet()) {
            for (Entry<Variable, Integer> ej : this.index.entrySet()) {
                if (!ei.getKey().equals(ej.getKey())) {
                    if (ej.getKey().equals(Variable.TSTAR)) {
                        // t_i - t_* <= b_i*
                        if (point.get(ei.getKey()).compareTo(
                                matrix[ei.getValue()][ej.getValue()]) > 0)
                            return false;
                    } else if (ei.getKey().equals(Variable.TSTAR)) {
                        // t_* - t_j <= b_*j
                        if (point
                                .get(ej.getKey())
                                .negate()
                                .compareTo(matrix[ei.getValue()][ej.getValue()]) > 0)
                            return false;
                    } else {
                        // t_i - t_j <= b_ij
                        if (point
                                .get(ei.getKey())
                                .subtract(point.get(ej.getKey()))
                                .compareTo(matrix[ei.getValue()][ej.getValue()]) > 0)
                            return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();
        List<Entry<Variable, Integer>> l = new ArrayList<Entry<Variable, Integer>>(
                this.index.entrySet());

        for (int i = 0; i < l.size(); i++)
            for (int j = 0; j < i; j++) {

                OmegaBigDecimal bij = matrix[l.get(i).getValue()][l.get(j)
                        .getValue()];
                OmegaBigDecimal bji = matrix[l.get(j).getValue()][l.get(i)
                        .getValue()];

                if (!bji.equals(OmegaBigDecimal.POSITIVE_INFINITY)
                        || !bij.equals(OmegaBigDecimal.POSITIVE_INFINITY)) {
                    if (!l.get(i).getKey().equals(Variable.TSTAR)) {
                        if (!bji.equals(OmegaBigDecimal.POSITIVE_INFINITY)) {
                            b.append(bji.negate());
                            b.append(" <= ");
                        }
                    } else {
                        if (!bij.equals(OmegaBigDecimal.POSITIVE_INFINITY)) {
                            b.append(bij.negate());
                            b.append(" <= ");
                        }
                    }

                    if (!l.get(i).getKey().equals(Variable.TSTAR))
                        b.append(l.get(i).getKey());

                    if (!l.get(j).getKey().equals(Variable.TSTAR)) {
                        if (!l.get(i).getKey().equals(Variable.TSTAR))
                            b.append(" - ");
                        b.append(l.get(j).getKey());
                    }

                    if (!l.get(i).getKey().equals(Variable.TSTAR)) {
                        if (!bij.equals(OmegaBigDecimal.POSITIVE_INFINITY)) {
                            b.append(" <= ");
                            b.append(bij);
                        }
                    } else {
                        if (!bji.equals(OmegaBigDecimal.POSITIVE_INFINITY)) {
                            b.append(" <= ");
                            b.append(bji);
                        }
                    }

                    b.append("\n");
                }
            }

        return b.toString();
    }

    /**
     * Produces a representation of the zone constraints as "and" {@code &&} operators.
     *
     * @return string representation of the zone as and operators
     */
    public String toAndString() {
        StringBuilder b = new StringBuilder();

        boolean first = true;
        for (Entry<Variable, Integer> ei : this.index.entrySet()) {
            for (Entry<Variable, Integer> ej : this.index.entrySet()) {
                if (!ei.getKey().equals(ej.getKey())) {
                    if (!first)
                        b.append(" && ");
                    else
                        first = false;

                    if (ej.getKey().equals(Variable.TSTAR))
                        b.append(ei.getKey() + " <= "
                                + matrix[ei.getValue()][ej.getValue()]);
                    else if (ei.getKey().equals(Variable.TSTAR))
                        b.append(ej.getKey() + " >= "
                                + matrix[ei.getValue()][ej.getValue()].negate());
                    else
                        b.append(ei.getKey() + "-" + ej.getKey() + " <= "
                                + matrix[ei.getValue()][ej.getValue()]);
                }
            }
        }

        return b.toString();
    }

    /**
     * Produces a representation of the zone constraints Mathematica {@code RegionPlot}.
     *
     * @return string representation of the zone as region plots
     */
    public String toRegionPlotString() {

        StringBuilder b = new StringBuilder();

        b.append("RegionPlot[{");
        b.append(this.toAndString());
        b.append("}");

        for (Variable v : index.keySet())
            if (!v.equals(Variable.TSTAR))
                b.append(", {" + v + ", "
                        + getBound(Variable.TSTAR, v).negate() + ", "
                        + getBound(v, Variable.TSTAR) + "}");

        b.append("]");

        return b.toString();
    }

    public String toUnitStepsString() {

        return null;
    }

    /**
     * A subzone resulting from a projection.
     */
    public static final class Subzone {
        DBMZone domain;
        Variable projectedVar;
        Variable minVar;
        OmegaBigDecimal minVarDelay;
        Variable maxVar;
        OmegaBigDecimal maxVarAdvance;

        /**
         * Creates a representation of a subzone resulting from a projection.
         *
         * @param domain the original zone
         * @param projectedVar the variable that was projected
         * @param minVar the variable attaining the minimum
         * @param minVarDelay the delay of the variable attaining the minimum
         * @param maxVar the variable attaining the maximum
         * @param maxVarAdvance the advance of the variable attaining the maximum
         */
        public Subzone(DBMZone domain, Variable projectedVar, Variable minVar,
                OmegaBigDecimal minVarDelay, Variable maxVar,
                OmegaBigDecimal maxVarAdvance) {
            this.domain = domain;
            this.projectedVar = projectedVar;
            this.minVar = minVar;
            this.minVarDelay = minVarDelay;
            this.maxVar = maxVar;
            this.maxVarAdvance = maxVarAdvance;
        }

        public DBMZone getDomain() {
            return domain;
        }

        public Variable getProjectedVar() {
            return projectedVar;
        }

        public Variable getMinVar() {
            return minVar;
        }

        public OmegaBigDecimal getMinVarDelay() {
            return minVarDelay;
        }

        public Variable getMaxVar() {
            return maxVar;
        }

        public OmegaBigDecimal getMaxVarAdvance() {
            return maxVarAdvance;
        }
    }
}
