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

package org.oristool.simulator.samplers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A pseudo-sampler cycling through a fixed sequence of sample (from a random
 * initial point).
 */
public final class PseudoSampler implements Sampler {

    private final List<BigDecimal> samples;
    private int nextSamplePos;

    /**
     * Builds a new instance using the input samples.
     *
     * @param samples input samples
     */
    public PseudoSampler(BigDecimal... samples) {
        this.samples = Arrays.asList(samples);
        this.nextSamplePos = (int) (Math.random() * (double) this.samples.size());
    }

    /**
     * Builds a new instance using samples from a file.
     *
     * @param filename file with one sample per line
     * @param initCapacity initial capacity of the list
     */
    public PseudoSampler(String filename, int initCapacity) {
        samples = new ArrayList<BigDecimal>(initCapacity);
        try {
            BufferedReader f = new BufferedReader(new FileReader(filename));
            String s;
            while ((s = f.readLine()) != null)
                samples.add(new BigDecimal(s));
            f.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("File IO not valid");
        }

        this.nextSamplePos = (int) (Math.random() * (double) samples.size());
    }

    @Override
    public BigDecimal getSample() {
        BigDecimal sample = samples.get(nextSamplePos);
        nextSamplePos = (nextSamplePos + 1) % samples.size();
        return sample;
    }
}
