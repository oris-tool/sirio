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

import java.util.Arrays;
import java.util.List;

import org.oristool.analyzer.Succession;

/**
 * Logical AND of two stop criteria.
 */
public final class AndStopCriterion implements StopCriterion {

    private final List<StopCriterion> criteria;

    public AndStopCriterion(List<StopCriterion> criteria) {
        this.criteria = criteria;
    }

    public AndStopCriterion(StopCriterion... criteria) {
        this.criteria = Arrays.asList(criteria);
    }

    @Override
    public boolean stop() {
        for (StopCriterion sc : criteria) {
            if (!sc.stop())
                return false;
        }
        return true;
    }

    @Override
    public void notifySuccessionExtracted(Succession succession) {
        for (StopCriterion sc : criteria) {
            sc.notifySuccessionExtracted(succession);
        }
    }

    @Override
    public void notifyNodeAdded(Succession succession) {
        for (StopCriterion sc : criteria) {
            sc.notifyNodeAdded(succession);
        }
    }

    @Override
    public void notifySuccessionCreated(Succession succession) {
        for (StopCriterion sc : criteria) {
            sc.notifySuccessionCreated(succession);
        }
    }

    @Override
    public void notifySuccessionInserted(Succession succession) {
        for (StopCriterion sc : criteria) {
            sc.notifySuccessionInserted(succession);
        }
    }

    @Override
    public void notifySuccessionPreProcessed(Succession succession) {
        for (StopCriterion sc : criteria) {
            sc.notifySuccessionPreProcessed(succession);
        }
    }

    @Override
    public void notifySuccessionPostProcessed(Succession succession) {
        for (StopCriterion sc : criteria) {
            sc.notifySuccessionPostProcessed(succession);
        }
    }
}
