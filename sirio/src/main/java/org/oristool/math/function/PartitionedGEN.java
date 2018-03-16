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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;

/**
 * Multidimensional PDF on a piecewise DBM zone support.
 */
public class PartitionedGEN implements PartitionedFunction {

    private List<GEN> functions;

    /**
     * Creates an empty PDF.
     */
    public PartitionedGEN() {
        this.functions = Collections.emptyList();
    }

    /**
     * Creates a new piecewise PDF from a list of PDFs with DBM zone support.
     *
     * @param functions PDFs with DBM zone support
     */
    public PartitionedGEN(List<GEN> functions) {
        this.functions = functions;
    }

    /**
     * Creates a copy of the input piecewise PDF.
     *
     * @param partitionedGEN input piecewise PDF
     */
    public PartitionedGEN(PartitionedGEN partitionedGEN) {

        this.functions = new ArrayList<GEN>(partitionedGEN.functions.size());
        for (GEN f : partitionedGEN.functions)
            this.functions.add(new GEN(f));

    }

    /**
     * Creates an empty instance with integral equal to 1. This instance has empty
     * support and density equal to 1; it can be used as neutral element in products
     * with other PDFs.
     *
     * @return an empty PDF
     */
    public static PartitionedGEN newOneInstance() {
        List<GEN> GENs = new ArrayList<>();
        GENs.add(new GEN(new DBMZone(), Expolynomial.newOneInstance()));

        return new PartitionedGEN(GENs);
    }


    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof PartitionedGEN))
            return false;

        PartitionedGEN other = (PartitionedGEN) obj;

        if (this.getFunctions().size() != other.getFunctions().size())
            return false;

        return this.getFunctions().equals(other.getFunctions());
    }

    @Override
    public int hashCode() {
        return this.getFunctions().hashCode();
    }

    @Override
    public List<GEN> getFunctions() {
        return functions;
    }

    /**
     * Returns the product with another PDF, which is the product of densities over
     * the Cartesian product of supports.
     *
     * @param f input PDF
     * @return product PDF
     */
    public PartitionedGEN cartesianProduct(Function f) {
        List<GEN> newFunctions = new ArrayList<>();
        for (GEN g: functions)
            newFunctions.add(g.cartesianProduct(f));

        PartitionedGEN partitionedGEN = new PartitionedGEN(newFunctions);
        return partitionedGEN;
    }

    /**
     * Returns the product with another partitioned function, which is the product
     * of densities over the Cartesian product of supports.
     *
     * @param partitionedFunction input function
     * @return product PDF
     */
    public PartitionedGEN cartesianProduct(PartitionedFunction partitionedFunction) {
        List<GEN> newGEN = new ArrayList<>();
        for (GEN g: functions)
            for (Function f: partitionedFunction.getFunctions())
                newGEN.add(g.cartesianProduct(f));

        PartitionedGEN partitionedGEN = new PartitionedGEN(newGEN);
        return partitionedGEN;
    }

    /**
     * Imposes the bound {@code v >= min} and normalizes the density.
     *
     * @param v target variable
     * @param min minimum value
     * @return probability that the bound is satisfied (before normalization)
     */
    public BigDecimal conditionToMin(Variable v, OmegaBigDecimal min) {
        return conditionToBound(v, min, OmegaBigDecimal.POSITIVE_INFINITY);
    }

    /**
     * Imposes the bound {@code v <= max} and normalizes the density.
     *
     * @param v target variable
     * @param max maximum value
     * @return probability that the bound is satisfied (before normalization)
     */
    public BigDecimal conditionToMax(Variable v, OmegaBigDecimal max) {
        return conditionToBound(v, OmegaBigDecimal.NEGATIVE_INFINITY, max);
    }

    /**
     * Imposes the bound {@code min <= v <= max} and normalizes the density.
     *
     * @param v target variable
     * @param min minimum value
     * @param max maximum value
     * @return probability that the bound is satisfied (before normalization)
     */
    public BigDecimal conditionToBound(Variable v, OmegaBigDecimal min,
            OmegaBigDecimal max) {

        for (GEN f: functions) {
            f.getDomain().imposeBound(Variable.TSTAR, v, min.negate());
            f.getDomain().imposeBound(v, Variable.TSTAR, max);
        }

        BigDecimal totalProbability = BigDecimal.ZERO;
        List<GEN> nonNullFunctions = new ArrayList<>(functions.size());

        for (GEN f: functions) {
            BigDecimal integralOverDomain = f.integrateOverDomain().bigDecimalValue();
            if (integralOverDomain.compareTo(BigDecimal.ZERO) > 0) {
                totalProbability = totalProbability.add(integralOverDomain);
                nonNullFunctions.add(f);
            }
        }

        for (GEN f: nonNullFunctions)
            f.getDensity().divide(totalProbability);

        functions = nonNullFunctions;
        return totalProbability;
    }

    /**
     * Subtracts the input variable from all others and removes it.
     *
     * @param var target variable
     */
    public void shiftAndProject(Variable var) {

        List<GEN> allFunctions = new ArrayList<>();
        for (int i = 0; i < functions.size(); i++) {
            PartitionedGEN partitionedGEN = functions.get(i).shiftAndProject(var);
            allFunctions.addAll(partitionedGEN.getFunctions());
        }

        List<GEN> finalFunctions = new ArrayList<>();
        while (allFunctions.size() > 0) {
            GEN gen = allFunctions.get(0);

            GEN foundGen = null;
            for (int i = 1; i < allFunctions.size() && foundGen == null; i++) {
                DBMZone intersection = new DBMZone(gen.getDomain());
                intersection.intersect(allFunctions.get(i).getDomain());
                if (intersection.isFullDimensional())
                    foundGen = allFunctions.get(i);
            }

            if (foundGen == null) {
                finalFunctions.add(gen);
            } else {
                allFunctions.addAll(gen.getSubZonesInducted(foundGen));
                allFunctions.remove(foundGen);
            }

            allFunctions.remove(0);
        }

        functions = finalFunctions;
        if (functions.size() == 0) {
            functions.add(new GEN(new DBMZone(), Expolynomial.newOneInstance()));
        }
    }

    /**
     * Removes the input variable.
     *
     * @param var target variable
     */
    public void project(Variable var) {

        List<GEN> allFunctions = new ArrayList<>();
        for (int i = 0; i < functions.size(); i++) {
            PartitionedGEN partitionedGEN = functions.get(i).project(var);
            allFunctions.addAll(partitionedGEN.getFunctions());
        }

        List<GEN> finalFunctions = new ArrayList<>();
        while (allFunctions.size() > 0) {
            GEN gen = allFunctions.get(0);

            GEN foundGen = null;
            for (int i = 1; i < allFunctions.size() && foundGen == null; i++) {
                DBMZone intersection = new DBMZone(gen.getDomain());
                intersection.intersect(allFunctions.get(i).getDomain());
                if (intersection.isFullDimensional())
                    foundGen = allFunctions.get(i);
            }

            if (foundGen == null) {
                finalFunctions.add(gen);
            } else {
                allFunctions.addAll(gen.getSubZonesInducted(foundGen));
                allFunctions.remove(foundGen);
            }

            allFunctions.remove(0);
        }

        functions = finalFunctions;
        if (functions.size() == 0) {
            functions.add(new GEN(new DBMZone(), Expolynomial.newOneInstance()));
        }
    }

    /**
     * Subtracts a constant from all variables.
     *
     * @param constant input constant
     */
    public void constantShift(BigDecimal constant) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).constantShift(constant);
    }

    /**
     * Subtracts a constant from a set of variables.
     *
     * @param constant input constant
     * @param variables variables to be shifted
     */
    public void constantShift(BigDecimal constant, Collection<Variable> variables) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).constantShift(constant, variables);
    }

    /**
     * Replaces one variable name with another one.
     *
     * @param oldVar old variable name
     * @param newVar new variable name
     */
    public void substitute(Variable oldVar, Variable newVar) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).substitute(oldVar, newVar);
    }

    /**
     * Replaces one variable name with another one plus a constant.
     *
     * @param oldVar old variable name
     * @param newVar new variable name
     * @param constant constant to add
     */
    public void substitute(Variable oldVar, Variable newVar, BigDecimal constant) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).substitute(oldVar, newVar, constant);
    }

    /**
     * Replaces {@code oldVar} with {@code Variable.TSTAR} and makes
     * {@code newVar - constant} the new ground.
     *
     * @param oldVar old variable name
     * @param newVar new variable name
     * @param constant constant to subtract from newVar
     */
    public void substituteAndShift(Variable oldVar, Variable newVar, BigDecimal constant) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).substituteAndShift(oldVar, newVar, constant);
    }

    /**
     * Integrates this PDF over the support.
     *
     * <p>The result should be 1, unless the support has been restricted without
     * normalization.
     *
     * @return integral over the support
     */
    public OmegaBigDecimal integrateOverDomain() {

        if (functions.size() == 1
                && functions.get(0).getDomain().getVariables().size() == 1)
            return OmegaBigDecimal.ONE;

        OmegaBigDecimal integral = OmegaBigDecimal.ZERO;
        for (int i = 0; i < functions.size(); i++)
            integral = integral.add(functions.get(i).integrateOverDomain());

        return integral;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < functions.size(); i++) {
            b.append("Partitioned GEN[" + i + "]: ");
            b.append(functions.get(i).getDensity());
            b.append("\n");
            b.append(functions.get(i).getDomain());
        }

        return b.toString();
    }

    /**
     * Returns the list of variables of this PDF.
     *
     * @return list of variables (not including {@code Variable.TSTAR})
     */
    public Set<Variable> getVariables() {
        Set<Variable> variables = new LinkedHashSet<>(
                functions.get(0).getDomain().getVariables());
        variables.remove(Variable.TSTAR);

        return variables;
    }

    @Override
    public String toMathematicaString() {

        //example: Piecewise[{{[function:x^2],[range:0<x<1]}, {[function:x],[range:x>1]}}]

        StringBuilder b = new StringBuilder();
        String separator = "Piecewise[{";
        for (GEN g: functions) {
            b.append(separator);
            b.append(g.toMathematicaString());
            separator = ",";
        }
        b.append("}]");
        return b.toString();
    }
}
