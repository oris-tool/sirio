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

package org.oristool.math;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.math.domain.DBMZone;
import org.oristool.math.domain.DBMZone.Subzone;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;

public class Calculus {

    public static GEN ios(GEN g, Subzone s, boolean f) {

        // TODO Add missing implementation
       return GEN.getDETInstance(Variable.X, BigDecimal.ONE);
    }
    
    public static Set<Subzone> cpsz(DBMZone d, Variable v) {

        // TODO Add missing implementation
        return new LinkedHashSet<>();
    }

    public static Set<Subzone> cspsz(DBMZone d, Variable k) {

        // TODO Add missing implementation
        return new LinkedHashSet<>();
    }
}
