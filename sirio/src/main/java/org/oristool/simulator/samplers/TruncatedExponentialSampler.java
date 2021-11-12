package org.oristool.simulator.samplers;

import java.math.BigDecimal;

public class TruncatedExponentialSampler implements Sampler{
    private final BigDecimal rate;
    private final BigDecimal shift;
    private final BigDecimal limit;

    public TruncatedExponentialSampler(BigDecimal rate, BigDecimal shift, BigDecimal limit) {
        this.rate = rate;
        this.shift = shift;
        this.limit = limit;
    }

    @Override
    public BigDecimal getSample() {

        BigDecimal sample;
        do {
            sample = BigDecimal.valueOf(-Math.log(1 - Math.random()) / rate.doubleValue()).add(shift);
        } while(sample.compareTo(limit)>0);

        return sample;
    }
}
