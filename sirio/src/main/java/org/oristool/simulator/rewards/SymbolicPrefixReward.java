/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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

import java.math.BigDecimal;
import java.math.MathContext;

import org.oristool.simulator.Sequencer;
import org.oristool.simulator.Sequencer.SequencerEvent;

/**
 * Reward tracking the occurrence of a sequence of event firings from the
 * initial state.
 */
public final class SymbolicPrefixReward extends BasicReward {
    private String[] prefix;
    private long successfulRuns;

    // current run (starting from 0)
    private long currentRun;

    // the last updated sample (starting from 0)
    private int currentSample;

    /**
     * Builds a new instance for a given simulator and sequence of transitions.
     *
     * @param sequencer simulator
     * @param prefix sequence of event names
     */
    public SymbolicPrefixReward(Sequencer sequencer, String... prefix) {

        super(sequencer, new DiscreteRewardTime(), prefix.length);

        // Ensure that prefix has at least one element, otherwise the result is trivial
        if (prefix.length <= 0)
            throw new IllegalArgumentException(
                    "A symbolic prefix should have a length greater than zero");

        this.prefix = prefix;
    }

    @Override
    public void update(SequencerEvent event) {

        switch (event) {
            case SIMULATION_START:
                this.successfulRuns = 0;
                currentRun = -1;
                break;

            case RUN_START:
                currentRun++;
                currentSample = 0;
                break;

            case FIRING_EXECUTED:
                String fired = this.getSequencer().getLastSuccession().getEvent().getName();
                if (!prefix[currentSample].equals(fired)) {
                    // This run took a bad path: we're not interested in it anymore
                    currentSample = getSamples();
                } else {
                    if (currentSample == prefix.length - 1) {
                        // The whole symbolic prefix has been executed: count and stop observing
                        successfulRuns++;
                        currentSample = getSamples();
                    } else {
                        // The right transition of the prefix has been executed: increase the prefix
                        // index
                        currentSample++;
                    }
                }
                break;

            case RUN_END:
                // This event is received only when a run is aborted (no firable transition)
                // No-op: the symbolic prefix has been partially executed and the current run
                // should not be counted among the successufulRuns
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

        return new NumericRewardResult(new BigDecimal(successfulRuns)
                .divide(new BigDecimal(currentRun + 1), MathContext.DECIMAL128));
    }

    @Override
    public int getCurrentTimeSample() {
        return currentSample;
    }
}
