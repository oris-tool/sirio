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

package org.oristool.models.stpn.steady;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.oristool.analyzer.state.State;

/**
 * Embedded DTMC solver and representation.
 *
 * @param <R> type of DTMC states
 */
class EmbeddedDTMC<R> {

    private Map<R, Map<R, BigDecimal>> reachingProbabilities;
    private Map<R, BigDecimal> steadyState;

    private EmbeddedDTMC() {
    }

    public static <R> EmbeddedDTMC<R> compute(Set<R> regenerations,
            Map<R, Map<R, Set<State>>> regenerationClasses) {
        EmbeddedDTMC<R> a = new EmbeddedDTMC<R>();

        Map<R, Integer> states = mapRegenerativeStates(regenerations);
        RealMatrix tmpReachingProbabilities = computeReachingProbabilities(states,
                regenerationClasses);
        RealVector tmpSteadyState = computeSteadyState(states, tmpReachingProbabilities);

        a.steadyState = new HashMap<>();
        for (R state : states.keySet()) {
            a.steadyState.put(state, new BigDecimal(tmpSteadyState.getEntry(states.get(state))));
        }

        a.reachingProbabilities = new HashMap<>();
        for (R from : states.keySet()) {
            a.reachingProbabilities.put(from, new HashMap<R, BigDecimal>());
            for (R to : states.keySet()) {
                a.reachingProbabilities.get(from).put(
                        to,
                        new BigDecimal(tmpReachingProbabilities.getEntry(states.get(from),
                                states.get(to))));
            }
        }

        return a;
    }

    private static <R> Map<R, Integer> mapRegenerativeStates(Set<R> regenerations) {
        Map<R, Integer> states = new HashMap<>();
        int i = 0;
        for (R state : regenerations) {
            states.put(state, i);
            i++;
        }
        return states;
    }

    private static <R> RealMatrix computeReachingProbabilities(Map<R, Integer> states,
            Map<R, Map<R, Set<State>>> regenerationClasses) {
        int n = states.size();
        RealMatrix reachingProbabilities = new Array2DRowRealMatrix(new double[n][n]);
        for (R from : regenerationClasses.keySet()) {
            for (R to : regenerationClasses.get(from).keySet()) {
                for (State s : regenerationClasses.get(from).get(to)) {
                    reachingProbabilities.addToEntry(states.get(from), states.get(to), s
                            .getFeature(ReachingProbabilityFeature.class).getValue().doubleValue());
                }
            }
        }
        return reachingProbabilities;
    }

    private static <R> RealVector computeSteadyState(Map<R, Integer> states,
            RealMatrix tmpReachingProbabilities) {
        int n = states.size();
        RealVector constants = new ArrayRealVector(new double[n + 1]);
        constants.setEntry(n, 1);
        RealMatrix coefficients = new Array2DRowRealMatrix(new double[n + 1][n]);
        coefficients.setSubMatrix(
                tmpReachingProbabilities.transpose()
                        .subtract(MatrixUtils.createRealIdentityMatrix(n)).getData(), 0, 0);
        for (int i = 0; i < n; i++) {
            coefficients.addToEntry(n, i, 1);
        }
        DecompositionSolver solver = new QRDecomposition(coefficients).getSolver();
        RealVector steadyState = solver.solve(constants);
        return steadyState;
    }

    public Map<R, Map<R, BigDecimal>> getReachingProbabilities() {

        return reachingProbabilities;
    }

    public Map<R, BigDecimal> getSteadyState() {

        return steadyState;
    }
}
