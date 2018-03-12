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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * Ordered visit of all points of a set of intervals.
 *
 * <p>Note that, once built, this object is mutable: interval are eliminated
 * from the {@code intervals()} list once the time tick reaches their
 * right-bound.
 *
 * @param <E> type of intervals visited by the scanner
 */
@AutoValue
abstract class IntervalScanner<E extends Interval> {
    public abstract List<E> intervals();

    public static <E extends Interval> Builder<E> builder() {
        return new AutoValue_IntervalScanner.Builder<>();
    }

    @AutoValue.Builder
    public abstract static class Builder<E extends Interval> {

        abstract LinkedListBuilder<E> intervalsBuilder();

        /**
         * Adds an interval to the set of target ones.
         *
         * <p>Empty intervals are ignored.
         *
         * @param value the interval to be visited
         * @return this builder
         */
        public Builder<E> addInterval(E value) {
            if (value.leftPoint() <= value.rightPoint())
                intervalsBuilder().add(value);
            return this;
        }

        abstract IntervalScanner<E> autoBuild();  // not public

        public IntervalScanner<E> build() {
            IntervalScanner<E> scanner = autoBuild();

            // sort intervals by left point
            Comparator<Interval> comp = (x, y) -> Integer.compare(x.leftPoint(), y.leftPoint());
            Collections.sort(scanner.intervals(), comp);

            return scanner;
        }
    }

    private int time = Integer.MIN_VALUE;

    /**
     * Returns the current time point.
     *
     * <p>Before any call to {@code #nextTimeIntervals()}, the current time is equal
     * to {@code Integer.MIN_VALUE}. After a call, it is equal to current time
     * point contained in the intervals returned by the iterator.
     *
     * @return current time point
     */
    public int time() {
        return time;
    }

    // computed during iteration
    private int newTime;

    /**
     * Advances {@code time()} to the next time point and returns an iterator over
     * all time intervals that overlap with it.
     *
     * <p>The first time point is the minimum lower bound of any interval.
     *
     * <p>If the next time point is the upper bound of some interval, the interval
     * is removed from the {@code intervals()} list (after being returned by the
     * iterator).
     *
     * <p>At most one concurrent iteration is supported.
     *
     * @return iterator of intervals containing the next time point
     */
    public Iterator<E> nextTimeIntervals() {

        if (intervals().isEmpty())
            return Collections.emptyIterator();

        if (time == Integer.MIN_VALUE) {
            time = intervals().iterator().next().leftPoint();
        } else {
            time = newTime;
        }

        return new Iterator<E>() {
            PeekingIterator<E> it = Iterators.peekingIterator(intervals().iterator());
            boolean nextTimeRequired = false;

            @Override
            public boolean hasNext() {
                boolean hasNext = it.hasNext() && time >= it.peek().leftPoint();

                if (!hasNext) {
                    if (nextTimeRequired) {
                        newTime = time + 1;
                    } else if (!intervals().isEmpty()) {
                        newTime = intervals().iterator().next().leftPoint();
                    }
                }

                return hasNext;

            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();

                } else {
                    E next = it.next();
                    assert next.leftPoint() <= time && time <= next.rightPoint();

                    if (time == next.rightPoint()) {
                        it.remove();
                    } else {
                        nextTimeRequired = true;
                    }

                    return next;
                }
            }
        };
    }
}
