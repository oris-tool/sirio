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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class IntervalScannerTest {
    @Test
    void test() {
        testOn(List.of(
                Range.of(2, 2),
                Range.of(4, 6),
                Range.of(5, 7),
                Range.of(8, 8),
                Range.of(10, 12),
                Range.of(10, 11)));

        testOn(List.of(
                Range.of(0, 0),
                Range.of(2, 2),
                Range.of(3, 3),
                Range.of(5, 5),
                Range.of(7, 8),
                Range.of(8, 8)));

        testOn(List.of(
                Range.of(0, 0),
                Range.of(0, 1),
                Range.of(1, 2),
                Range.of(4, 8),
                Range.of(4, 5),
                Range.of(6, 7)));
    }

    private static void initWeights(State[] w) {
        for (int i = 0; i < w.length; i++)
            w[i] = State.NEW;
    }

    /**
     * Checks that each point of each range is updated exactly once and then
     * finalized exactly once.
     *
     * @param ranges input set of ranges
     */
    void testOn(List<Interval> ranges) {
        // also checks that all useful time points are visited exactly once
        Set<Integer> timesToVisit = ranges.stream().flatMapToInt(r ->
            IntStream.rangeClosed(r.leftPoint(), r.rightPoint()))
                .boxed().collect(Collectors.toSet());
        Set<Integer> timesVisited = new HashSet<>();

        // prepares the scanner and the states keeping track of weight status
        IntervalScanner.Builder<Interval> scannerBuilder = IntervalScanner.builder();
        Map<Interval, State[]> weightsOf = new HashMap<>(ranges.size());
        State[][] weights = new State[ranges.size()][];
        int k = 0;
        for (Interval range : ranges) {
            scannerBuilder.addInterval(range);
            weights[k] = new State[range.rightPoint() - range.leftPoint() + 1];
            weightsOf.put(range, weights[k]);
            initWeights(weights[k]);
            k++;
        }

        // iterates
        IntervalScanner<Interval> scanner = scannerBuilder.build();
        while (!scanner.intervals().isEmpty()) {
            final Iterator<Interval> it = scanner.nextTimeIntervals();
            assertFalse(timesVisited.contains(scanner.time()));
            timesVisited.add(scanner.time());
            // printStatus(scanner, weightsOf);

            while (it.hasNext()) {
                Interval range = it.next();
                assertTrue(scanner.time() >= range.leftPoint());
                assertTrue(scanner.time() <= range.rightPoint());

                int weightIdx = scanner.time() - range.leftPoint();
                assertEquals(State.NEW, weightsOf.get(range)[weightIdx]);
                weightsOf.get(range)[weightIdx] = State.UPDATED;

                if (scanner.time() == range.rightPoint()) {
                    for (int i = range.leftPoint(); i <= range.rightPoint(); i++) {
                        int idx = i - range.leftPoint();
                        assertEquals(State.UPDATED, weightsOf.get(range)[idx]);
                        weightsOf.get(range)[idx] = State.FINALIZED;
                    }
                }
            }
        }

        assertEquals(timesToVisit, timesVisited);
        for (State[] ws : weights)
            for (State w : ws)
                assertEquals(State.FINALIZED, w);
    }


    @SuppressWarnings("unused")
    private static void printStatus(IntervalScanner<Interval> scanner,
            Map<Interval, State[]> weightsOf) {

        for (Interval range : scanner.intervals()) {
            State[] weights = weightsOf.get(range);

            for (int i = 0; i <= range.rightPoint(); i++) {
                if (i < range.leftPoint())
                    System.out.printf("--( )");
                else {
                    System.out.printf("%2d(%s)", i, weights[i - range.leftPoint()]);
                }

                if (i == scanner.time())
                    System.out.printf("< ");
                else
                    System.out.printf("  ");
            }
            System.out.printf("%n");
        }
    }

    private static class Range implements Interval {

        private final int left;
        private final int right;

        private Range(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public static Range of(int left, int right) {
            return new Range(left, right);
        }

        @Override
        public int leftPoint() {
            return left;
        }

        @Override
        public int rightPoint() {
            return right;
        }
    }

    private static enum State {
        NEW, UPDATED, FINALIZED;

        @Override
        public String toString() {
            return Integer.toString(this.ordinal());
        }
    }
}
