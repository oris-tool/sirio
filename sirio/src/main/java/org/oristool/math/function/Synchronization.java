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

package org.oristool.math.function;

import java.math.BigDecimal;

import org.oristool.math.expression.Variable;

/**
 * Synchronization information of a variable.
 */
public class Synchronization {

    private Variable distributed;
    private BigDecimal delay;

    public Synchronization(Variable distributed, BigDecimal coefficient) {
        this.distributed = distributed;
        this.delay = coefficient;
    }

    public Synchronization(Synchronization s) {
        this.distributed = s.distributed;
        this.delay = s.delay;
    }

    public Variable getDistributed() {
        return distributed;
    }

    public BigDecimal getDelay() {
        return delay;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Synchronization))
            return false;

        Synchronization s = (Synchronization) obj;

        return distributed.equals(s.distributed)
                && delay.compareTo(s.delay) == 0;
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.distributed.hashCode();
        result = 31 * result + this.delay.hashCode();

        return result;
    }
}