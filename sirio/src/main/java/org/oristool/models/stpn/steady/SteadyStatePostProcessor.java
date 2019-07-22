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

package org.oristool.models.stpn.steady;

import java.math.BigDecimal;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.models.stpn.trees.StochasticSuccessionFeature;

/**
 * Post-processor updating {@code ReachingProbabilityFeature}. This class is a
 * decorator augmenting the operations performed by another post-processor.
 */
class SteadyStatePostProcessor implements SuccessionProcessor {

    private SuccessionProcessor baseProc;

    public SteadyStatePostProcessor(SuccessionProcessor baseProc) {
        this.baseProc = baseProc;
    }

    @Override
    public Succession process(Succession succession) {
        Succession withEnablingSyncs = baseProc.process(succession);

        if (succession.getParent() != null) {
            BigDecimal parentReachingProbability = withEnablingSyncs.getParent()
                    .getFeature(ReachingProbabilityFeature.class).getValue();
            BigDecimal childReachingProbability = withEnablingSyncs
                    .getFeature(StochasticSuccessionFeature.class).getProbability()
                    .multiply(parentReachingProbability);

            withEnablingSyncs.getChild().addFeature(
                    new ReachingProbabilityFeature(childReachingProbability));
        }

        return withEnablingSyncs;
    }
}
