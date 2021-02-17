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

package org.oristool.models.stpn;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;

/**
 * Transient probabilities from many initial states.
 *
 * @param <R> type of initial states (such as regenerations)
 * @param <S> type of reachable states (such as markings)
 */
public class TransientSolution<R, S> {
    private BigDecimal timeLimit;
    private BigDecimal step;
    int samplesNumber;
    R initialRegeneration;

    List<R> regenerations;
    List<S> columnStates;

    double[][][] solution;

    private TransientSolution() {

    }

    /**
     * Prepares a new instance from a given array.
     *
     * @param <S> type of reachable states (such as markings)
     * @param probs array of transient probabilities for each time/state
     * @param step time step between solutions in the array
     * @param statePos mapping from states to position
     * @param initialState initial state of the analysis
     * @return a new transient solution backed by the input parameters
     */
    public static <S> TransientSolution<S, S> fromArray(double[][] probs,
            double step, Map<S, Integer> statePos, S initialState) {

        TransientSolution<S, S> solution = new TransientSolution<>();
        solution.timeLimit = new BigDecimal(step * (probs.length - 1));
        solution.step = new BigDecimal(step);
        solution.samplesNumber = probs.length;
        solution.initialRegeneration = initialState;
        solution.regenerations = List.of(initialState);
        solution.columnStates = statePos.entrySet().stream()
                .sorted(Map.Entry.<S, Integer>comparingByValue())
                .map(e -> e.getKey()).collect(Collectors.toList());
        solution.solution = new double[probs.length][1][statePos.size()];
        for (int t = 0; t < probs.length; t++)
            for (int j = 0; j < statePos.size(); j++)
                solution.solution[t][0][j] = probs[t][j];
        return solution;
    }

    /**
     * Prepares a new instance and allocates the vector of probabilities.
     *
     * @param timeBound time bound for of the time series
     * @param timeStep time step
     * @param regenerations initial states
     * @param columnStates reachable states
     * @param initialRegeneration first initial state
     */
    public TransientSolution(BigDecimal timeBound, BigDecimal timeStep, List<R> regenerations,
            List<S> columnStates, R initialRegeneration) {
        this.timeLimit = timeBound;
        this.step = timeStep;
        this.samplesNumber = timeBound.divide(timeStep, MathContext.DECIMAL128).intValue() + 1;
        this.initialRegeneration = initialRegeneration;

        this.regenerations = new ArrayList<R>(regenerations);
        this.columnStates = new ArrayList<S>(columnStates);

        this.solution = new double[samplesNumber][regenerations.size()][columnStates.size()];
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < regenerations.size(); i++) {
            b.append(">> Initial regeneration: ");
            b.append(regenerations.get(i));
            b.append("\n");

            b.append(">> Column states: ");
            b.append(columnStates);
            b.append("\n");

            for (int t = 0; t < samplesNumber; t++) {
                b.append(step.multiply(new BigDecimal(t)));
                for (int j = 0; j < columnStates.size(); j++) {
                    b.append(" ");
                    b.append(solution[t][i][j]);
                }
                b.append("\n");
            }
        }

        return b.toString();
    }

    public void writeCSV(String filepath, int floatDigits) {
        try {
            FileWriter writer = new FileWriter(Path.of(filepath).toFile());
            this.writeCSV(writer, floatDigits);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCSV(Writer writer, int floatDigits) {
        String format = "%." + Integer.toString(floatDigits) + "f";
        
        String[] columns = Stream.concat(
                    Stream.of("initialRegeneration", "time"), 
                    columnStates.stream()
                ).map(Object::toString).toArray(String[]::new);
        
        int init = regenerations.indexOf(initialRegeneration);
        
        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(columns))) {
            for (int i = 0; i < regenerations.size(); i++) {
                // print initial regeneration first
                i = (i == 0) ? init : (i == init) ? 0 : i;
                
                for (int t = 0; t < samplesNumber; t++) {
                    csv.print(regenerations.get(i));
                    csv.print(step.multiply(new BigDecimal(t)));
                    for (int j = 0; j < columnStates.size(); j++) {
                        csv.print(String.format(format, solution[t][i][j]));
                    }
                    csv.println();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public static TransientSolution<String, String> readCSV(String filepath) {
        try {
            //TransientSolution.class.getResourceAsStream(.getClass()
            FileReader reader = new FileReader(Path.of(filepath).toFile());
            return readCSV(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TransientSolution<String, String> readCSV(Reader reader) {
        try (CSVParser csv = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            List<String> columnStates = csv.getHeaderNames();
            columnStates = new ArrayList<>(columnStates.subList(2, columnStates.size()));
            List<String> regenerations = new ArrayList<>();
            
            String prevRegeneration = null;
            BigDecimal prevTime = null;
            BigDecimal step = null;
            Integer samplesNumber = null;
            
            Map<String, Map<BigDecimal, double[]>> probs = new LinkedHashMap<>();
            for (CSVRecord record : csv) {
                // add a time -> probs map for new regenerations
                String regeneration = record.get(0);
                if (!regeneration.equals(prevRegeneration)) {
                    if (prevRegeneration != null) {
                        if (probs.containsKey(regeneration)) {
                            throw new IllegalArgumentException("Multiple row series for regeneration" + 
                                                               regeneration.toString());
                        }
                        
                        // check that the previous regeneration had the right number of time steps
                        if (samplesNumber == null) {
                            samplesNumber = probs.get(prevRegeneration).size(); 
                        } else if (samplesNumber != probs.get(prevRegeneration).size()) {
                            throw new IllegalArgumentException(
                                    String.format("%d steps instead of %d for regeneration %s",
                                            probs.get(prevRegeneration).size(), samplesNumber, prevRegeneration));
                        }   
                    }

                    regenerations.add(regeneration);
                    probs.put(regeneration, new LinkedHashMap<BigDecimal, double[]>());
                    prevTime = null;
                }

                // check that time starts from 0.0 with equal steps
                BigDecimal time = new BigDecimal(record.get(1)).stripTrailingZeros();
                if (prevTime == null) {
                    if (BigDecimal.ZERO.compareTo(time) != 0) {
                        throw new IllegalArgumentException("Time not starting from 0.0");
                    }                    
                    prevTime = BigDecimal.ZERO;
                } else if (step == null) {
                    step = time.subtract(prevTime);
                } else {
                    if (time.subtract(prevTime).compareTo(step) != 0) {
                        throw new IllegalArgumentException("Different time step at "+record.toString());
                    }
                }
                
                
                // parse and save probs of each column state
                double[] values = IntStream.range(0, columnStates.size())
                        .mapToDouble(i -> Double.parseDouble(record.get(i + 2)))
                        .toArray();
                probs.get(regeneration).put(time,  values);

                prevTime = time;
                prevRegeneration = regeneration;
            }

            // check that the last regeneration had the right number of time steps
            if (samplesNumber == null) {
                samplesNumber = probs.get(prevRegeneration).size(); 
            } else if (samplesNumber != probs.get(prevRegeneration).size()) {
                throw new IllegalArgumentException(
                        String.format("%d steps instead of %d for regeneration %s",
                                probs.get(prevRegeneration).size(), samplesNumber, prevRegeneration));
            }   

            // prepare and return the TransientSolution instance
            TransientSolution<String, String> result = new TransientSolution<>();
            result.columnStates = columnStates;
            result.regenerations = regenerations;
            result.initialRegeneration = regenerations.get(0);
            result.samplesNumber = samplesNumber;
            result.step = step;
            result.timeLimit = new BigDecimal(samplesNumber-1).multiply(step);
            
            result.solution = new double[samplesNumber][regenerations.size()][];
            for (int t = 0; t < samplesNumber; t++) {
                BigDecimal time = new BigDecimal(t).multiply(step).stripTrailingZeros();
                for (int i = 0; i < regenerations.size(); i++) {
                    String reg = regenerations.get(i);
                    result.solution[t][i] = probs.get(reg).get(time);
                }
            }

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    
    /**
     * Returns the first initial state of the analysis.
     *
     * @return first initial state
     */
    public R getInitialRegeneration() {
        return initialRegeneration;
    }

    public BigDecimal getTimeLimit() {
        return timeLimit;
    }

    public BigDecimal getStep() {
        return step;
    }

    public int getSamplesNumber() {
        return samplesNumber;
    }

    public List<R> getRegenerations() {
        return Collections.unmodifiableList(regenerations);
    }

    public List<S> getColumnStates() {
        return Collections.unmodifiableList(columnStates);
    }

    public double[][][] getSolution() {
        return solution;
    }

    /**
     * Computes the integral of each time series.
     *
     * @return a transient solution with cumulative values
     */
    public TransientSolution<R, S> computeIntegralSolution() {
        TransientSolution<R, S> integralSolution = new TransientSolution<R, S>(this.timeLimit,
                this.step, this.regenerations, this.columnStates, this.regenerations.get(0));

        for (int t = 1; t < integralSolution.solution.length; ++t)
            for (int i = 0; i < integralSolution.solution[t].length; ++i)
                for (int j = 0; j < integralSolution.solution[t][i].length; ++j)
                    integralSolution.solution[t][i][j] = integralSolution.solution[t - 1][i][j]
                            + 0.5 * step.doubleValue()
                                    * (solution[t][i][j] + solution[t - 1][i][j]);

        return integralSolution;
    }

    /**
     * Aggregates the marking probabilities based on a predicate.
     *
     * @param <R> type of initial states (such as regenerations)
     * @param solution input solution
     * @param markingConditions string of marking conditions separated by comma
     * @return transient probabilities for each marking condition
     */
    public static <R> TransientSolution<R, MarkingCondition> computeAggregateSolution(
            TransientSolution<R, Marking> solution, String markingConditions) {

        String[] c = markingConditions.split(",");
        MarkingCondition[] mc = new MarkingCondition[c.length];
        for (int i = 0; i < c.length; ++i)
            mc[i] = MarkingCondition.fromString(c[i]);

        return computeAggregateSolution(solution, solution.getInitialRegeneration(), mc);
    }


    /**
     * Aggregates the marking probabilities based on a predicate.
     *
     * @param <R> type of initial states (such as regenerations)
     * @param solution input solution
     * @param initialRegeneration first initial state
     * @param markingConditions marking conditions
     * @return transient probabilities for each marking condition
     */
    public static <R> TransientSolution<R, MarkingCondition> computeAggregateSolution(
            TransientSolution<R, Marking> solution, R initialRegeneration,
            MarkingCondition... markingConditions) {

        TransientSolution<R, MarkingCondition> aggregateSolution =
                new TransientSolution<R, MarkingCondition>(solution.timeLimit, solution.step,
                        Collections.singletonList(initialRegeneration),
                        Arrays.asList(markingConditions), initialRegeneration);

        int initialRegenerationIndex = solution.regenerations.indexOf(initialRegeneration);

        for (int j = 0; j < markingConditions.length; ++j)
            for (int m = 0; m < solution.columnStates.size(); ++m)
                if (markingConditions[j].evaluate(solution.columnStates.get(m)))
                    for (int t = 0; t < aggregateSolution.solution.length; ++t)
                        aggregateSolution.solution[t][0][j]
                                += solution.solution[t][initialRegenerationIndex][m];

        return aggregateSolution;
    }

    /**
     * Creates reward evaluators from a string of expressions separated by semicolon.
     *
     * @param rewardRates list of rewards separated by semicolon
     * @return reward rate evaluators
     */
    public static RewardRate[] rewardRates(String rewardRates) {

        String[] c = rewardRates.split(";");
        RewardRate[] rs = new RewardRate[c.length];
        for (int i = 0; i < c.length; ++i)
            rs[i] = RewardRate.fromString(c[i]);

        return rs;
    }
    
    /**
     * Computes rewards from a transient solution (only for the first initial state).
     *
     * @param <R> type of initial states (such as regenerations)
     * @param cumulative whether to compute cumulative rewards
     * @param solution input transient solution
     * @param rewardRates list of rewards separated by semicolon
     * @return transient rewards for each reward expression
     */
    public static <R> TransientSolution<R, RewardRate> computeRewards(boolean cumulative,
            TransientSolution<R, Marking> solution, String rewardRates) {

        return computeRewards(cumulative, solution,  rewardRates(rewardRates));
    }

    /**
     * Computes rewards from a transient solution (only for the first initial state).
     *
     * @param <R> type of initial states (such as regenerations)
     * @param cumulative whether to compute cumulative rewards
     * @param solution input transient solution
     * @param rewardRates list of rewards separated by semicolon
     * @return transient rewards for each reward expression
     */
    public static <R> TransientSolution<R, RewardRate> computeRewards(boolean cumulative,
            TransientSolution<R, Marking> solution, RewardRate... rewardRates) {

        R initialRegeneration = solution.getInitialRegeneration();

        TransientSolution<R, RewardRate> rewards = new TransientSolution<R, RewardRate>(
                solution.timeLimit, solution.step, Collections.singletonList(initialRegeneration),
                Arrays.asList(rewardRates), initialRegeneration);

        int initialRegenerationIndex = solution.regenerations.indexOf(initialRegeneration);

        double step = solution.step.doubleValue();
        double time = 0.0;

        if (!cumulative) {
            for (int t = 0; t < rewards.solution.length; ++t) {
                for (int j = 0; j < rewardRates.length; ++j)
                    // sum of reward rate in each state weighted by its
                    // probability
                    for (int m = 0; m < solution.columnStates.size(); ++m)
                        rewards.solution[t][0][j] += rewardRates[j].evaluate(time,
                                solution.columnStates.get(m))
                                * solution.solution[t][initialRegenerationIndex][m];
                time += step;
            }

        } else {
            double[] prevExpectedRewardRates = null;
            for (int t = 0; t < rewards.solution.length; ++t) {

                // computing expected reward rates at time t
                double[] expectedRewardRates = new double[rewardRates.length];
                for (int j = 0; j < rewardRates.length; ++j)
                    // sum of reward rate j in each state weighted by state
                    // probability
                    for (int m = 0; m < solution.columnStates.size(); ++m)
                        expectedRewardRates[j] += rewardRates[j].evaluate(time,
                                solution.columnStates.get(m))
                                * solution.solution[t][initialRegenerationIndex][m];

                if (prevExpectedRewardRates != null) {
                    for (int j = 0; j < rewardRates.length; ++j)
                        rewards.solution[t][0][j] = rewards.solution[t - 1][0][j] + 0.5 * step
                                * (expectedRewardRates[j] + prevExpectedRewardRates[j]);

                }
                prevExpectedRewardRates = expectedRewardRates;
                time += step;
            }

        }

        return rewards;
    }
    
    public boolean isClose(TransientSolution<String, String> other, double epsilon) {
        if (samplesNumber != other.samplesNumber)
            return false;
        
        if (step.compareTo(other.step) != 0)
            return false;
        
        if (regenerations.size() != other.regenerations.size())
            return false;

        if (columnStates.size() != other.columnStates.size())
            return false;

        for (int i = 0; i < regenerations.size(); i++) {
            int io = other.regenerations.indexOf(regenerations.get(i).toString());
            if (io == -1) return false;
            
            for (int j = 0; j < columnStates.size(); j++) {
                int jo = other.columnStates.indexOf(columnStates.get(j).toString());
                if (jo == -1) return false;
                
                for (int t = 0; t < samplesNumber; t++) {
                    if (Math.abs(solution[t][i][j] - other.solution[t][io][jo]) > epsilon) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    public boolean sumsToOne(double epsilon) {
        for (int t = 0; t < samplesNumber; t++) {
            for (int i = 0; i < solution[t].length; i++) {
                double sum = 0.0;
                for (int j = 0; j < solution[t][i].length; j++) {
                      sum += solution[t][i][j];
                }
                
                if (Math.abs(sum - 1.0) > epsilon) {
                    return false;
                }
            }
        }
        
        return true; 
    }
}