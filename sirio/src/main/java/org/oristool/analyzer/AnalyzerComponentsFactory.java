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

package org.oristool.analyzer;

import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.stop.StopCriterion;

/**
 * Factory for all the components used by the analyzer.
 *  
 * @param <M> type of the model (such as a {@link PetriNet}
 * @param <E> type of the event (such as the firing of a {@link Transition}
 */
public interface AnalyzerComponentsFactory<M, E extends Event> {

    /**
     * Returns the policy selecting the next event to explore.
     *  
     * @return policy used by the analyzer 
     */
    EnumerationPolicy getEnumerationPolicy();

    /**
     * Returns the builder for the events enabled in a state.
     *  
     * @return enabled events builder used by the analyzer 
     */
    EnabledEventsBuilder<M, E> getEnabledEventsBuilder();

    /**
     * Returns the builder for the successor states after an event.
     *  
     * @return succession evaluator used by the analyzer  
     */
    SuccessionEvaluator<M, E> getSuccessionEvaluator();

    /**
     * Returns the pre-processor used before computing the enabled events.
     *  
     * @return pre-processor used by the analyzer  
     */
    SuccessionProcessor getPreProcessor();

    /**
     * Returns the post-processor used after computing a successor state.
     *  
     * @return post-processor used by the analyzer  
     */
    SuccessionProcessor getPostProcessor();

    /**
     * Returns the criterion to arrest the enumeration on a specific node.
     *  
     * @return local criterion used by the analyzer  
     */
    StopCriterion getLocalStopCriterion();

    /**
     * Returns the criterion to arrest the entire graph enumeration.
     *  
     * @return global criterion used by the analyzer  
     */
    StopCriterion getGlobalStopCriterion();
}
