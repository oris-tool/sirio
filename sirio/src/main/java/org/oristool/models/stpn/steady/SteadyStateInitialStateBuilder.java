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

package org.oristool.models.stpn.steady;

import java.math.BigDecimal;

import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;

/**
 * State builder for steady-state analysis of STPNs. This class is a decorator
 * adding a {@code ReachingProbabilityFeature} with value 1 to the state built
 * by another {@code StateBuilder}.
 */
class SteadyStateInitialStateBuilder<T> implements StateBuilder<T> {

    private StateBuilder<T> sb;

    public SteadyStateInitialStateBuilder(StateBuilder<T> sb) {
        this.sb = sb;
    }

    @Override
    public State build(T discreteState) {
        State s = sb.build(discreteState);
        s.addFeature(new ReachingProbabilityFeature(BigDecimal.ONE));
        return s;
    }
}
