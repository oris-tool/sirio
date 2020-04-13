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

package org.oristool.models.stpn.trees;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.oristool.analyzer.state.State;
import org.oristool.analyzer.state.StateBuilder;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Variable;
import org.oristool.models.pn.InitialPetriStateBuilder;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.tpn.TimedStateFeature;
import org.oristool.models.tpn.TimedTransitionFeature;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * State builder for time Petri nets using deterministic enabling times.
 */
public final class DeterministicEnablingTimedStateBuilder implements
        StateBuilder<DeterministicEnablingState> {

    private final PetriNet petriNet;
    private final boolean transientAnalysis;

    /**
     * Configures a state builder for TPNs.
     *
     * @param petriNet Petri net
     * @param transientAnalysis whether the state should include
     *        {@code Variable.AGE}
     */
    public DeterministicEnablingTimedStateBuilder(PetriNet petriNet,
            boolean transientAnalysis) {

        this.petriNet = petriNet;
        this.transientAnalysis = transientAnalysis;
    }

    @Override
    public State build(DeterministicEnablingState s) {

        // adds the petri state feature
        State state = InitialPetriStateBuilder.computeInitialState(petriNet,
                s.getMarking(), false);
        Set<Transition> enabledTransitions = state.getFeature(
                PetriStateFeature.class).getNewlyEnabled();

        // adds a TimedFeature to STPNs
        for (Transition t : petriNet.getTransitions())
            if (!t.hasFeature(TimedTransitionFeature.class)
                    && t.hasFeature(StochasticTransitionFeature.class))
                t.addFeature(t.getFeature(StochasticTransitionFeature.class)
                        .asTimedTransitionFeature());

        // builds the timed state feature
        Set<Variable> enabledVariables = Transition
                .newVariableSetInstance(enabledTransitions);
        if (transientAnalysis)
            enabledVariables.add(Variable.AGE);

        DBMZone domain = new DBMZone(enabledVariables);
        Map<Variable, BigDecimal> enablingTimes = s.getEnablingTimes();

        for (Transition t : enabledTransitions) {
            Variable v = new Variable(t.getName());

            TimedTransitionFeature ttf = t
                    .getFeature(TimedTransitionFeature.class);
            OmegaBigDecimal eft = ttf.getEFT();
            OmegaBigDecimal lft = ttf.getLFT();

            if (enablingTimes.containsKey(v)) {
                eft.subtract(new OmegaBigDecimal(enablingTimes.get(v))).max(
                        OmegaBigDecimal.ZERO);
                lft.subtract(new OmegaBigDecimal(enablingTimes.get(v)));

            } else if (eft.compareTo(OmegaBigDecimal.ZERO) != 0
                    || lft.compareTo(OmegaBigDecimal.POSITIVE_INFINITY) != 0)
                // has memory! this is not a regeneration...
                throw new IllegalArgumentException("The GEN transition " + v
                        + " does not have a deterministic enabling time");

            domain.setCoefficient(t.newVariableInstance(), Variable.TSTAR, lft);
            domain.setCoefficient(Variable.TSTAR, t.newVariableInstance(),
                    eft.negate());
        }

        if (transientAnalysis) {
            domain.setCoefficient(Variable.AGE, Variable.TSTAR,
                    OmegaBigDecimal.ZERO);
            domain.setCoefficient(Variable.TSTAR, Variable.AGE,
                    OmegaBigDecimal.ZERO);
        }

        // adds the timed state feature
        TimedStateFeature tsf = new TimedStateFeature();
        tsf.setDomain(domain);
        state.addFeature(tsf);

        // adds the regeneration object
        state.addFeature(new Regeneration<DeterministicEnablingState>(s));

        return state;
    }
}
