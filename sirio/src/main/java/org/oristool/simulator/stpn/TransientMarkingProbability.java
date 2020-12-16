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
import org.oristool.simulator.TimeSeriesRewardResult;
import org.oristool.simulator.rewards.BasicReward;
import org.oristool.simulator.rewards.ContinuousRewardTime;
import org.oristool.simulator.rewards.RewardTime;

/**
 * Reward estimating the transient probability of a marking.
 */
public final class TransientMarkingProbability extends BasicReward {

    private final Marking marking;
    private final long[] countings;

    // time elapsed according to the RewardTime of this Reward
    private BigDecimal currentRunElapsedTime;

    // current run (starting from 0)
    private long currentRun;

    // the last updated sample (starting from 0)
    private int currentSample;

    /**
     * Builds a new instance for the given marking.
     *
     * @param sequencer simulator
     * @param rewardTime time abstraction (discrete or continuous)
     * @param samples number of time ticks
     * @param marking target marking
     */
    public TransientMarkingProbability(Sequencer sequencer, RewardTime rewardTime, int samples,
            Marking marking) {

        // One more sample is needed in case of immediate transitions with continuous time
        super(sequencer, rewardTime,
                rewardTime instanceof ContinuousRewardTime ? samples + 1 : samples);

        if (samples <= 0)
            throw new IllegalArgumentException("A transient reward must have at least one sample");

        this.marking = marking;
        this.countings = new long[samples];
    }

    @Override
    public void update(SequencerEvent event) {

        switch (event) {
            case SIMULATION_START:
                for (int i = 0; i < countings.length; ++i)
                    countings[i] = 0;
                currentRun = -1;
                break;

            case RUN_START:
                currentRun++;
                if (this.getSequencer().getInitialMarking().equals(marking))
                    countings[0]++;
                currentRunElapsedTime = BigDecimal.ZERO;
                currentSample = 0;
                break;

            case FIRING_EXECUTED:
                BigDecimal timeBeforeFiring = currentRunElapsedTime;
                BigDecimal sojournTime = this.getRewardTime()
                        .getSojournTime(this.getSequencer().getLastSuccession());
                currentRunElapsedTime = currentRunElapsedTime.add(sojournTime);

                int startSample = timeBeforeFiring.divide(this.getRewardTime().getTimeStep(),
                        0, RoundingMode.CEILING).intValue();
                boolean sojournStartedOnTimeSample = timeBeforeFiring
                        .remainder(this.getRewardTime().getTimeStep()).equals(BigDecimal.ZERO);
                if (sojournStartedOnTimeSample)
                    startSample++;

                int endSample = currentRunElapsedTime.divide(this.getRewardTime().getTimeStep(),
                        0, RoundingMode.FLOOR).intValue();
                currentSample = endSample;

                boolean sojournEndedOnTimeSample = currentRunElapsedTime
                        .remainder(this.getRewardTime().getTimeStep()).equals(BigDecimal.ZERO);
                if (endSample > countings.length - 1) {
                    // The sojourn ended after the target interval: counters should be incremented
                    // in the
                    // interval (timeBeforeFiring, currentRunElapsedTime] for the marking before the
                    // transition
                    sojournEndedOnTimeSample = false;
                    endSample = countings.length - 1;
                }

                if (sojournEndedOnTimeSample)
                    endSample--;

                // increments counters in the open interval (timeBeforeFiring,
                // currentRunElapsedTime)
                // if the marking before the transition firing was the target one
                if (this.getSequencer().getLastSuccession().getParent()
                        .getFeature(PetriStateFeature.class).getMarking().equals(marking))
                    for (int i = startSample; i <= endSample; ++i)
                        countings[i]++;

                // increments the counter of the sample on currentRunElapsedTime
                // if the arrival marking was the target one
                if (sojournEndedOnTimeSample && this.getSequencer().getLastSuccession().getChild()
                        .getFeature(PetriStateFeature.class).getMarking().equals(marking))
                    countings[endSample + 1]++;

                // decrements the counter of the sample on currentRunElapsedTime
                // if the starting marking is the target one and a zero time transition has
                // fired
                if (sojournEndedOnTimeSample
                        && this.getSequencer().getLastSuccession().getParent()
                                .getFeature(PetriStateFeature.class).getMarking().equals(marking)
                        && sojournTime.equals(BigDecimal.ZERO))
                    countings[endSample + 1]--;
                break;

            case RUN_END:
                // This event is received only when a run is aborted (no firable transition)
                if (this.getSequencer().getLastSuccession().getChild()
                        .getFeature(PetriStateFeature.class).getMarking().equals(marking))
                    for (int i = currentSample + 1; i <= countings.length - 1; ++i)
                        countings[i]++;
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

        TimeSeriesRewardResult result = new TimeSeriesRewardResult(
                this.getRewardTime().getTimeStep());
        BigDecimal[] timeSeries = new BigDecimal[countings.length];

        for (int i = 0; i < countings.length; ++i)
            timeSeries[i] = new BigDecimal(countings[i]).divide(new BigDecimal(currentRun + 1),
                    MathContext.DECIMAL128);

        result.addTimeSeries(marking, timeSeries);
        return result;
    }

    @Override
    public int getCurrentTimeSample() {
        return currentSample;
    }

}