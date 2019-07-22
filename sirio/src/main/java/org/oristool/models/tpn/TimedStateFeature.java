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

package org.oristool.models.tpn;

import org.oristool.analyzer.state.StateFeature;
import org.oristool.math.domain.DBMZone;

public final class TimedStateFeature implements StateFeature {

    private DBMZone domain;

    public TimedStateFeature() {

    }

    public TimedStateFeature(TimedStateFeature other) {
        this.domain = new DBMZone(other.getDomain());
    }

    public DBMZone getDomain() {
        return domain;
    }

    public void setDomain(DBMZone domain) {
        this.domain = domain;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof TimedStateFeature))
            return false;

        TimedStateFeature o = (TimedStateFeature) other;

        return domain.equals(o.domain);
    }

    @Override
    public int hashCode() {
        // FIXME the value should be cached
        return 0; // domain.hashCode();
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();
        b.append("-- TimedStateFeature --\n");
        b.append(domain);

        return b.toString();
    }

}
