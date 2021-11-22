package org.oristool.models.tpn;

import org.oristool.petrinet.TransitionFeature;

import java.math.BigInteger;

public class RegenerationEpochLengthTransitionFeature implements TransitionFeature {
    private BigInteger R;

    /**
     * Builds a timed feature from minimum/maximum firing times specified as
     * strings. The strings must be valid inputs to the constructor of
     * OmegaBigDecimal.
     *
     * @param R regeneration epoch length (minimum time to fire)
     */
    public RegenerationEpochLengthTransitionFeature(BigInteger R) {
        this.R = R;
    }

    public BigInteger getR() {
        return R;
    }
}
