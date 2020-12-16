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

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.oristool.analyzer.state.StateFeature;
import org.oristool.math.expression.Variable;

/**
 * State feature encoding synchronizations between enabled timers.
 */
public final class EnablingSyncsFeature implements StateFeature {

    public final Map<Variable, Map<Variable, BigDecimal>> enablingSyncs;

    public EnablingSyncsFeature() {
        enablingSyncs = new LinkedHashMap<Variable, Map<Variable, BigDecimal>>();
    }

    /**
     * Builds a deep copy of another instance.
     *
     * @param other input instance
     */
    public EnablingSyncsFeature(EnablingSyncsFeature other) {
        enablingSyncs = new LinkedHashMap<>();

        for (Map.Entry<Variable, Map<Variable, BigDecimal>> e : other.enablingSyncs
                .entrySet())
            enablingSyncs.put(e.getKey(),
                    new LinkedHashMap<Variable, BigDecimal>(e.getValue()));
    }

    /**
     * Discards all synchronization information about a variable.
     *
     * @param t variable to be removed
     */
    public void remove(Variable t) {
        // removes the Variable t (det or gen)

        // as a key
        enablingSyncs.remove(t);

        // as a gen with enabling sync (removes empty maps)
        Iterator<Map.Entry<Variable, Map<Variable, BigDecimal>>> iter = enablingSyncs
                .entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Variable, Map<Variable, BigDecimal>> entry = iter.next();
            entry.getValue().remove(t);
            if (entry.getValue().size() == 0)
                iter.remove();
        }
    }

    /**
     * Returns the set of (initially) deterministic variables.
     *
     * @return set of deterministic variables
     */
    public Set<Variable> dets() {
        return enablingSyncs.keySet();
    }

    /**
     * Returns the enabling delay of a deterministic variable with respect to a
     * general one.
     *
     * @param det deterministic variable
     * @param gen general variable
     * @return enabling delay
     */
    public BigDecimal get(Variable det, Variable gen) {
        if (!enablingSyncs.containsKey(det)
                || !enablingSyncs.get(det).containsKey(gen))
            return null;
        else
            return enablingSyncs.get(det).get(gen);
    }

    /**
     * Returns the enabling delay of a determinstic variable with respect to a
     * general one.
     *
     * @param det deterministic variable
     * @param gen general variable
     * @param enablingTime delay
     */
    public void set(Variable det, Variable gen, BigDecimal enablingTime) {
        // at the firing of `det`, Variable `gen` will be
        // enabled by `enablingTime` time units

        if (!enablingSyncs.containsKey(det))
            enablingSyncs.put(det, new LinkedHashMap<Variable, BigDecimal>());

        enablingSyncs.get(det).put(gen, enablingTime);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof EnablingSyncsFeature))
            return false;

        EnablingSyncsFeature e = (EnablingSyncsFeature) obj;

        return enablingSyncs.equals(e.enablingSyncs);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + this.enablingSyncs.hashCode();
        return result;
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();
        b.append("-- EnablingSyncsFeature --\n");

        for (Map.Entry<Variable, Map<Variable, BigDecimal>> e : enablingSyncs
                .entrySet()) {
            b.append("@");
            b.append(e.getKey());
            String joiner = ": ";
            for (Map.Entry<Variable, BigDecimal> s : e.getValue().entrySet()) {
                b.append(joiner);
                joiner = ", ";
                b.append(s.getKey());
                b.append("|");
                b.append(s.getValue());
            }
            b.append("\n");
        }

        return b.toString();
    }
}
