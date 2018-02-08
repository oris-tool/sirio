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

package org.oristool.models.pn;

import org.oristool.analyzer.AnalyzerObserverAdapter;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.stop.StopCriterion;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;

/**
 * Stop criterion based on a marking condition.
 */
public final class MarkingConditionStopCriterion extends AnalyzerObserverAdapter
        implements StopCriterion {

    MarkingCondition markingCondition;
    Marking lastExtractedMarking;

    public MarkingConditionStopCriterion(MarkingCondition mc) {
        this.markingCondition = mc;
    }

    public MarkingConditionStopCriterion(String cond) {
        this.markingCondition = MarkingCondition.fromString(cond);
    }

    @Override
    public void notifySuccessionExtracted(Succession succession) {
        lastExtractedMarking = succession.getChild().getFeature(PetriStateFeature.class)
                        .getMarking();
    }

    @Override
    public boolean stop() {
        return markingCondition.evaluate(lastExtractedMarking);
    }
}
