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

package org.oristool.simulator.rewards;

import java.util.ArrayList;
import java.util.List;

import org.oristool.simulator.Sequencer;
import org.oristool.simulator.Sequencer.SequencerEvent;

/**
 * A reward implementation for generic time.
 */
public abstract class BasicReward implements Reward {

    private final List<RewardObserver> observers = new ArrayList<>();
    private final Sequencer sequencer;
    private final RewardTime rewardTime;
    private final int samples;

    public abstract int getCurrentTimeSample();

    /**
     * Builds a reward instance and adds it as an observer to the simulator.
     *
     * @param sequencer a simulator instance
     * @param rewardTime the reward time
     * @param samples the number of time ticks in each run
     */
    public BasicReward(Sequencer sequencer, RewardTime rewardTime, int samples) {
        this.sequencer = sequencer;
        this.rewardTime = rewardTime;
        this.samples = samples;

        sequencer.addObserver(this);
    }

    @Override
    public void update(SequencerEvent event) {

        switch (event) {
            case RUN_START:
                if (!observers.isEmpty())
                    sequencer.addCurrentRunObserver(this);
                break;

            case FIRING_EXECUTED:
                // Note: this is the current sample after the firing (counting from 0)
                if (this.getCurrentTimeSample() > getSamples() - 1)
                    // Not interested in this run anymore
                    this.update(SequencerEvent.RUN_END);
                break;

            case RUN_END:
                // This event is received only when a run is aborted
                // by the sequencer (i.e. no firable transition) or
                // by the reward (i.e. not interested in this run anymore)
                abortCurrentRun();
                break;

            case SIMULATION_END:
                break;

            case SIMULATION_START:
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private void abortCurrentRun() {

        sequencer.removeCurrentRunObserver(this);
        notifyObservers(RewardEvent.RUN_END);
        if (observers.isEmpty())
            sequencer.removeObserver(this);
    }

    @Override
    public void addObserver(RewardObserver observer) {

        if (!observers.contains(observer))
            observers.add(observer);
    }

    @Override
    public void removeObserver(RewardObserver observer) {

        observers.remove(observer);
        if (observers.isEmpty())
            sequencer.removeObserver(this);
    }

    private void notifyObservers(RewardEvent event) {

        List<RewardObserver> observersCopy = new ArrayList<RewardObserver>(observers);
        for (RewardObserver o : observersCopy)
            o.update(event);
    }

    @Override
    public Sequencer getSequencer() {
        return sequencer;
    }

    @Override
    public RewardTime getRewardTime() {
        return rewardTime;
    }

    public int getSamples() {
        return samples;
    }
}
