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

package org.oristool.analyzer.stop;

import org.oristool.analyzer.Succession;

/**
 * A stop criterion that stops the analysis after the given number of nodes has
 * been added.
 */
public final class IterationsNumberStopCriterion implements StopCriterion {

    private final int maxIterationsCount;
    private int currentIterationsCount;

    public IterationsNumberStopCriterion(int maxIterationsCount) {
        this.maxIterationsCount = maxIterationsCount;
        this.currentIterationsCount = 0;
    }

    @Override
    public boolean stop() {
        return currentIterationsCount >= maxIterationsCount;
    }

    @Override
    public void notifyNodeAdded(Succession succession) {
        currentIterationsCount++;
    }
}
