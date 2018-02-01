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

package org.oristool.petrinet;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.oristool.util.Featurizable;

/**
 * A Petri net model.
 */
public final class PetriNet extends Featurizable<PetriNetFeature> {

    private final Map<String, Place> places = 
            new LinkedHashMap<>();
    private final Map<String, Transition> transitions = 
            new LinkedHashMap<>();
    private final Map<Transition, Map<Place, Precondition>> preconditions = 
            new LinkedHashMap<>();
    private Map<Transition, Map<Place, Postcondition>> postconditions = 
            new LinkedHashMap<>();
    private Map<Transition, Map<Place, InhibitorArc>> inhibitorArcs = 
            new LinkedHashMap<>();

    /**
     * Adds a place to the Petri net.
     * 
     * <p>If a place with the same name already exists, the existing place is
     * returned.
     * 
     * @param placeName the name of the place
     * @return the place instance
     */
    public Place addPlace(String placeName) {

        Place p = places.get(placeName);
        if (p == null) {
            p = new Place(placeName);
            places.put(placeName, p);
        }

        return p;
    }

    /**
     * Gets an existing place with the given name.
     * 
     * @param placeName name of the place
     * @return place instance, or {@code null} if no place exists with the given
     *         name
     */
    public Place getPlace(String placeName) {
        return places.get(placeName);
    }

    /**
     * Removes an existing place.
     * 
     * @param p place to be removed
     * @return the removed place
     */
    public Place removePlace(Place p) {
        return places.remove(p.toString());
    }

    /**
     * Returns the places of the Petri net.
     * 
     * @return collection of places
     */
    public Collection<Place> getPlaces() {
        return Collections.unmodifiableCollection(places.values());
    }

    /**
     * Returns the names of the places in the Petri net.
     * 
     * @return names of the places
     */
    public Collection<String> getPlaceNames() {

        return Collections.unmodifiableCollection(places.keySet());
    }

    /**
     * Adds a transition to the Petri net.
     * 
     * <p>If a transition with the same name already exists, the existing transition
     * is returned.
     * 
     * @param transitionName the name of the transition
     * @return the transition instance
     */
    public Transition addTransition(String transitionName) {

        Transition t = transitions.get(transitionName);
        if (t == null) {
            t = new Transition(transitionName);
            transitions.put(transitionName, t);
        }

        return t;
    }

    /**
     * Gets an existing transition with the given name.
     * 
     * @param transitionName name of the transition
     * @return transition instance, or {@code null} if no transition exists with the
     *         given name
     */
    public Transition getTransition(String transitionName) {
        return transitions.get(transitionName);
    }

    /**
     * Removes an existing transition.
     * 
     * @param t transition to be removed
     * @return the removed transition
     */
    public Transition removeTransition(Transition t) {
        return transitions.remove(t.toString());
    }

    /**
     * Returns the transitions of the Petri net.
     * 
     * @return collection of transitions
     */
    public Collection<Transition> getTransitions() {
        return Collections.unmodifiableCollection(transitions.values());
    }

    /**
     * Returns the names of the transitions in the Petri net.
     * 
     * @return names of the transitions
     */
    public Collection<String> getTransitionNames() {

        return Collections.unmodifiableCollection(transitions.keySet());
    }

    /**
     * Adds a precondition with multiplicity 1.
     * 
     * @param p place
     * @param t transition
     * @return the added precondition
     */
    public Precondition addPrecondition(Place p, Transition t) {
        return this.addPrecondition(p, t, 1);
    }

    /**
     * Adds a precondition with given multiplicity.
     * 
     * @param p input place
     * @param t target transition
     * @param multiplicity precondition multiplicity 
     * @return the added precondition
     */
    public Precondition addPrecondition(Place p, Transition t, int multiplicity) {

        Map<Place, Precondition> transitionPreconditions = preconditions.get(t);
        if (transitionPreconditions == null) {
            transitionPreconditions = new LinkedHashMap<Place, Precondition>();
            preconditions.put(t, transitionPreconditions);
        }

        Precondition pre = transitionPreconditions.get(p);
        if (pre == null) {
            pre = new Precondition(p, t, multiplicity);
            transitionPreconditions.put(p, pre);
        }

        return pre;
    }

    /**
     * Returns the existing precondition between a place and a transition.
     * 
     * @param p input place
     * @param t target transition
     * @return precondition between the place and the transition, or {@code null} if
     *         no precondition exists
     */
    public Precondition getPrecondition(Place p, Transition t) {

        Map<Place, Precondition> transitionPreconditions = preconditions.get(t);
        if (transitionPreconditions == null)
            return null;
        else
            return transitionPreconditions.get(p);
    }

    /**
     * Removes the existing precondition between a place and a transition.
     * 
     * @param pre existing precondition
     * @return precondition between the place and the transition
     */
    public Precondition removePrecondition(Precondition pre) {

        return preconditions.get(pre.getTransition()).remove(pre.getPlace());
    }

    /**
     * Gets all the preconditions of a transition.
     * 
     * @param t transition
     * @return preconditions of the transition
     */
    public Collection<Precondition> getPreconditions(Transition t) {

        Map<Place, Precondition> transitionPreconditions = preconditions.get(t);
        if (transitionPreconditions == null)
            return Collections.unmodifiableCollection(Collections.emptySet());
        else
            return Collections.unmodifiableCollection(preconditions.get(t)
                    .values());
    }

    /**
     * Adds an inhibitor arc with multiplicity 1.
     * 
     * @param p place
     * @param t transition
     * @return the added inhibitor arc
     */
    public InhibitorArc addInhibitorArc(Place p, Transition t) {
        return this.addInhibitorArc(p, t, 1);
    }

    /**
     * Adds an inhibitor arc with given multiplicity.
     * 
     * @param p input place
     * @param t target transition
     * @param multiplicity inhibitor arc multiplicity 
     * @return the added inhibitor arc
     */
    public InhibitorArc addInhibitorArc(Place p, Transition t, int multiplicity) {

        Map<Place, InhibitorArc> transitionInhibitorArcs = inhibitorArcs.get(t);
        if (transitionInhibitorArcs == null) {
            transitionInhibitorArcs = new LinkedHashMap<Place, InhibitorArc>();
            inhibitorArcs.put(t, transitionInhibitorArcs);
        }

        InhibitorArc ia = transitionInhibitorArcs.get(p);
        if (ia == null) {
            ia = new InhibitorArc(p, t, multiplicity);
            transitionInhibitorArcs.put(p, ia);
        }

        return ia;
    }

    /**
     * Returns the existing inhibitor arc between a place and a transition.
     * 
     * @param p input place
     * @param t target transition
     * @return inhibitor arc between the place and the transition, or {@code null} if
     *         inhibitor arc exists
     */
    public InhibitorArc getInhibitorArc(Place p, Transition t) {

        Map<Place, InhibitorArc> transitionInhibitorArcs = inhibitorArcs.get(t);
        if (transitionInhibitorArcs == null)
            return null;
        else
            return transitionInhibitorArcs.get(p);
    }

    /**
     * Removes the existing inhibitor arc between a place and a transition.
     * 
     * @param ia existing inhibitor arc
     * @return inhibitor arc between the place and the transition
     */
    public InhibitorArc removeInhibitorArc(InhibitorArc ia) {

        return inhibitorArcs.get(ia.getTransition()).remove(ia.getPlace());
    }

    /**
    * Gets all the inhibitor arcs of a transition.
    * 
    * @param t transition
    * @return inhibitor arcs of the transition
    */
    public Collection<InhibitorArc> getInhibitorArcs(Transition t) {

        Map<Place, InhibitorArc> transitionInhibitorArcs = inhibitorArcs.get(t);
        if (transitionInhibitorArcs == null)
            return Collections.unmodifiableCollection(Collections.emptySet());
        else
            return Collections.unmodifiableCollection(inhibitorArcs.get(t)
                    .values());
    }

    /**
     * Adds a postcondition with multiplicity 1.
     * 
     * @param p place
     * @param t transition
     * @return the added postcondition
     */
    public Postcondition addPostcondition(Transition t, Place p) {
        return this.addPostcondition(t, p, 1);
    }

    /**
     * Adds a postcondition with given multiplicity.
     * 
     * @param p input place
     * @param t target transition
     * @param multiplicity postcondition multiplicity 
     * @return the added postcondition
     */
    public Postcondition addPostcondition(Transition t, Place p,
            int multiplicity) {

        Map<Place, Postcondition> transitionPostconditions = postconditions
                .get(t);
        if (transitionPostconditions == null) {
            transitionPostconditions = new LinkedHashMap<Place, Postcondition>();
            postconditions.put(t, transitionPostconditions);
        }

        Postcondition post = transitionPostconditions.get(p);
        if (post == null) {
            post = new Postcondition(t, p, multiplicity);
            transitionPostconditions.put(p, post);
        }

        return post;
    }

    /**
     * Returns the existing postcondition between a place and a transition.
     * 
     * @param p input place
     * @param t target transition
     * @return postcondition between the place and the transition, or {@code null} if
     *         no postcondition exists
     */
    public Postcondition getPostcondition(Transition t, Place p) {

        Map<Place, Postcondition> transitionPostconditions = postconditions
                .get(t);
        if (transitionPostconditions == null)
            return null;
        else
            return transitionPostconditions.get(p);
    }

    /**
     * Removes the existing postcondition between a place and a transition.
     * 
     * @param post existing postcondition
     * @return postcondition between the place and the transition
     */
    public Postcondition removePostcondition(Postcondition post) {

        return postconditions.get(post.getTransition()).remove(post.getPlace());
    }

    /**
     * Gets all the postconditions of a transition.
     * 
     * @param t transition
     * @return postconditions of the transition
     */
    public Collection<Postcondition> getPostconditions(Transition t) {

        Map<Place, Postcondition> transitionPostconditions = postconditions.get(t);
        if (transitionPostconditions == null)
            return Collections.unmodifiableCollection(Collections.emptySet());
        else
            return Collections.unmodifiableCollection(postconditions.get(t)
                    .values());
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("> Places\n");
        for (Place p : this.places.values())
            b.append(p + "\n");

        b.append("> Transitions\n");
        for (Transition t : this.transitions.values())
            b.append(t + "\n");

        b.append("> Preconditions\n");
        for (Transition t : preconditions.keySet())
            for (Precondition pre : this.preconditions.get(t).values())
                b.append(pre + "\n");

        b.append("> Postconditions\n");
        for (Transition t : postconditions.keySet())
            for (Postcondition post : this.postconditions.get(t).values())
                b.append(post + "\n");
        
        b.append("> Inhibitor\n");
        for (Transition t : inhibitorArcs.keySet())
            for (InhibitorArc inhibitor : this.inhibitorArcs.get(t).values())
                b.append(inhibitor + "\n");

        b.append(super.toString());

        return b.toString();
    }

    /**
     * Returns the set of transitions enabled by a given marking.
     * 
     * @param marking any marking
     * @return set of transitions enabled by the marking 
     */
    public Set<Transition> getEnabledTransitions(Marking marking) {

        Set<Transition> enabled = new LinkedHashSet<Transition>();

        for (Transition t : this.getTransitions()) {
            boolean isEnabled = true;

            for (Precondition pc : this.getPreconditions(t))
                if (marking.getTokens(pc.getPlace()) < pc.getMultiplicity()) {
                    isEnabled = false;
                    break;
                }

            if (isEnabled)
                for (InhibitorArc ia : this.getInhibitorArcs(t))
                    if (marking.getTokens(ia.getPlace()) >= ia
                            .getMultiplicity()) {
                        isEnabled = false;
                        break;
                    }

            EnablingFunction ec = t.getFeature(EnablingFunction.class);
            if (isEnabled && ec != null)
                isEnabled = ec.getMarkingCondition().evaluate(marking);

            if (isEnabled)
                enabled.add(t);
        }

        return enabled;
    }
}
