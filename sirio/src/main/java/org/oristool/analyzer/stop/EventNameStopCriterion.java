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

package org.oristool.analyzer.stop;

import org.oristool.analyzer.Succession;

/**
 * Criterion stopping the analysis when a specific event fired.
 */
public class EventNameStopCriterion implements StopCriterion {

    private final String eventName;
    private String lastEventName;

    public EventNameStopCriterion(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    @Override
    public void notifySuccessionExtracted(Succession succession) {
        if (succession.getEvent() != null)
            lastEventName = succession.getEvent().toString();
        else
            lastEventName = null;
    }

    @Override
    public boolean stop() {
        return lastEventName != null
                && lastEventName.equals(eventName);
    }
}
