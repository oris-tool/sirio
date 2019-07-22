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

package org.oristool.analyzer.stop;

import java.util.function.Predicate;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.state.State;

/**
 * Stop criterion based on a state predicate.
 */
public final class StateStopCriterion implements StopCriterion {

    private final Predicate<State> predicate;
    private State lastExtractedState;

    public StateStopCriterion(Predicate<State> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void notifySuccessionExtracted(Succession succession) {
        lastExtractedState = succession.getChild();
    }

    @Override
    public boolean stop() {
        return predicate.test(lastExtractedState);
    }
}
