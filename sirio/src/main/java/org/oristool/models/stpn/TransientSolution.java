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

package org.oristool.models.stpn;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;

public class TransientSolution<R, S> {
    private BigDecimal timeLimit;
    private BigDecimal step;
    int samplesNumber;
    R initialRegeneration;

    List<R> regenerations;
    List<S> columnStates;

    double[][][] solution;

    public R getInitialRegeneration() {
        return initialRegeneration;
    }

    public TransientSolution(BigDecimal timeLimit, BigDecimal step, List<R> regenerations,
            List<S> columnStates) {
        this(timeLimit, step, regenerations, columnStates, regenerations.get(0));
    }

    public TransientSolution(BigDecimal timeLimit, BigDecimal step, List<R> regenerations,
            List<S> columnStates, R initialRegeneration) {
        this.timeLimit = timeLimit;
        this.step = step;
        this.samplesNumber = timeLimit.divide(step, MathContext.DECIMAL128).intValue() + 1;
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

    public TransientSolution<R, S> computeIntegralSolution() {
        TransientSolution<R, S> integralSolution = new TransientSolution<R, S>(this.timeLimit,
                this.step, this.regenerations, this.columnStates);

        for (int t = 1; t < integralSolution.solution.length; ++t)
            for (int i = 0; i < integralSolution.solution[t].length; ++i)
                for (int j = 0; j < integralSolution.solution[t][i].length; ++j)
                    integralSolution.solution[t][i][j] = integralSolution.solution[t - 1][i][j]
                            + 0.5 * step.doubleValue()
                                    * (solution[t][i][j] + solution[t - 1][i][j]);

        return integralSolution;
    }

    public static <R> TransientSolution<R, MarkingCondition> computeAggregateSolution(
            TransientSolution<R, Marking> solution, String markingConditions) {

        String[] c = markingConditions.split(",");
        MarkingCondition[] mc = new MarkingCondition[c.length];
        for (int i = 0; i < c.length; ++i)
            mc[i] = MarkingCondition.fromString(c[i]);

        return computeAggregateSolution(solution, mc);
    }

    public static <R> TransientSolution<R, MarkingCondition> computeAggregateSolution(
            TransientSolution<R, Marking> solution, MarkingCondition... markingConditions) {
        return computeAggregateSolution(solution, solution.regenerations.get(0), markingConditions);
    }

    public static <R> TransientSolution<R, MarkingCondition> computeAggregateSolution(
            TransientSolution<R, Marking> solution, R initialRegeneration,
            MarkingCondition... markingConditions) {

        TransientSolution<R, MarkingCondition> aggregateSolution = new TransientSolution<R, MarkingCondition>(
                solution.timeLimit, solution.step, Collections.singletonList(initialRegeneration),
                Arrays.asList(markingConditions));

        int initialRegenerationIndex = solution.regenerations.indexOf(initialRegeneration);

        for (int j = 0; j < markingConditions.length; ++j)
            for (int m = 0; m < solution.columnStates.size(); ++m)
                if (markingConditions[j].evaluate(solution.columnStates.get(m)))
                    for (int t = 0; t < aggregateSolution.solution.length; ++t)
                        aggregateSolution.solution[t][0][j] += solution.solution[t][initialRegenerationIndex][m];

        return aggregateSolution;
    }

    public static <R> TransientSolution<R, RewardRate> computeRewards(boolean cumulative,
            TransientSolution<R, Marking> solution, String rewardRates) {

        String[] c = rewardRates.split(";");
        RewardRate[] rs = new RewardRate[c.length];
        for (int i = 0; i < c.length; ++i)
            rs[i] = RewardRate.fromString(c[i]);

        return computeRewards(cumulative, solution, rs);
    }

    public static <R> TransientSolution<R, RewardRate> computeRewards(boolean cumulative,
            TransientSolution<R, Marking> solution, RewardRate... rewardRates) {
        return computeRewards(cumulative, solution, solution.regenerations.get(0), rewardRates);
    }

    public static <R> TransientSolution<R, RewardRate> computeRewards(boolean cumulative,
            TransientSolution<R, Marking> solution, R initialRegeneration,
            RewardRate... rewardRates) {

        TransientSolution<R, RewardRate> rewards = new TransientSolution<R, RewardRate>(
                solution.timeLimit, solution.step, Collections.singletonList(initialRegeneration),
                Arrays.asList(rewardRates));

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
}