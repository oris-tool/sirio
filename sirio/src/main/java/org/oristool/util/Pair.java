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

package org.oristool.util;

import java.util.Objects;

/**
 * Immutable pair of elements.
 * @param <A> type of first element
 * @param <B> type of second element
 */
public class Pair<A, B> {

    private final A first;
    private final B second;

    private Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a pair of elements.
     *
     * @param <A> type of the first element
     * @param <B> type of the second element
     * @param first first element
     * @param second second element
     * @return pair of the two input elements
     */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    public A first() {
        return first;
    }

    public B second() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Pair))
            return false;

        Pair<?,?> other = (Pair<?,?>)o;

        return Objects.equals(this.first(), other.first())
                && Objects.equals(this.second(), other.second());
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ',' + second + ')';
    }
}
