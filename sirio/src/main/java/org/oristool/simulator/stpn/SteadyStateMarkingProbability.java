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

package org.oristool.simulator.stpn;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.simulator.Sequencer;
import org.oristool.simulator.Sequencer.SequencerEvent;
import org.oristool.simulator.rewards.BasicReward;
import org.oristool.simulator.rewards.NumericRewardResult;
import org.oristool.simulator.rewards.RewardTime;

/**
 * Reward estimating the steady-state probability of a marking.
 */
public final class SteadyStateMarkingProbability extends BasicReward {

    private final Marking marking;

    private BigDecimal cumulativeSojournTime;
    private BigDecimal cumulativeElapsedTime;
    private int currentSample;

    /**
     * Builds a new instance for a given marking, time abstraction, number of time ticks.
     *
     * @param sequencer simulator
     * @param rewardTime time abstraction (discrete or continuous)
     * @param samples number of time ticks
     * @param marking target marking
     */
    public SteadyStateMarkingProbability(Sequencer sequencer, RewardTime rewardTime, int samples,
            Marking marking) {

        super(sequencer, rewardTime, samples);
        this.marking = marking;
    }

    @Override
    public void update(SequencerEvent event) {

        switch (event) {
            case SIMULATION_START:
                this.cumulativeSojournTime = BigDecimal.ZERO;
                this.cumulativeElapsedTime = BigDecimal.ZERO;
                break;

            case RUN_START:
                currentSample = -1;
                break;

            case FIRING_EXECUTED:
                BigDecimal sojournTime = this.getRewardTime()
                        .getSojournTime(this.getSequencer().getLastSuccession());

                cumulativeElapsedTime = cumulativeElapsedTime.add(sojournTime);
                currentSample = cumulativeElapsedTime
                        .divide(this.getRewardTime().getTimeStep(), 0, RoundingMode.FLOOR)
                        .intValue();

                if (this.getSequencer().getLastSuccession().getParent()
                        .getFeature(PetriStateFeature.class).getMarking().equals(marking))
                    cumulativeSojournTime = cumulativeSojournTime.add(sojournTime);
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

        if (cumulativeElapsedTime.equals(BigDecimal.ZERO))
            return new NumericRewardResult(BigDecimal.ZERO);
        else
            return new NumericRewardResult(
                    cumulativeSojournTime.divide(cumulativeElapsedTime, MathContext.DECIMAL128));
    }

    @Override
    public int getCurrentTimeSample() {
        return currentSample;
    }
}
