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

package org.oristool.simulator;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.oristool.petrinet.Marking;

public final class TimeSeriesRewardResult {

    private final Map<Marking, BigDecimal[]> results = new LinkedHashMap<>();
    private final BigDecimal timeStep;

    public TimeSeriesRewardResult(BigDecimal timeStep) {
        this.timeStep = timeStep;
    }

    public BigDecimal[] addTimeSeries(Marking m, BigDecimal[] timeSeries) {
        return results.put(m, timeSeries);
    }

    public BigDecimal[] getTimeSeries(Marking m) {
        return results.get(m);
    }

    public Collection<Marking> getMarkings() {
        return results.keySet();
    }

    public BigDecimal getTimeStep() {
        return timeStep;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        BigDecimal time = BigDecimal.ZERO;

        Iterator<BigDecimal[]> r = results.values().iterator();
        if (!r.hasNext())
            return "";

        b.append("Column markings: ");
        b.append(results.keySet());
        b.append("\n");

        int max = r.next().length;
        for (int i = 0; i < max; ++i) {
            b.append(time);
            for (BigDecimal[] s : results.values())
                b.append(" " + s[i]);

            b.append("\n");
            time = time.add(timeStep);
        }

        return b.toString();
    }

    /**
     * Checks that, for each time point, the sum of probabilities adds to 1 (up to
     * the given error).
     *
     * @param error allowed error at each time point
     * @return true if probabilities are greater than (1-error) at each time point
     */
    public boolean isValid(BigDecimal error) {

        Iterator<BigDecimal[]> r = results.values().iterator();
        if (!r.hasNext())
            return true;

        int max = r.next().length;
        for (int i = 0; i < max; ++i) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal[] s : results.values())
                sum = sum.add(s[i]);

            if (BigDecimal.ONE.subtract(sum).abs().compareTo(error) > 0)
                return false;
        }

        return true;
    }
}
