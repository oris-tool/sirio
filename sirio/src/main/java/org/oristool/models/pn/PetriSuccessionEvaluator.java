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

package org.oristool.models.pn;

import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.analyzer.Succession;
import org.oristool.analyzer.SuccessionEvaluator;
import org.oristool.analyzer.state.State;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;

/**
 * Builder of successor states for Petri nets.
 */
public final class PetriSuccessionEvaluator implements
        SuccessionEvaluator<PetriNet, Transition> {

    private final MarkingUpdater tokensRemover;
    private final MarkingUpdater tokensAdder;
    private final boolean checkNewlyEnabled;

    public PetriSuccessionEvaluator() {

        this(new PetriTokensRemover(), new PetriTokensAdder(), false);
    }

    /**
     * Builds a succession evaluator with a given token remover and adder.
     *
     * <p>If {@code checkNewlyEnabled} is true, states with the same marking are
     * considered different if they have a different set of newly-enabled
     * transitions.
     *
     * @param tokensRemover the object used to add tokens after a firing
     * @param tokensAdder the object used to remove tokens after a firing
     * @param checkNewlyEnabled whether to compare the sets of newly-enabled
     *        transitions of states
     */
    public PetriSuccessionEvaluator(MarkingUpdater tokensRemover,
            MarkingUpdater tokensAdder, boolean checkNewlyEnabled) {

        this.tokensRemover = tokensRemover;
        this.tokensAdder = tokensAdder;
        this.checkNewlyEnabled = checkNewlyEnabled;
    }

    @Override
    public Succession computeSuccession(PetriNet petriNet, State state,
            Transition fired) {

        PetriStateFeature prev = state.getFeature(PetriStateFeature.class);

        Marking tmpMarking = new Marking(prev.getMarking());
        tokensRemover.update(tmpMarking, petriNet, fired);

        Marking nextMarking = new Marking(tmpMarking);
        tokensAdder.update(nextMarking, petriNet, fired);

        // performs additional token moves if requested
        if (fired.hasFeature(PostUpdater.class))
            fired.getFeature(PostUpdater.class).update(nextMarking, petriNet,
                    fired);

        Set<Transition> prevEnabled = petriNet.getEnabledTransitions(prev
                .getMarking());
        Set<Transition> tmpEnabled = petriNet.getEnabledTransitions(tmpMarking);
        Set<Transition> nextEnabled = petriNet
                .getEnabledTransitions(nextMarking);

        // the reset set alters the intermediate enabling
        if (fired.hasFeature(ResetSet.class))
            tmpEnabled
                    .removeAll(fired.getFeature(ResetSet.class).getResetSet());

        Set<Transition> persistent = new LinkedHashSet<Transition>(nextEnabled);
        persistent.retainAll(tmpEnabled);
        persistent.retainAll(prevEnabled);
        persistent.remove(fired);

        Set<Transition> newlyEnabled = new LinkedHashSet<Transition>(
                nextEnabled);
        newlyEnabled.removeAll(persistent);

        Set<Transition> prevAndNotNext = new LinkedHashSet<Transition>(
                prevEnabled);
        prevAndNotNext.removeAll(nextEnabled);
        Set<Transition> prevAndNew = new LinkedHashSet<Transition>(prevEnabled);
        prevAndNew.retainAll(newlyEnabled);
        Set<Transition> disabled = new LinkedHashSet<Transition>(prevAndNotNext);
        disabled.addAll(prevAndNew);
        disabled.remove(fired);

        PetriStateFeature next = new PetriStateFeature();
        next.setMarking(nextMarking);
        next.setPersistent(persistent);
        next.setNewlyEnabled(newlyEnabled);
        next.setEnabled(nextEnabled);
        next.setDisabled(disabled);
        next.setCheckNewlyEnabled(checkNewlyEnabled);

        State nextState = new State();
        nextState.addFeature(next);

        return new Succession(state, fired, nextState);
    }
}
