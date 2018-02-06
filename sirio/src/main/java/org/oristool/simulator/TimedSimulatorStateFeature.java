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

package org.oristool.simulator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.oristool.analyzer.state.StateFeature;
import org.oristool.petrinet.Transition;

/**
 * State feature of time-based STPN simulations. It includes a time-to-fire for
 * each enabled transition.
 */
public class TimedSimulatorStateFeature implements StateFeature {

    private Map<Transition, BigDecimal> ttf = new HashMap<Transition, BigDecimal>();

    public BigDecimal getTimeToFire(Transition t) {
        return ttf.get(t);
    }

    public void setTimeToFire(Transition t, BigDecimal timeToFire) {
        ttf.put(t, timeToFire);
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();

        for (Entry<Transition, BigDecimal> e : ttf.entrySet()) {
            b.append(e.getKey());
            b.append(" = ");
            b.append(e.getValue());
            b.append("\n");
        }

        return b.toString();
    }

}
