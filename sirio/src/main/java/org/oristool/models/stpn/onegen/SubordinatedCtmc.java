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

package org.oristool.models.stpn.onegen;

import java.math.BigDecimal;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateFeature;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.models.gspn.RateExpressionFeature;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

class SubordinatedCtmc {
    private PetriNet pn;
    private SuccessionGraph ctmc;
    private Transition subordinatingGen;
    private State rootSink;
    private BigDecimal rootRateSum;

    public SubordinatedCtmc(State root, PetriNet pn) {
        super();
        this.pn = pn;
        this.subordinatingGen = null;
        this.rootSink = null;
        this.rootRateSum = BigDecimal.ZERO;

        this.ctmc = new SuccessionGraph();
        this.ctmc.addSuccession(new Succession(null, null, root));
    }

    public SuccessionGraph getGraph() {
        return ctmc;
    }

    public boolean addSuccession(Succession s) {
        State root = ctmc.getState(ctmc.getRoot());
        if (s.getParent().equals(root)) {
            Transition t = (Transition) s.getEvent();
            Marking parentMarking = s.getParent().getFeature(PetriStateFeature.class).getMarking();
            if (Utils.isExponential(t)) {
                BigDecimal exponentialRate = new BigDecimal(
                        t.getFeature(RateExpressionFeature.class).getRate(pn, parentMarking));
                rootRateSum = rootRateSum.add(exponentialRate);
            }
        }
        if (s.getChild().equals(root)) {
            if (null == rootSink) {
                rootSink = new State();
                for (StateFeature f : root.getFeatures()) {
                    rootSink.addFeature(f);
                }

                // add a fake feature to distinguish rootSink from root
                rootSink.addFeature(new StateFeature() {
                });
            }

            ctmc.addSuccession(new Succession(s.getParent(), s.getEvent(), rootSink));
            return false;
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
            return subordinatingGen.getFeature(StochasticTransitionFeature.class)
                    .getFiringTimeDensity();
        }
        return null;
    }

    public void setSubordinatingGen(Transition gen) {
        if (gen.hasFeature(StochasticTransitionFeature.class)) {

            if (Utils.isExponential(gen)) {
                throw new IllegalArgumentException("GEN transition is actually EXP");
            } else {

                if (this.subordinatingGen != null) {
                    if (!this.subordinatingGen.getName().equals(gen.getName())) {
                        // FIXME quando si avrà il metodo per eseguire l'analisi
                        // con più GEN
                        throw new IllegalStateException("Two GEN found: "
                                + this.subordinatingGen.getName() + " and " + gen.getName());
                    }
                } else {
                    this.subordinatingGen = gen;
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "GEN transition without stochastic transition features");
        }
    }
}