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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;

public class PartitionedGEN implements PartitionedFunction {

    private List<GEN> functions;

    public PartitionedGEN(List<GEN> functions) {
        this.functions = functions;
    }

    public PartitionedGEN(GEN... functions) {
        this.functions = Arrays.asList(functions);
    }

    public PartitionedGEN(PartitionedGEN partitionedGEN) {

        this.functions = new ArrayList<GEN>(partitionedGEN.functions.size());
        for (GEN f : partitionedGEN.functions)
            this.functions.add(new GEN(f));

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
        int result = 17;

        result = 31 * result + this.getFunctions().hashCode();

        return result;
    }

    @Override
    public List<GEN> getFunctions() {
        return functions;
    }

    public void setFunctions(List<GEN> functions) {
        this.functions = functions;
    }

    /**
     * Prodotto cartesiano di una PartitionedGEN per una Function
     * 
     * @param f
     *            funzione di riferimento
     * @return una PartitionedGEN dove ogni GEN è ottenuta facendo il prodotto
     *         cartesiano per la funzione passata come riferimento
     * @throws DomainDefinitionException
     * @throws DomainException
     * @throws ExpressionException
     */
    public PartitionedGEN cartesianProduct(Function f) {
        List<GEN> newFunctions = new ArrayList<GEN>();
        for (GEN g: functions)
            newFunctions.add(g.cartesianProduct(f));

        PartitionedGEN partitionedGEN = new PartitionedGEN(newFunctions);
        return partitionedGEN;
    }

    /**
     * Prodotto cartesiano di una PartitionedGEN per una PartitionedFunction
     * 
     * @param f
     *            funzione di riferimento
     * @return una PartitionedGEN dove ogni GEN è ottenuta facendo il prodotto
     *         cartesiano per la funzione passata come riferimento
     * @throws DomainDefinitionException
     * @throws DomainException
     * @throws ExpressionException
     */
    public PartitionedGEN cartesianProduct(PartitionedFunction partitionedFunction) {
        List<GEN> newGEN = new ArrayList<GEN>();
        for (GEN g: functions)
            for (Function f: partitionedFunction.getFunctions())
                newGEN.add(g.cartesianProduct(f));

        PartitionedGEN partitionedGEN = new PartitionedGEN(newGEN);
        return partitionedGEN;
    }
    
    public void conditionToMin(Variable v, OmegaBigDecimal min) {
        conditionToBound(v, min, OmegaBigDecimal.POSITIVE_INFINITY);
    }

    public void conditionToMax(Variable v, OmegaBigDecimal max) {
        conditionToBound(v, OmegaBigDecimal.NEGATIVE_INFINITY, max);
    }

    public BigDecimal conditionToBound(Variable v, OmegaBigDecimal min,
            OmegaBigDecimal max) {

        for (GEN f: functions) {
            f.getDomain().imposeBound(Variable.TSTAR, v, min.negate());
            f.getDomain().imposeBound(v, Variable.TSTAR, max);
        }

        BigDecimal totalProbability = BigDecimal.ZERO;
        List<GEN> nonNullFunctions = new ArrayList<GEN>(functions.size());

        for (GEN f: functions) {
            BigDecimal integralOverDomain = f.integrateOverDomain().bigDecimalValue();
            if (integralOverDomain.compareTo(BigDecimal.ZERO) > 0) {
                totalProbability = totalProbability.add(integralOverDomain);
                nonNullFunctions.add(f);
            }
        }

        for (GEN f: nonNullFunctions)
            f.getDensity().divide(totalProbability);

        //Integral is zero because its duration is end. To avoid a division by zero error, PartitionedGEN is replaced by an IMM.
        if(totalProbability.compareTo(BigDecimal.ZERO) == 0){
            GEN immediate = GEN.getDETInstance(Variable.X, BigDecimal.ZERO);
            functions = new ArrayList<>();
            functions.add(immediate);
            return BigDecimal.ZERO;
        }else{
            functions = nonNullFunctions;
            return totalProbability;
        }
    }

    /**
     * Operazione di shift e proiezione
     * 
     * @param var
     *            variabile rispetto alla quale operare
     * @return PartitionedGEN risultante dell'operazione
     * @throws FunctionException
     * @throws DomainException
     * @throws ExpressionException
     */
    public void shiftAndProject(Variable var) {

        List<GEN> allFunctions = new ArrayList<GEN>();
        for (int i = 0; i < functions.size(); i++) {
            PartitionedGEN partitionedGEN = functions.get(i).shiftAndProject(
                    var);
            allFunctions.addAll(partitionedGEN.getFunctions());
        }

        List<GEN> finalFunctions = new ArrayList<GEN>();
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
            functions
                    .add(new GEN(new DBMZone(), Expolynomial.newOneInstance()));
        }
    }

    /**
     * Operazione di proiezione
     * 
     * @param var
     *            variabile rispetto alla quale operare
     * @return PartitionedGEN risultante dell'operazione
     * @throws FunctionException
     * @throws DomainException
     * @throws ExpressionException
     */
    public void project(Variable var) {

        List<GEN> allFunctions = new ArrayList<GEN>();
        for (int i = 0; i < functions.size(); i++) {
            PartitionedGEN partitionedGEN = functions.get(i).project(var);
            allFunctions.addAll(partitionedGEN.getFunctions());
        }

        List<GEN> finalFunctions = new ArrayList<GEN>();
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
            functions
                    .add(new GEN(new DBMZone(), Expolynomial.newOneInstance()));
        }
    }

    /**
     * Shift rispetto ad una costante
     * 
     * @param constant
     *            costante di riferimento
     * @return PartitionedGEN risultante dallo shift
     * @throws IllegalConstraintException
     * @throws DomainDefinitionException
     * @throws ExpressionException
     */
    public void constantShift(BigDecimal constant) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).constantShift(constant);
    }

    // Shift della partitioned gen solo delle progressing
    public void constantShift(BigDecimal constant, Collection<Variable> others) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).constantShift(constant, others);
    }

    /**
     * oldVar -> newVar
     */
    public void substitute(Variable oldVar, Variable newVar) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).substitute(oldVar, newVar);
    }

    /**
     * oldVar -> newVar + coefficient
     */
    public void substitute(Variable oldVar, Variable newVar,
            BigDecimal coefficient) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).substitute(oldVar, newVar, coefficient);
    }

    /**
     * oldVar -> newVar (in the domain for oldVar) var -> var + constant (in the
     * domain for all variables) oldVar -> - newVar + constant (in the density
     * for oldVar) otherVar -> otherVar - newVar + constant (in the density for
     * other variables)
     */
    public void substituteAndShift(Variable oldVar, Variable newVar,
            BigDecimal coefficient) {

        for (int c = 0; c < functions.size(); c++)
            functions.get(c).substituteAndShift(oldVar, newVar, coefficient);
    }

    /**
     * Integrale sul dominio partizionato della densit? piecewise
     * 
     * @return somma degli integrali sulle varie zone
     * @throws IntegrationException
     * @throws DomainDefinitionException
     * @throws DomainException
     * @throws ExpressionException
     */
    public OmegaBigDecimal integrateOverDomain() {

        // Return one in case of a deterministic-only state density function
        if (functions.size() == 1
                && functions.get(0).getDomain().getVariables().size() == 1)
            return OmegaBigDecimal.ONE;

        OmegaBigDecimal integral = OmegaBigDecimal.ZERO;
        for (int i = 0; i < functions.size(); i++)
            integral = integral.add(functions.get(i).integrateOverDomain());

        return integral;
    }

    /**
     * Confronto approssimato fra due PartitionedGEN. Esse sono uguali se le
     * zone coincidono, e la somma della norma delle differenze fra le densità
     * corrispondenti calcolata in modo discreto è sotto ad <code>epsilon</code>
     * 
     * @param other
     *            partizione di confronto
     * @param numSamples
     *            numero di campioni da prelevare per ogni dimensione
     * @param epsilon
     *            soglia di confronto
     * @return esito del confronto
     */
    public boolean equals(PartitionedGEN other, int numSamples,
            BigDecimal epsilon) {

        // TODO testare con costrutture per copia di PartitionedGEN
        // the discrete error norm would be null
        if (other == this)
            return true;

        if (functions.size() != other.getFunctions().size())
            return false;

        return false;
        // // total discrete error norm over all the partitions
        // BigDecimal totalDifference = BigDecimal.ZERO;
        //
        // // TODO Stabilire un ordinamento dei domini per evitare la ricerca
        // quadratica
        // // (magari mantenendo un hash dominio -> GEN con quel dominio, o un
        // tree con un comparator)
        // for(int i=0; i<functions.size(); i++){
        // boolean found = false;
        // for(int j=0; j<other.getFunctions().size() && !found; j++){
        // if(functions.get(i).getDomain().equals(other.getFunctions().get(j).getDomain()))
        // {
        // found = true;
        // Expolynomial difference = new
        // Expolynomial(functions.get(i).getDensity());
        // difference.sub(other.getFunctions().get(j).getDensity());
        // GEN f = new GEN(functions.get(i).getDomain(), difference);
        // totalDifference = totalDifference.add(f.discreteDNorm(numSamples));
        // }
        // }
        //
        // if (!found)
        // return false;
        // }
        //
        // return totalDifference.compareTo(epsilon) < 0;
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

    public static PartitionedGEN newOneInstance() {
        List<GEN> GENs = new ArrayList<GEN>();
        GENs.add(new GEN(new DBMZone(), Expolynomial.newOneInstance()));

        PartitionedGEN partitionedGEN = new PartitionedGEN(GENs);

        return partitionedGEN;
    }

    public Set<Variable> getVariables() {
        Set<Variable> variables = new LinkedHashSet<Variable>(functions.get(0)
                .getDomain().getVariables());
        variables.remove(Variable.TSTAR);

        return variables;
    }

    public BigDecimal evaluateMeanValue(int samples) {
        // BigDecimal width = BigDecimal.ZERO;
        BigDecimal meanValue = BigDecimal.ZERO;
        BigDecimal sumProbs = BigDecimal.ZERO;

        for (int i = 0; i < this.getFunctions().size(); i++) {
            GEN gen = this.getFunctions().get(i);
            if (gen.getDomain().getVariables().size() != 2)
                return BigDecimal.ZERO;

            Iterator<Variable> itor = gen.getDomain().getVariables().iterator();
            Variable var = itor.next();
            if (var.equals(Variable.TSTAR))
                var = itor.next();
            BigDecimal eft = gen.getDomain()
                    .getCoefficient(Variable.TSTAR, var).negate()
                    .bigDecimalValue();
            BigDecimal lft = gen.getDomain()
                    .getCoefficient(var, Variable.TSTAR).bigDecimalValue();
            BigDecimal step = lft.subtract(eft).divide(
                    new BigDecimal(samples - 1));

            for (BigDecimal point = eft; point.compareTo(lft) <= 0; point = point
                    .add(step)) {
                HashMap<Variable, OmegaBigDecimal> hm = new HashMap<Variable, OmegaBigDecimal>();
                hm.put(var, new OmegaBigDecimal(point));
                BigDecimal prob = gen.getDensity().evaluate(hm)
                        .bigDecimalValue();
                BigDecimal addend = point.multiply(prob);
                if (point.equals(eft) == false && point.equals(lft) == false) {
                    meanValue = meanValue.add(addend);
                    sumProbs = sumProbs.add(prob);
                } else {
                    meanValue = meanValue.add(addend.divide(new BigDecimal(2),
                            Expolynomial.mathContext));
                    sumProbs = sumProbs.add(prob.divide(new BigDecimal(2),
                            Expolynomial.mathContext));
                }
            }
        }
        meanValue = meanValue.divide(sumProbs, Expolynomial.mathContext);
        return meanValue;
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
