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

package org.oristool.simulator.rewards;

import org.oristool.simulator.Sequencer;
import org.oristool.simulator.SequencerObserver;

/**
 * General reward interface.
 */
public interface Reward extends SequencerObserver {

    public enum RewardEvent {
        RUN_END
    }

    Sequencer getSequencer();

    RewardTime getRewardTime();

    Object evaluate();

    void addObserver(RewardObserver observer);

    void removeObserver(RewardObserver observer);
}
