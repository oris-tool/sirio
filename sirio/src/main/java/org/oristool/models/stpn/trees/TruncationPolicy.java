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

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.math.OmegaBigDecimal;

/**
 * Priority policy halting the enumeration on dropRegenerative leaves or when
 * the total reaching (before time limit) probability of nodes to be expanded is
 * lower than epsilon (or both).
 */
public final class TruncationPolicy implements EnumerationPolicy {
    /**
     * Priority queue of nodes to be expanded.
     */
    private final Queue<Succession> queue = new PriorityQueue<>(1,
            new Comparator<Succession>() {
                @Override
                public int compare(Succession n1, Succession n2) {
                    BigDecimal eta1 = reachingProbabilityBeforeLimit.get(n1);
                    BigDecimal eta2 = reachingProbabilityBeforeLimit.get(n2);

                    return eta2.compareTo(eta1);
                }
            });

    /**
     * Reaching probability of nodes on the queue.
     */
    private final Map<Succession, BigDecimal> reachingProbabilityBeforeLimit = new HashMap<>();

    /**
     * Total reaching probability of nodes on the queue.
     */
    private BigDecimal totalReachingProbability = BigDecimal.ZERO;

    /**
     * Reaching probabilities refer to tauAge in [0, tauAgeLimit].
     */
    private final OmegaBigDecimal tauAgeLimit;

    public OmegaBigDecimal getTauAgeLimit() {
        return tauAgeLimit;
    }

    /**
     * Threshold for totalReachingProbability.
     */
    private final BigDecimal epsilon;

    public BigDecimal getEpsilon() {
        return epsilon;
    }

    /**
     * Builds a truncation policy for a given time limit and allowed error (total
     * probability mass of discarded nodes).
     *
     * <p>Nodes with higher probability are expanded first.
     *
     * @param epsilon allowed error
     * @param tauAgeLimit time bound
     */
    public TruncationPolicy(BigDecimal epsilon, OmegaBigDecimal tauAgeLimit) {

        this.epsilon = epsilon;
        this.tauAgeLimit = tauAgeLimit;
    }

    @Override
    public void add(Succession succession) {

        // Adds the node if not dropRegenerative or regeneratives are permitted
        TransientStochasticStateFeature transientFeature = succession
                .getChild().getFeature(TransientStochasticStateFeature.class);
        if (!reachingProbabilityBeforeLimit.containsKey(succession))
            try {
                reachingProbabilityBeforeLimit.put(succession, transientFeature
                        .computeVisitedProbability(
                                OmegaBigDecimal.ZERO,
                                tauAgeLimit,
                                succession.getChild().getFeature(
                                        StochasticStateFeature.class)));
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Error computing visited probability for the transient state class",
                        e);
            }

        totalReachingProbability = totalReachingProbability
                .add(reachingProbabilityBeforeLimit.get(succession));

        queue.add(succession);
    }

    @Override
    public Succession remove() {
        Succession removed = queue.poll();

        if (removed != null) {
            totalReachingProbability = totalReachingProbability
                    .subtract(reachingProbabilityBeforeLimit.get(removed));
            reachingProbabilityBeforeLimit.remove(removed);
        }
        return removed;
    }

    @Override
    public boolean isEmpty() {
        // The queue is emptied if the total probability mass is less than
        // epsilon
        if (totalReachingProbability.compareTo(epsilon) <= 0)
            queue.clear();

        return queue.isEmpty();
    }

}
