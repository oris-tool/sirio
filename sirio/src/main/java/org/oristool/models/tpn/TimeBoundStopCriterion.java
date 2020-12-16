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

package org.oristool.models.tpn;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Variable;

/**
 * A global stop criterion.
 */
public final class TimeBoundStopCriterion implements StopCriterion {
    private final OmegaBigDecimal timeBound;
    private boolean lastClassIsBeyondTimeBound;

    public TimeBoundStopCriterion(OmegaBigDecimal timeBound) {
        this.timeBound = timeBound;
    }

    @Override
    public boolean stop() {
        return lastClassIsBeyondTimeBound;
    }

    @Override
    public void notifySuccessionExtracted(Succession succession) {
        DBMZone d = succession.getChild()
                .getFeature(TimedStateFeature.class).getDomain();
        OmegaBigDecimal minAge =
                d.getBound(Variable.AGE, Variable.TSTAR).negate();
        lastClassIsBeyondTimeBound = minAge.compareTo(timeBound) > 0;
    }

    @Override
    public void notifySuccessionPreProcessed(Succession succession) {

    }

    @Override
    public void notifyNodeAdded(Succession succession) {

    }

    @Override
    public void notifySuccessionCreated(Succession succession) {

    }

    @Override
    public void notifySuccessionPostProcessed(Succession succession) {

    }

    @Override
    public void notifySuccessionInserted(Succession succession) {

    }
}
