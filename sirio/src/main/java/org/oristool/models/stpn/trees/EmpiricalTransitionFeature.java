package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import org.oristool.petrinet.TransitionFeature;

public class EmpiricalTransitionFeature implements TransitionFeature {
    // TODO - TBD: note that histogramPDF can be provided, and then CDF can be computed starting from PDF
    private ArrayList<BigDecimal> histogramCDF;
    private BigInteger binsNumber;
    private BigDecimal lower;
    private BigDecimal upper;

    public EmpiricalTransitionFeature (ArrayList<BigDecimal> histogramCDF, BigDecimal lower, BigDecimal upper){
        this.histogramCDF = histogramCDF;
        binsNumber = BigInteger.valueOf(histogramCDF.size());
        this.lower = lower;
        this.upper = upper;
    }

    public ArrayList<BigDecimal> getHistogramCDF() {
        return histogramCDF;
    }

    public BigInteger getBinsNumber() {
        return binsNumber;
    }

    public BigDecimal getLower() {
        return lower;
    }

    public BigDecimal getUpper() {
        return upper;
    }

    public static EmpiricalTransitionFeature newInstance(ArrayList<BigDecimal> histogramCDF, BigDecimal lower, BigDecimal upper) {
        return new EmpiricalTransitionFeature(histogramCDF, lower, upper);
    }
}
