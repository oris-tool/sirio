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

/**
 * Common interface of observers of the analyzer execution.
 */
public interface AnalyzerObserver {
    /**
     * Notifies the observer that the analyzer has extracted the given succession
     * from the policy.
     * 
     * @param succession succession extracted by the analyzer
     */
    void notifySuccessionExtracted(Succession succession);

    /**
     * Notifies the observer that the analyzer has pre-processed the given
     * succession.
     * 
     * @param succession succession pre-processed by the analyzer
     */
    void notifySuccessionPreProcessed(Succession succession);

    /**
     * Notifies the observer that the analyzer has added the child node of the given
     * succession to the graph.
     * 
     * @param succession succession added to the graph by the analyzer
     */
    void notifyNodeAdded(Succession succession);

    /**
     * Notifies the observer that the analyzer has created the given succession
     * after firing one of the events in the extracted succession.
     * 
     * @param succession succession created by the analyzer
     */
    void notifySuccessionCreated(Succession succession);

    /**
     * Notifies the observer that the analyzer has post-processed the given
     * succession.
     * 
     * @param succession succession post-processed by the analyzer
     */
    void notifySuccessionPostProcessed(Succession succession);

    /**
     * Notifies the observer that the analyzer has inserted the given succession
     * into the policy queue.
     * 
     * @param succession succession added by the analyzer to the policy queue
     */
    void notifySuccessionInserted(Succession succession);
}
