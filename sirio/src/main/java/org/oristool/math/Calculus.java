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

import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.math.domain.DBMZone;
import org.oristool.math.domain.DBMZone.Subzone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;

public class Calculus {

    public static GEN ios(GEN g, Subzone s, boolean f) {

        if (g.getDomain().getVariables().size() != s.getDomain()
                .getVariables().size() + 1
                || !g.getDomain().getVariables()
                        .containsAll(s.getDomain().getVariables())
                || !g.getDomain().getVariables()
                        .contains(s.getProjectedVar()))
            throw new IllegalArgumentException(
                    "The subzone refers to a different set of variables");

        Variable t = Variable.TSTAR;
        OmegaBigDecimal p = OmegaBigDecimal.POSITIVE_INFINITY;

        Expolynomial x = g.getDensity().integrate(s.getProjectedVar());
        Expolynomial y, z;

        if (s.getMaxVar().equals(t)
                || s.getMaxVarAdvance().equals(p))
            y = x.evaluate(s.getProjectedVar(),
                    s.getMaxVarAdvance());
        else
            y = x.evaluate(s.getProjectedVar(), !f,
                    s.getMaxVar(), s.getMaxVarAdvance().bigDecimalValue());

        if (s.getMinVar().equals(t)
                || s.getMinVarDelay().equals(p))
            z = x.evaluate(s.getProjectedVar(), s.getMinVarDelay()
                    .negate());
        else
            z = x.evaluate(s.getProjectedVar(), !f,
                    s.getMinVar(), s.getMinVarDelay().negate()
                            .bigDecimalValue());

        y.sub(z);

        return new GEN(new DBMZone(s.getDomain()), y);
    }
    
    public static Set<Subzone> cpsz(DBMZone d, Variable v) {

        if (d.getVariables().size() < 2)
            throw new IllegalArgumentException("Cannot project T_STAR");

        if (!d.getVariables().contains(v))
            throw new IllegalArgumentException("Variable " + v + " not present");

        OmegaBigDecimal p = OmegaBigDecimal.POSITIVE_INFINITY;
        OmegaBigDecimal w = OmegaBigDecimal.ZERO;

        Set<Variable> r = new LinkedHashSet<>(d.getVariables());
        r.remove(v);

        Set<Subzone> s = new LinkedHashSet<>();

        for (Variable l : r) {
            sij: for (Variable h : r) {

                DBMZone z = new DBMZone(d);
                OmegaBigDecimal x = d.getBound(l, v);
                OmegaBigDecimal y = d.getBound(v, h);

                for (Variable o : r) {
                    if (!o.equals(l)) {
                        OmegaBigDecimal u = d.getBound(o, v);
                        if (x.equals(p) && u.isFinite()) {
                            continue sij;
                        } else if (x.equals(p) && u.equals(p)) {
                            z.imposeBound(o, l, w);
                        } else {
                            z.imposeBound(o, l, u.subtract(x));
                        }
                    }

                    if (!o.equals(h)) {
                        OmegaBigDecimal u = d.getBound(v, o);
                        if (y.equals(p) && u.isFinite()) {
                            continue sij;
                        } else if (y.equals(p) && u.equals(p)) {
                            z.imposeBound(h, o, w);
                        } else {
                            z.imposeBound(h, o, u.subtract(y));
                        }
                    }
                }

                if (z.isFullDimensional()) {
                    z.projectVariable(v);
                    s.add(new Subzone(z, v, l, x, h, y));
                }
            }
        }

        return s;
    }

    public static Set<Subzone> cspsz(DBMZone d, Variable k) {

        if (d.getVariables().size() < 2)
            throw new IllegalArgumentException("Cannot project T_STAR");

        if (!d.getVariables().contains(k))
            throw new IllegalArgumentException("Variable " + k + " not present");

        Variable t = Variable.TSTAR;
        OmegaBigDecimal p = OmegaBigDecimal.POSITIVE_INFINITY;
        OmegaBigDecimal w = OmegaBigDecimal.ZERO;
        
        DBMZone z = new DBMZone(d);
        z.setNewGround(k);

        Set<Variable> rr = new LinkedHashSet<>(d.getVariables());
        rr.remove(k);

        Set<Subzone> o = new LinkedHashSet<>();

        for (Variable i : rr) {
            sij: for (Variable j : rr) {
                DBMZone s = new DBMZone(z);
                OmegaBigDecimal x = i.equals(t) ? 
                        d.getBound(k, t) : d.getBound(i, t);
                OmegaBigDecimal y = j.equals(t) ? 
                        d.getBound(t, k) : d.getBound(t, j);

                for (Variable r : rr) {
                    if (!r.equals(i)) {
                        OmegaBigDecimal u = r.equals(t) ? 
                                d .getBound(k, t) : d.getBound(r, t);
                        if (x.equals(p) && u.isFinite()) {
                            continue sij;
                        } else if (x .equals(p) && u.equals(p)) {
                            s.imposeBound(r, i, w);
                        } else {
                            s.imposeBound(r, i, u.subtract(x));
                        }
                    }

                    if (!r.equals(j)) {
                        OmegaBigDecimal u = r.equals(t) ? 
                                d.getBound(t, k) : d.getBound(t, r);
                        if (y.equals(p) && u.isFinite()) {
                            continue sij;
                        } else if (y.equals(p) && u.equals(p)) {
                            s.imposeBound(j, r, w);
                        } else {
                            s.imposeBound(j, r, u.subtract(y));
                        }
                    }
                }

                if (!i.equals(j)) {
                    s.imposeBound(i, j, x.add(y));
                }

                if (s.isFullDimensional()) {
                    o.add(new Subzone(s, k, j, y, i, x));
                }
            }
        }

        return o;
    }
}
