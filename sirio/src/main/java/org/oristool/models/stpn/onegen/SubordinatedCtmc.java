/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateFeature;
import org.oristool.math.function.EXP;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.Transition;

class SubordinatedCtmc {
    private final SuccessionGraph ctmc;
    private final State rootSink;
    private final State root;
    private Transition subordinatingGen;
    private BigDecimal rootRateSum;

    public SubordinatedCtmc(State root) {

        this.rootRateSum = BigDecimal.ZERO;

        this.ctmc = new SuccessionGraph();
        this.ctmc.addSuccession(new Succession(null, null, root));
        this.root = ctmc.getState(ctmc.getRoot());
        this.rootSink = new State();
        for (StateFeature f : root.getFeatures()) {
            rootSink.addFeature(f);
        }

        // add a fake feature to distinguish rootSink from root
        rootSink.addFeature(new StateFeature() { });
    }

    public SuccessionGraph getGraph() {
        return ctmc;
    }

    public boolean addSuccession(Succession s) {

        if (s.getParent().equals(root)) {
            // update outgoing rate from root
            Transition t = (Transition) s.getEvent();
            if (Utils.isExponential(t)) {
                Marking parentMarking = root.getFeature(PetriStateFeature.class).getMarking();
                StochasticTransitionFeature f = t.getFeature(StochasticTransitionFeature.class);
                BigDecimal rate = ((EXP)f.density()).getLambda();
                BigDecimal clockRate = new BigDecimal(f.clockRate().evaluate(parentMarking));
                rootRateSum = rootRateSum.add(rate.multiply(clockRate));
            }
        }

        if (s.getChild().equals(root)) {
            return ctmc.addSuccession(new Succession(s.getParent(), s.getEvent(), rootSink));
        } else {
            return ctmc.addSuccession(s);
        }
    }

    public boolean hasRootLoop() {
        return rootSink != null;
    }

    public State getRootSink() {
        return rootSink;
    }

    public BigDecimal getRootRateSum() {
        return rootRateSum;
    }

    public boolean hasSubordinatingGen() {
        return subordinatingGen != null;
    }

    public PartitionedFunction getSubordinatingGenPdf() {
        if (hasSubordinatingGen()) {
            return subordinatingGen.getFeature(StochasticTransitionFeature.class).density();
        }

        return null;
    }

    public void setSubordinatingGen(Transition gen) {

        if (!gen.hasFeature(StochasticTransitionFeature.class))
            throw new IllegalArgumentException(
                    "GEN transition without stochastic transition features");

        if (Utils.isExponential(gen))
            throw new IllegalArgumentException("GEN transition is actually EXP");

        if (this.subordinatingGen != null
                && !this.subordinatingGen.getName().equals(gen.getName()))
            throw new IllegalStateException("Two GEN found: "
                    + this.subordinatingGen.getName() + " and " + gen.getName());

        this.subordinatingGen = gen;
    }
}