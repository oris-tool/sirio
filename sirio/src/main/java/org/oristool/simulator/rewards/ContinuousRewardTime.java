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

import java.math.BigDecimal;
import java.math.MathContext;

import org.oristool.analyzer.Succession;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.TimedSimulatorStateFeature;

/**
 * Continuous reward time: time steps are arbitrary and sojourn times are those
 * associated with a firing event.
 */
public final class ContinuousRewardTime implements RewardTime {

    private final BigDecimal timeStep;

    public ContinuousRewardTime(BigDecimal timeStep) {
        this.timeStep = timeStep;
    }

    @Override
    public BigDecimal getTimeStep() {
        return timeStep;
    }

    @Override
    public BigDecimal getSojournTime(Succession succession) {
        Transition fired = (Transition) succession.getEvent();
        Marking m = succession.getParent().getFeature(PetriStateFeature.class).getMarking();
        BigDecimal rate = new BigDecimal(
                fired.getFeature(StochasticTransitionFeature.class).clockRate().evaluate(m));
        return succession.getParent().getFeature(TimedSimulatorStateFeature.class)
                .getTimeToFire(fired).divide(rate, MathContext.DECIMAL128);
    }
}
