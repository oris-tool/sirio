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
package org.oristool.models.stpn;

import java.util.List;

import org.oristool.analyzer.state.State;
import org.oristool.petrinet.Marking;

public abstract class MarkovRenewalTransientAnalysis {
    protected double[][][] globalKernel;
    protected double[][][] localKernel;

    protected List<State> globalStates;
    protected List<State> localStates;
    protected List<Marking> markings;

    double[][][] getGlobalKernel() {
        if (globalKernel == null)
            throw new IllegalStateException("Global kernel hasn't been set");
        return globalKernel;
    }

    double[][][] getLocalKernel() {
        if (localKernel == null)
            throw new IllegalStateException("Local kernel hasn't been set");
        return localKernel;
    }

    List<State> getGlobalStates() {
        if (globalStates == null)
            throw new IllegalStateException("Global states haven't been set");
        return globalStates;
    }

    List<State> getLocalStates() {
        if (localStates == null)
            throw new IllegalStateException("Local states haven't been set");
        return localStates;
    }

    List<Marking> getMarkings() {
        if (markings == null)
            throw new IllegalStateException("Markings haven't been set");
        return markings;
    }
}
