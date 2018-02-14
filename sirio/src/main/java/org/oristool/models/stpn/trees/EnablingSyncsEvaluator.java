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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionProcessor;
import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.StateDensityFunction;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.tpn.TimedStateFeature;
import org.oristool.petrinet.Transition;

/**
 * Detector for generalized regenerations.
 */
public final class EnablingSyncsEvaluator implements SuccessionProcessor {

    /**
     * Detects whether the succession corresponds to a regeneration.
     *
     * <p>This method reads the {@link EnablingSyncsFeature} of the parent node to
     * decide whether, after the firing, all non-exponential transitions had
     * deterministic enabling times. If so, it adds a {@link Regeneration} feature
     * with an appropriate {@link DeterministicEnablingState}.
     *
     * <p>It also adds an {@code EnablingSyncsFeature} to the child node.
     *
     * @param succession input succession
     */
    @Override
    public Succession process(Succession succession) {

        // info on persistent, disabled, newly-enabled transitions
        PetriStateFeature nextPetriStateFeature = succession.getChild()
                .getFeature(PetriStateFeature.class);

        // current enabling times of GEN transitions
        Map<Variable, BigDecimal> enablingTimes = null;
        Variable firedVar = null;

        if (succession.getParent() == null) {
            if (succession.getChild().hasFeature(Regeneration.class)) {
                // the enabling times are given by the regeneration's
                // DeterministicEnablingState
                @SuppressWarnings("unchecked")
                Regeneration<DeterministicEnablingState> r = succession
                        .getChild().getFeature(Regeneration.class);
                enablingTimes = r.getValue().getEnablingTimes();
            } else {
                // the enabling time is zero for every GEN, and the state is a
                // regeneration
                DeterministicEnablingState r = new DeterministicEnablingState(
                        nextPetriStateFeature.getMarking(),
                        nextPetriStateFeature.getEnabled());

                succession.getChild().addFeature(
                        new Regeneration<DeterministicEnablingState>(r));
                enablingTimes = r.getEnablingTimes();
            }

        } else if (succession.getParent() != null) {
            // this class should be marked as regenerative if all the persistent
            // GENs
            // are enabled by a deterministic time, i.e. they are in the delay
            // set of the fired DET
            boolean isRegenerative = true;

            enablingTimes = new LinkedHashMap<Variable, BigDecimal>();
            firedVar = new Variable(succession.getEvent().getName());

            EnablingSyncsFeature prevEnablingSyncs = succession.getParent()
                    .getFeature(EnablingSyncsFeature.class);
            boolean detFiring = prevEnablingSyncs.dets().contains(firedVar);

            // regenerative class check
            for (Transition t : nextPetriStateFeature.getPersistent()) {
                Variable v = new Variable(t.getName());

                if (isEXP(t, succession.getChild())) {
                    // nothing to do: EXPs do not affect enablingTimes nor
                    // isRegenerative

                } else if (detFiring
                        && prevEnablingSyncs.get(firedVar, v) != null) {
                    // a statically-DET transition fired, and v was renewed at
                    // its enabling:
                    // hence v is now a persistent GEN enabled by a
                    // deterministic time
                    enablingTimes.put(v, prevEnablingSyncs.get(firedVar, v));

                } else {
                    // persistent GEN enabled by a non-deterministic time
                    isRegenerative = false;
                }
            }

            // add newly enabled GEN to the map
            for (Transition t : nextPetriStateFeature.getNewlyEnabled())
                if (!isEXP(t, succession.getChild()))
                    enablingTimes.put(new Variable(t.getName()),
                            BigDecimal.ZERO);

            if (isRegenerative && !hasIMM(succession.getChild()))
                // the regeneration value is the marking and enabling times of
                // GENs
                succession.getChild().addFeature(
                        new Regeneration<DeterministicEnablingState>(
                                new DeterministicEnablingState(
                                        nextPetriStateFeature.getMarking(),
                                        enablingTimes)));
        }

        // Update the enabling sync information of the previous state
        EnablingSyncsFeature nextEnablingSyncs;
        if (succession.getParent() != null)
            nextEnablingSyncs = new EnablingSyncsFeature(succession.getParent()
                    .getFeature(EnablingSyncsFeature.class));
        else
            nextEnablingSyncs = new EnablingSyncsFeature();

        // (1) remove all the disabled transitions (and the fired one)
        for (Transition t : nextPetriStateFeature.getDisabled())
            nextEnablingSyncs.remove(new Variable(t.getName()));

        if (firedVar != null)
            nextEnablingSyncs.remove(firedVar);

        // (2) add the renewed transitions (i.e., those with deterministic
        // enabling time)
        // to the synchronizing set of the deterministic variables
        for (Variable det : getDeterministicVariables(succession.getChild())) {
            if (!det.equals(Variable.AGE)) {
                BigDecimal detValue = getDeterministicValue(det,
                        succession.getChild());

                for (Variable gen : enablingTimes.keySet()) {
                    if (!gen.equals(det)) {
                        BigDecimal enablingTimeAtFiring = enablingTimes
                                .get(gen).add(detValue);
                        // TODO avoid adding 'enabling times at firing' if the
                        // firing will
                        // necessarily happen after the GEN's one
                        nextEnablingSyncs.set(det, gen, enablingTimeAtFiring);
                    }
                }
            }
        }

        succession.getChild().addFeature(nextEnablingSyncs);
        return succession;
    }

    private BigDecimal getDeterministicValue(Variable v, State s) {

        if (s.hasFeature(StochasticStateFeature.class)) {
            // use stochastic information
            return s.getFeature(StochasticStateFeature.class).getStateDensity()
                    .getDeterministicValue(v);

        } else {
            // use non-deterministic information
            DBMZone nextDomain = s.getFeature(TimedStateFeature.class).getDomain();
            return nextDomain.getBound(v, Variable.TSTAR).bigDecimalValue();
        }
    }

    private Set<Variable> getDeterministicVariables(State s) {

        if (s.hasFeature(StochasticStateFeature.class)) {
            // use stochastic information
            return s.getFeature(StochasticStateFeature.class).getStateDensity()
                    .getDeterministicVariables();

        } else {
            // use non-deterministic information
            DBMZone nextDomain = s.getFeature(TimedStateFeature.class)
                    .getDomain();

            Set<Variable> dets = new LinkedHashSet<Variable>();
            for (Variable v : nextDomain.getVariables())
                if (!v.equals(Variable.TSTAR)
                        && nextDomain.getBound(v, Variable.TSTAR)
                                .compareTo(nextDomain.getBound(Variable.TSTAR, v).negate()) == 0)
                    dets.add(v);

            return dets;
        }
    }

    private boolean hasIMM(State s) {
        if (s.hasFeature(StochasticStateFeature.class)) {
            // use stochastic information
            StateDensityFunction f = s.getFeature(StochasticStateFeature.class).getStateDensity();
            for (Variable v : f.getDeterministicVariables())
                if (f.getDeterministicValue(v).compareTo(BigDecimal.ZERO) == 0)
                    return true;

            return false;

        } else {
            // use non-deterministic information
            DBMZone nextDomain = s.getFeature(TimedStateFeature.class)
                    .getDomain();

            for (Variable v : nextDomain.getVariables())
                if (!v.equals(Variable.TSTAR)
                        && nextDomain.getBound(v, Variable.TSTAR)
                            .compareTo(OmegaBigDecimal.ZERO) == 0
                        && nextDomain.getBound(Variable.TSTAR, v)
                            .compareTo(OmegaBigDecimal.ZERO) == 0)
                    return true;

            return false;
        }
    }

    private boolean isEXP(Transition t, State s) {

        Variable v = new Variable(t.getName());

        if (s.hasFeature(StochasticStateFeature.class)) {
            // use stochastic information
            return s.getFeature(StochasticStateFeature.class).getEXPVariables()
                    .contains(v);

        } else {
            // use non-deterministic information
            DBMZone nextDomain = s.getFeature(TimedStateFeature.class)
                    .getDomain();

            boolean isZeroInf = nextDomain.getBound(v, Variable.TSTAR)
                    .compareTo(OmegaBigDecimal.POSITIVE_INFINITY) == 0
                    && nextDomain.getBound(Variable.TSTAR, v)
                    .compareTo(OmegaBigDecimal.ZERO) == 0;

            return isZeroInf
                    && t.hasFeature(StochasticTransitionFeature.class)
                    && t.getFeature(StochasticTransitionFeature.class)
                        .getFiringTimeDensity().getDensities().size() == 1
                    && t.getFeature(StochasticTransitionFeature.class)
                        .getFiringTimeDensity().getDensities().get(0).isExponential();
        }
    }
}
