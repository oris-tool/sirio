package org.oristool.models.tpn;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.petrinet.TransitionFeature;

import java.math.BigInteger;

public final class ConcurrencyTransitionFeature implements TransitionFeature {
    private BigInteger C;

    public ConcurrencyTransitionFeature(BigInteger C) {
        this.C = C;
    }

    /**
     * Builds a timed feature from minimum/maximum firing times specified as
     * strings. The strings must be valid inputs to the constructor of
     * {@link OmegaBigDecimal}.
     *
     * @param C regeneration epoch length (minimum time to fire)
     */
    public ConcurrencyTransitionFeature(String C) {
        this(BigInteger.valueOf(Integer.parseInt(C)));
    }

    public BigInteger getC() {
        return C;
    }

    /*@Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof ConcurrencyTransitionFeature))
            return false;

        ConcurrencyTransitionFeature o = (ConcurrencyTransitionFeature) other;

        return C.equals(o.C);
    }

    @Override
    public int hashCode() {

        int result = 17;

        result = 31 * result + this.C.hashCode();

        return result;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("-- ConcurrencyTransitionFeature --\n");
        b.append("[");
        b.append(this.C);
        b.append("]");
        return b.toString();
    }*/
}
