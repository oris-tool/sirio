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

package org.oristool.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base implementation of a generic featurizable element.
 *
 * @param <E> the type of allowed features
 * @see Feature
 */
public abstract class Featurizable<E extends Feature> {

    protected Map<Class<? extends E>, E> features = new LinkedHashMap<Class<? extends E>, E>();

    /**
     * Adds a feature to this featurizable object.
     *
     * @param feature the feature to be added
     * @throws IllegalArgumentException if another feature of the same type is
     *         already present
     */
    @SuppressWarnings("unchecked")
    public void addFeature(E feature) {
        if (features.containsKey(feature.getClass())) {
            throw new IllegalArgumentException("Feature type already present");
        }
        
        features.put((Class<? extends E>) feature.getClass(), feature);
    }

    /**
     * Returns the collection of features associated with this object.
     *
     * @return a collection of features
     */
    public Collection<E> getFeatures() {
        return Collections.unmodifiableCollection(features.values());
    }

    /**
     * Returns the feature associated with this object for a given type.
     *
     * @param typeToken type of feature to be returned
     * @return instance of the associated feature, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <F extends E> F getFeature(Class<F> typeToken) {

        return (F) features.get(typeToken);
    }

    /**
     * Returns true if this object has a feature with the given type.
     *
     * @param typeToken type of feature to be checked
     */
    public <F extends E> boolean hasFeature(Class<F> typeToken) {
        return features.containsKey(typeToken);
    }

    /**
     * Removes the feature with the given type from this object.
     *
     * @param typeToken type of the feature to be removed
     */
    public <F extends E> void removeFeature(Class<F> typeToken) {
        features.remove(typeToken);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        for (E e : features.values()) {
            b.append(e.toString());
            b.append('\n');
        }

        return b.toString();
    }
}
