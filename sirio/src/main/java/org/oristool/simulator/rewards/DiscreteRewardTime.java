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

import java.math.BigDecimal;

import org.oristool.analyzer.Succession;

/**
 * Discrete reward time: time steps and sojourn times are always equal to 1.
 */
public final class DiscreteRewardTime implements RewardTime {

    @Override
    public BigDecimal getTimeStep() {
        return BigDecimal.ONE;
    }

    @Override
    public BigDecimal getSojournTime(Succession succession) {
        return BigDecimal.ONE;
    }
}
