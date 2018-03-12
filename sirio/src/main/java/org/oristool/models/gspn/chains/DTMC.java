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

package org.oristool.models.gspn.chains;

import java.util.ArrayList;
import java.util.List;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

/**
 * Directed graph encoding a DTMC.
 *
 * @param <S> type of DTMC states
 */
public final class DTMC<S> {

    private MutableValueGraph<S, Double> probsGraph;
    private List<S> initialStates;
    private List<Double> initialProbs;

    private DTMC() {

    }

    public MutableValueGraph<S, Double> probsGraph() {
        return probsGraph;
    }

    public List<S> initialStates() {
        return initialStates;
    }

    public List<Double> initialProbs() {
        return initialProbs;
    }

    /**
     * Creates an empty DTMC.
     *
     * @param <S> type of DTMC states
     * @return empty DTMC instance
     */
    public static <S> DTMC<S> create() {

        DTMC<S> DTMC = new DTMC<>();
        DTMC.probsGraph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        DTMC.initialStates = new ArrayList<>();
        DTMC.initialProbs = new ArrayList<>();
        return DTMC;
    }
}
