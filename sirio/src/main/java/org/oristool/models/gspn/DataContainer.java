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

package org.oristool.models.gspn;

import java.util.List;

import org.oristool.analyzer.log.AnalysisLogger;

/**
 * Container for input parameters and output results of uniformization.
 */
class DataContainer {

    private boolean noUnderflow;
    private int leftTruncPoint;
    private int rightTruncPoint;
    private double lambda;
    private double[] foxGlynnWeights;
    private double foxGlynnTotalWeight;
    private double requiredAccuracy;
    private double underFlowLimit;
    private double overFlowLimit;
    private double foxGlynnModeWeight;

    private double[] initialProbsVector;
    private double[][] matrixUnif;

    private List<double[]> dtmcTransientProbs;
    private double[] cmtcTransientProbs;

    private AnalysisLogger logger;

    public boolean isNoUnderflow() {
        return noUnderflow;
    }

    public void setNoUnderflow(boolean noUnderflow) {
        this.noUnderflow = noUnderflow;
    }

    public int getLeftTruncPoint() {
        return leftTruncPoint;
    }

    public void setLeftTruncPoint(int leftTruncPoint) {
        this.leftTruncPoint = leftTruncPoint;
    }

    public int getRightTruncPoint() {
        return rightTruncPoint;
    }

    public void setRightTruncPoint(int rightTruncPoint) {
        this.rightTruncPoint = rightTruncPoint;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public double[] getFoxGlynnWeights() {
        return foxGlynnWeights;
    }

    public void setFoxGlynnWeights(double[] foxGlynnWeights) {
        this.foxGlynnWeights = foxGlynnWeights;
    }

    public double getFoxGlynnTotalWeight() {
        return foxGlynnTotalWeight;
    }

    public void setFoxGlynnTotalWeight(double foxGlynnTotalWeight) {
        this.foxGlynnTotalWeight = foxGlynnTotalWeight;
    }

    public double getRequiredAccuracy() {
        return requiredAccuracy;
    }

    public void setRequiredAccuracy(double requiredAccuracy) {
        this.requiredAccuracy = requiredAccuracy;
    }

    public double getUnderFlowLimit() {
        return underFlowLimit;
    }

    public void setUnderFlowLimit(double underFlowLimit) {
        this.underFlowLimit = underFlowLimit;
    }

    public double getOverFlowLimit() {
        return overFlowLimit;
    }

    public void setOverFlowLimit(double overFlowLimit) {
        this.overFlowLimit = overFlowLimit;
    }

    public double getFoxGlynnModeWeight() {
        return foxGlynnModeWeight;
    }

    public void setFoxGlynnModeWeight(double foxGlynnModeWeight) {
        this.foxGlynnModeWeight = foxGlynnModeWeight;
    }

    public double[] getInitialProbsVector() {
        return initialProbsVector;
    }

    public void setInitialProbsVector(double[] initialProbsVector) {
        this.initialProbsVector = initialProbsVector;
    }

    public double[][] getMatrixUnif() {
        return matrixUnif;
    }

    public void setMatrixUnif(double[][] matrixUnif) {
        this.matrixUnif = matrixUnif;
    }

    public List<double[]> getDtmcTransientProbs() {
        return dtmcTransientProbs;
    }

    public void setDtmcTransientProbs(List<double[]> dtmcTransientProbs) {
        this.dtmcTransientProbs = dtmcTransientProbs;
    }

    public double[] getCmtcTransientProbs() {
        return cmtcTransientProbs;
    }

    public void setCmtcTransientProbs(double[] cmtcTransientProbs) {
        this.cmtcTransientProbs = cmtcTransientProbs;
    }

    public AnalysisLogger getLogger() {
        return logger;
    }

    public void setLogger(AnalysisLogger logger) {
        this.logger = logger;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("|-----DataContainer------");
        b.append("\n|lambda=");
        b.append(lambda);
        b.append("\n|Left Truncation Point=");
        b.append(leftTruncPoint);
        b.append("\n|Right Truncation Point=");
        b.append(rightTruncPoint);
        b.append("\n|FoxGlynn Flag F=");
        b.append(noUnderflow);
        b.append("\n|required accuracy=");
        b.append(requiredAccuracy);
        b.append("\n|------------------------");

        return b.toString();
    }
}