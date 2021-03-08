package org.oristool.simulator.samplers;

import org.oristool.models.stpn.trees.EmpiricalTransitionFeature;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

public class EmpiricalTransitionSampler implements Sampler {

    private ArrayList<BigDecimal> histogramCDF;
    private BigInteger binsNumber;
    private BigDecimal lower;
    private BigDecimal upper;

    public EmpiricalTransitionSampler(ArrayList<BigDecimal> histogramCDF, BigDecimal lower, BigDecimal upper){
        this.histogramCDF = histogramCDF;
        this.binsNumber = BigInteger.valueOf(histogramCDF.size());;
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    public BigDecimal getSample() {
        double firstDraw = Math.random();

        int selectedBin = 0;

        while(histogramCDF.get(selectedBin).doubleValue() <= firstDraw) {
            selectedBin++;
        }

        double secondDraw = Math.random() * (lower.doubleValue() + (upper.doubleValue() - lower.doubleValue()) * (selectedBin + 1) / binsNumber.intValue());
        return new BigDecimal(secondDraw);
    }
}
