package org.oristool.simulator.samplers;

import java.math.BigDecimal;

public final class ShiftedExponentialSampler implements Sampler {
	
	private final BigDecimal rate;
    private final BigDecimal shift;
    
    public ShiftedExponentialSampler(BigDecimal rate, BigDecimal shift) {
        this.rate = rate;
    	this.shift = shift;
    }

    @Override
    public BigDecimal getSample() {

        return new BigDecimal(-Math.log(1 - Math.random()) / rate.doubleValue()).add(shift);
    }


}
