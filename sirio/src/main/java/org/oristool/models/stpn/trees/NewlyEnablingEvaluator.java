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

package org.oristool.models.stpn.trees;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Variable;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.tpn.TimedStateFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.Transition;

/**
 * Detector for standard regenerations.
 */
public class NewlyEnablingEvaluator implements SuccessionProcessor {

    @Override
    public Succession process(Succession succession) {

        PetriStateFeature nextPetriStateFeature = succession.getChild()
                .getFeature(PetriStateFeature.class);

        boolean isRegenerative = true;

        if (succession.getChild().hasFeature(StochasticStateFeature.class)) {
            // use stochastic information
            StochasticStateFeature nextStochasticStateFeature = succession
                    .getChild().getFeature(StochasticStateFeature.class);

            if (nextStochasticStateFeature.isVanishing()) {
                // vanishing classes are never regenerative
                isRegenerative = false;

            } else {

                // regenerative class check
                for (Transition t : nextPetriStateFeature.getPersistent()) {
                    if (!nextStochasticStateFeature.getEXPVariables().contains(
                            new Variable(t.getName()))) {
                        isRegenerative = false;
                        break;
                    }
                }
            }

        } else {
            // use non-deterministic information
            DBMZone nextDomain = succession.getChild()
                    .getFeature(TimedStateFeature.class).getDomain();

            // TODO add vanishing exclusion

            // regenerative class check
            for (Transition t : nextPetriStateFeature.getPersistent()) {
                Variable tVar = new Variable(t.toString());
                boolean isZeroInf = nextDomain.getBound(tVar, Variable.TSTAR)
                        .equals(OmegaBigDecimal.POSITIVE_INFINITY)
                        && nextDomain.getBound(Variable.TSTAR, tVar).equals(
                                OmegaBigDecimal.ZERO);

                if (!isZeroInf
                        || !t.hasFeature(StochasticTransitionFeature.class)
                        || !(t.getFeature(StochasticTransitionFeature.class)
                                .getFiringTimeDensity().getDensities().size() == 1)
                        || !(t.getFeature(StochasticTransitionFeature.class)
                                .getFiringTimeDensity().getDensities().get(0).isExponential())) {

                    isRegenerative = false;
                    break;
                }
            }
        }

        if (isRegenerative)
            // the regeneration value is the marking
            succession.getChild().addFeature(
                    new Regeneration<Marking>(nextPetriStateFeature
                            .getMarking()));

        return succession;
    }
}
