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
/**
 *
 */

package org.oristool.simulator.stpn;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.oristool.simulator.Sequencer;
import org.oristool.simulator.Sequencer.SequencerEvent;
import org.oristool.simulator.rewards.BasicReward;
import org.oristool.simulator.rewards.RewardTime;

/**
 * Reward estimating the absolute time of a transition firing.
 */
public final class TransitionAbsoluteFiringTime extends BasicReward {

    private final String transitionName;
    private final List<BigDecimal> absoluteFiringTimes;
    private boolean transitionFiredInCurrentRun;
    private BigDecimal currentRunElapsedTime;

    /**
     * Builds a new instance for a given transition name.
     *
     * @param sequencer simulator
     * @param rewardTime time abstraction (discrete or continuous)
     * @param samples maximum number of time ticks
     * @param transitionName target transition name
     */
    public TransitionAbsoluteFiringTime(Sequencer sequencer, RewardTime rewardTime, int samples,
            String transitionName) {

        super(sequencer, rewardTime, samples);

        this.transitionName = transitionName;
        this.absoluteFiringTimes = new LinkedList<>();
    }

    @Override
    public void update(SequencerEvent event) {

        switch (event) {
            case SIMULATION_START:
                break;

            case RUN_START:
                currentRunElapsedTime = BigDecimal.ZERO;
                transitionFiredInCurrentRun = false;
                break;

            case FIRING_EXECUTED:
                if (!transitionFiredInCurrentRun) {
                    BigDecimal sojournTime = this.getRewardTime().getSojournTime(
                            this.getSequencer().getLastSuccession());

                    currentRunElapsedTime = currentRunElapsedTime.add(sojournTime);

                    if (this.getSequencer().getLastSuccession().getEvent().toString()
                            .equals(transitionName)) {
                        absoluteFiringTimes.add(currentRunElapsedTime);
                        transitionFiredInCurrentRun = true;
                    }
                }
                break;

            case RUN_END:
                break;

            case SIMULATION_END:
                break;

            default:
                throw new IllegalStateException();
        }

        super.update(event);
    }

    @Override
    public Object evaluate() {
        return absoluteFiringTimes;
    }

    @Override
    public int getCurrentTimeSample() {
        return transitionFiredInCurrentRun ? 1 : 0;
    }
}
