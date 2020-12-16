/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A marking, assigning a token count to each place of a Petri net.
 */
public final class Marking {

    private Map<String, Integer> map;

    /**
     * Builds an empty marking.
     */
    public Marking() {
        map = new HashMap<String, Integer>();
    }

    /**
     * Builds the copy of a marking.
     *
     * @param m input marking
     */
    public Marking(Marking m) {
        map = new HashMap<String, Integer>(m.map);
    }

    /**
     * Resets this marking to the token counts of the input one.
     *
     * @param m input marking
     */
    public void setTokensFrom(Marking m) {
        map = new HashMap<String, Integer>(m.map);
    }

    /**
     * Returns the number of tokens in a place.
     *
     * @param place the input place
     * @return token count of the place
     */
    public int getTokens(Place place) {
        return this.getTokens(place.toString());
    }

    /**
     * Returns the number of tokens of a place from its string name.
     *
     * @param placeName name of the place
     * @return number of tokens of the place
     */
    public int getTokens(String placeName) {
        if (map.containsKey(placeName))
            return map.get(placeName);
        else
            return 0;
    }

    /**
     * Sets the number of tokens of a place.
     *
     * @param place target place
     * @param tokens count of the place
     */
    public void setTokens(Place place, Integer tokens) {
        this.setTokens(place.toString(), tokens);
    }

    /**
     * Sets the number of tokens of a place from its string name.
     *
     * @param placeName name of the place
     * @param tokens number of tokens of the palce
     */
    private void setTokens(String placeName, Integer tokens) {

        if (tokens < 0)
            throw new IllegalArgumentException("Negative number of tokens");

        else if (tokens == 0)
            map.remove(placeName);

        else
            map.put(placeName, tokens);
    }

    /**
     * Adds a specified number of tokens in a place.
     *
     * @param place target place
     * @param number tokens to be added
     */
    public void addTokens(Place place, int number) {
        this.addTokens(place.toString(), number);
    }

    /**
     * Adds a specified number of tokens in a place from its string name.
     *
     * @param placeName name of the place
     * @param number tokens to be added
     */
    private void addTokens(String placeName, int number) {

        Integer tokens = map.get(placeName);

        if (tokens == null)
            map.put(placeName, number);
        else
            map.put(placeName, tokens + number);
    }

    /**
     * Removes a specified number of tokens from a place.
     *
     * @param place target place
     * @param number tokens to be removed
     */
    public void removeTokens(Place place, int number) {
        this.removeTokens(place.toString(), number);
    }

    /**
     * Removes a specified number of tokens from a place using its string name.
     *
     * @param placeName name of the place
     * @param number tokens to be removed
     */
    private void removeTokens(String placeName, int number) {

        Integer tokens = map.get(placeName);
        if (tokens == null || tokens < number)
            throw new IllegalArgumentException("Negative number of tokens");

        if (tokens == number)
            map.remove(placeName);
        else
            map.put(placeName, tokens - number);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof Marking))
            return false;

        Marking other = (Marking) obj;

        return this.map.equals(other.map);
    }

    @Override
    public String toString() {
        List<String> places = new ArrayList<String>(map.keySet());

        if (places.size() == 0)
            return "<empty>";

        Collections.sort(places, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1.length() < o2.length())
                    return -1;
                else if (o1.length() > o2.length())
                    return 1;
                else
                    return o1.compareTo(o2);
            }
        });

        StringBuilder b = new StringBuilder();
        for (String place : places) {
            int tokens = map.get(place);
            if (tokens > 0) {
                if (tokens != 1)
                    b.append(tokens);

                b.append(place);
                b.append(" ");
            }
        }

        return b.toString();
    }

    /**
     * Checks whether this marking contains the same token counts for a subset of
     * places.
     *
     * @param subMarking marking for a subset of places
     * @return true if this marking contains (as a subset) the same token
     *         assignments of the input one
     */
    public boolean containsSubMarking(Marking subMarking) {
        return this.map.entrySet().containsAll(subMarking.map.entrySet());
    }

    /**
     * Returns the names of places with nonzero token counts.
     *
     * @return a set of strings of place names
     */
    public Set<String> getNonEmptyPlacesNames() {
        return map.keySet();
    }
}
