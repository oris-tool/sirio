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
import java.util.HashMap;
import java.util.Map;

import org.oristool.petrinet.Marking;

public class SteadyStateSolution<T> {

    private Map<T, BigDecimal> steadyState;

    public SteadyStateSolution() {
        this.steadyState = new HashMap<T, BigDecimal>();
    }

    public SteadyStateSolution(Map<T, BigDecimal> steadyState) {
        this.steadyState = steadyState;
    }

    public Map<T, BigDecimal> getSteadyState() {
        return steadyState;
    }

    public void setSteadyState(Map<T, BigDecimal> steadyState) {
        this.steadyState = steadyState;
    }

    /**
     * Computes rewards for a given solution.
     *
     * @param solution input solution
     * @param rewardRates expression of reward rates
     * @return a new solution for the rewards
     */
    public static SteadyStateSolution<RewardRate> computeRewards(
            SteadyStateSolution<Marking> solution, String rewardRates) {
        String[] c = rewardRates.split(";");
        RewardRate[] rs = new RewardRate[c.length];
        for (int i = 0; i < c.length; ++i)
            rs[i] = RewardRate.fromString(c[i]);

        return computeRewards(solution, rs);
    }

    /**
     * Computes rewards for a given solution.
     *
     * @param solution input solution
     * @param rewardRates expressions of reward rates
     * @return a new solution for the rewards
     */
    public static SteadyStateSolution<RewardRate> computeRewards(
            SteadyStateSolution<Marking> solution, RewardRate... rewardRates) {
        double time = 0;// Because steady state

        SteadyStateSolution<RewardRate> solutionRewards = new SteadyStateSolution<RewardRate>();

        for (RewardRate rewardRate : rewardRates) {
            solutionRewards.getSteadyState().put(rewardRate, BigDecimal.ZERO);
            for (Marking marking : solution.getSteadyState().keySet()) {
                BigDecimal newValue = solutionRewards.getSteadyState().get(rewardRate)
                        .add(new BigDecimal(rewardRate.evaluate(time, marking))
                                .multiply(solution.getSteadyState().get(marking)));
                solutionRewards.getSteadyState().put(rewardRate, newValue);
            }

        }

        return solutionRewards;
    }
}
