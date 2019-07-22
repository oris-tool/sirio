/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2019 The ORIS Authors.
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

package org.oristool.simulator.rewards;

import java.util.ArrayList;
import java.util.List;

import org.oristool.simulator.rewards.Reward.RewardEvent;

/**
 * Evaluator collecting the values of a reward over multiple runs.
 */
public final class RewardEvaluator implements RewardObserver {

    public enum RewardEvaluatorEvent {
        RESULT_EVALUATED
    }

    private final List<RewardEvaluatorObserver> observers = new ArrayList<>();
    private final Reward reward;
    private final long runs;
    private Object result;

    /**
     * Builds an evaluator for a given reward and number of runs.
     *
     * @param reward target reward
     * @param runs number of runs
     */
    public RewardEvaluator(Reward reward, long runs) {
        this.reward = reward;
        this.runs = runs;

        reward.addObserver(this);
    }

    public long getRuns() {
        return runs;
    }

    public Reward getReward() {
        return reward;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public void update(RewardEvent event) {
        switch (event) {
            case RUN_END:
                if (reward.getSequencer().getCurrentRunNumber() == (runs - 1)) {
                    // The last run ended
                    this.result = reward.evaluate();
                    reward.removeObserver(this);
                    notifyObservers(RewardEvaluatorEvent.RESULT_EVALUATED);
                }

                break;

            default:
                // ignore the event
        }
    }

    /**
     * Adds an observer to this evaluator.
     *
     * @param observer input observer
     */
    public void addObserver(RewardEvaluatorObserver observer) {

        if (!observers.contains(observer))
            observers.add(observer);
    }

    public void removeObserver(RewardEvaluatorObserver observer) {

        observers.remove(observer);
    }

    private void notifyObservers(RewardEvaluatorEvent event) {

        List<RewardEvaluatorObserver> observersCopy = new ArrayList<>(observers);
        for (RewardEvaluatorObserver o : observersCopy)
            o.update(event);
    }
}
