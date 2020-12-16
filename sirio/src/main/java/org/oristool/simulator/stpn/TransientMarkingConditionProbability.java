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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.simulator.Sequencer;
import org.oristool.simulator.Sequencer.SequencerEvent;
import org.oristool.simulator.TimeSeriesRewardResult;
import org.oristool.simulator.rewards.BasicReward;
import org.oristool.simulator.rewards.RewardTime;

/**
 * Reward estimating the transient probability of a marking condition.
 */
public final class TransientMarkingConditionProbability extends BasicReward {

    private final int samples;
    private final MarkingCondition markingCondition;
    private final Set<Marking> markings = new HashSet<Marking>();
    private final Map<Marking, long[]> countings = new HashMap<Marking, long[]>();

    // time elapsed according to the RewardTime of this Reward
    private BigDecimal currentRunElapsedTime;

    // current run (starting from 0)
    private long currentRun;

    // the last updated sample (starting from 0)
    private int currentSample;

    /**
     * Builds an instance for a given marking condition and number of time ticks.
     *
     * @param sequencer simulator
     * @param rewardTime time abstraction (discrete or continuous)
     * @param samples number of ticks
     * @param markingCondition target marking condition
     */
    public TransientMarkingConditionProbability(Sequencer sequencer, RewardTime rewardTime,
            int samples, MarkingCondition markingCondition) {

        super(sequencer, rewardTime, samples);

        if (samples <= 0)
            throw new IllegalArgumentException("A transient reward must have at least one sample");

        this.samples = samples;
        this.markingCondition = markingCondition;
    }

    private void addObservedMarking(Marking m) {

        markings.add(m);

        long[] c = new long[samples];
        for (int i = 0; i < c.length; ++i)
            c[i] = 0;

        countings.put(m, c);
    }

    @Override
    public void update(SequencerEvent event) {

        switch (event) {
            case SIMULATION_START:
                currentRun = -1;
                break;

            case RUN_START:
                currentRun++;
                if (markingCondition.evaluate(this.getSequencer().getInitialMarking())) {
                    if (!countings.containsKey(this.getSequencer().getInitialMarking()))
                        addObservedMarking(this.getSequencer().getInitialMarking());

                    countings.get(this.getSequencer().getInitialMarking())[0]++;
                }
                currentRunElapsedTime = BigDecimal.ZERO;
                currentSample = 0;
                break;

            case FIRING_EXECUTED:
                BigDecimal timeBeforeFiring = currentRunElapsedTime;
                BigDecimal sojournTime = this.getRewardTime()
                        .getSojournTime(this.getSequencer().getLastSuccession());
                currentRunElapsedTime = currentRunElapsedTime.add(sojournTime);

                final State child = this.getSequencer().getLastSuccession().getChild();
                final State parent = this.getSequencer().getLastSuccession().getParent();

                int startSample = timeBeforeFiring.divide(this.getRewardTime().getTimeStep(),
                        0, RoundingMode.CEILING).intValue();
                boolean sojournStartedOnTimeSample = timeBeforeFiring.remainder(
                        this.getRewardTime().getTimeStep()).compareTo(BigDecimal.ZERO) == 0;
                if (sojournStartedOnTimeSample)
                    startSample++;

                int endSample = currentRunElapsedTime.divide(this.getRewardTime().getTimeStep(),
                        0, RoundingMode.FLOOR).intValue();
                currentSample = endSample;

                boolean sojournEndedOnTimeSample = currentRunElapsedTime.remainder(
                        this.getRewardTime().getTimeStep()).compareTo(BigDecimal.ZERO) == 0;
                if (endSample > samples - 1) {
                    // The sojourn ended after the target interval
                    sojournEndedOnTimeSample = false;
                    endSample = samples - 1;
                }

                if (sojournEndedOnTimeSample)
                    endSample--;

                // adds the child marking if it is a target one and has never been observed before
                Marking childMarking = child.getFeature(PetriStateFeature.class).getMarking();
                if (markingCondition.evaluate(childMarking) && !countings.containsKey(childMarking))
                    addObservedMarking(childMarking);

                // increments counters in the open interval (timeBeforeFiring,currentRunElapsedTime)
                // if the marking before the transition firing was the target one
                Marking parentMarking = parent.getFeature(PetriStateFeature.class).getMarking();
                if (markingCondition.evaluate(parentMarking))
                    for (int i = startSample; i <= endSample; ++i)
                        countings.get(parentMarking)[i]++;

                // increments the counter of the sample on currentRunElapsedTime
                // if the arrival marking was the target one
                if (sojournEndedOnTimeSample && markingCondition.evaluate(childMarking))
                    countings.get(childMarking)[endSample + 1]++;

                // decrements the counter of the sample on currentRunElapsedTime
                // if the starting marking is the target one and a zero time transition has
                // fired
                if (sojournEndedOnTimeSample && markingCondition.evaluate(parentMarking)
                        && sojournTime.compareTo(BigDecimal.ZERO) == 0)
                    countings.get(parentMarking)[endSample + 1]--;
                break;

            case RUN_END:
                // This event is received only when a run is aborted (no firable transition)
                final State c = this.getSequencer().getLastSuccession().getChild();
                final Marking cMarking = c.getFeature(PetriStateFeature.class).getMarking();
                if (markingCondition.evaluate(cMarking))
                    for (int i = currentSample + 1; i <= countings
                            .get(cMarking).length - 1; ++i)
                        countings.get(cMarking)[i]++;
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

        for (Entry<Marking, long[]> e : countings.entrySet()) {
            BigDecimal[] timeSeries = new BigDecimal[samples];
            for (int i = 0; i < samples; i++)
                timeSeries[i] = new BigDecimal(e.getValue()[i])
                        .divide(new BigDecimal(currentRun + 1), MathContext.DECIMAL128);
            result.addTimeSeries(e.getKey(), timeSeries);
        }

        return result;
    }

    @Override
    public int getCurrentTimeSample() {
        return currentSample;
    }

}