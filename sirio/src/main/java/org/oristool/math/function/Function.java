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

package org.oristool.math.function;

import java.util.Collections;
import java.util.List;

import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;

public interface Function extends PartitionedFunction {

    DBMZone getDomain();

    Expolynomial getDensity();

    @Override
    default List<? extends Function> getFunctions() {
        return Collections.singletonList(this);
    }

    @Override
    String toMathematicaString();
}
