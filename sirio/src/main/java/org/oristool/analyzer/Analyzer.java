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

import java.util.LinkedHashSet;
import java.util.Set;

import org.oristool.analyzer.graph.SuccessionGraph;
import org.oristool.analyzer.policy.EnumerationPolicy;
import org.oristool.analyzer.state.LocalStop;
import org.oristool.analyzer.state.State;
import org.oristool.analyzer.stop.StopCriterion;

/**
 * General state-space expansion algorithm.
 * <pre>{@code
 *
 *   create succession (null, s0)        // and notify
 *   post-process succession (null, s0)  // and notify
 *   add succession (null, s0) to queue  // and notify
 *
 *   while not queue.isEmpty() and not globalStop():
 *
 *     (parent, i) = queue.remove()      // and notify
 *     preprocess succession (parent, i) // and notify
 *     isNewChild = G.add((parent, i))   // and notify
 *
 *     if isNewChild and not localStop((parent, i)):
 *       for j in postprocessed non-null successors of i
 *       // (notified creation and postprocessing)
 *
 *         queue.add((i,j))              // and notify
 *         if globalStop():
 *           break
 *
 *   while not queue.isEmpty()           // ended because of global stop
 *     (parent, i) = queue.remove()      // and notify
 *     pre-process (parent, i)           // and notify
 *     G.add((parent, i))                // and notify
 * }</pre>
 */
public final class Analyzer<M, E extends Event> {
    private final Set<AnalyzerObserver> observers = new LinkedHashSet<>();
    private final SuccessionGraph graph = new SuccessionGraph();

    private final M model;
    private final State initialState;

    private AnalyzerComponentsFactory<M, E> componentsFactory;
    private EnumerationPolicy enumerationPolicy;
    private EnabledEventsBuilder<M, E> enabledEventsBuilder;
    private SuccessionEvaluator<M, E> successionEvaluator;
    private SuccessionProcessor preProcessor;
    private SuccessionProcessor postProcessor;
    private StopCriterion globalStopCriterion;
    private StopCriterion localStopCriterion;

    /**
     * Creates an analyzer that will use the objects provided by the given factory
     * to analyze a model from some initial state.
     *
     * @param componentsFactory factory of objects used during the analysis
     * @param model input model
     * @param initialState initial state of the model
     */
    public Analyzer(AnalyzerComponentsFactory<M, E> componentsFactory, M model,
            State initialState) {
        this.componentsFactory = componentsFactory;
        this.model = model;
        this.initialState = initialState;
    }

    public void addObserver(AnalyzerObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AnalyzerObserver observer) {
        observers.remove(observer);
    }

    /**
     * Starts the analysis.
     *
     * @return the graph resulting from the enumeration
     */
    public SuccessionGraph analyze() {

        // Instantiating helper objects
        enumerationPolicy = componentsFactory.getEnumerationPolicy();
        enabledEventsBuilder = componentsFactory.getEnabledEventsBuilder();
        successionEvaluator = componentsFactory.getSuccessionEvaluator();
        preProcessor = componentsFactory.getPreProcessor();
        postProcessor = componentsFactory.getPostProcessor();
        globalStopCriterion = componentsFactory.getGlobalStopCriterion();
        localStopCriterion = componentsFactory.getLocalStopCriterion();

        // Building the initial succession null --null--> initial_state
        Succession initialSuccession = new Succession(null, null, initialState);
        this.notifySuccessionCreated(initialSuccession);

        // Postprocessing the initial succession
        initialSuccession = postProcessor.process(initialSuccession);
        this.notifySuccessionPostProcessed(initialSuccession);

        // Adding the initial succession to the queue
        if (initialSuccession != null) {
            enumerationPolicy.add(initialSuccession);
            this.notifySuccessionInserted(initialSuccession);
        }

        // While queue is not empty and the global stop criterion is not
        // satisfied
        while (!enumerationPolicy.isEmpty() && !globalStopCriterion.stop()) {

            // Extracts and postprocesses the next succession
            Succession currentSuccession = enumerationPolicy.remove();
            this.notifySuccessionExtracted(currentSuccession);

            currentSuccession = preProcessor.process(currentSuccession);
            this.notifySuccessionPreProcessed(currentSuccession);

            // Adds the succession to the graph
            boolean newChild = graph.addSuccession(currentSuccession);
            this.notifyNodeAdded(currentSuccession);

            // If new, expands the child of this succession
            if (localStopCriterion.stop()) {
                currentSuccession.getChild().addFeature(LocalStop.INSTANCE);

            } else if (newChild) {
                // Computes successions starting from the child of the current
                // one
                for (E e : enabledEventsBuilder.getEnabledEvents(model,
                        currentSuccession.getChild())) {
                    Succession childSuccession = successionEvaluator
                            .computeSuccession(model,
                                    currentSuccession.getChild(), e);

                    if (childSuccession != null) {
                        // FIXME: forse andrebbe spostato fuori dall'if per
                        // permettere il log delle successioni
                        // nulle
                        this.notifySuccessionCreated(childSuccession);

                        childSuccession = postProcessor
                                .process(childSuccession);
                        this.notifySuccessionPostProcessed(childSuccession);

                        enumerationPolicy.add(childSuccession);
                        this.notifySuccessionInserted(childSuccession);
                    }

                    // FIXME: questo non funziona con un random stop criterion.
                    // Mettiamo nel contratto degli StopCriterion che se non
                    // cambia lo stato
                    // stop() ritorna sempre il solito valore.
                    if (globalStopCriterion.stop()) {
                        break;
                    }
                }
            }
        }

        // se scatta il global stop criterion, aggiunge le le successioni (p,i)
        // ancora in coda TODO: da disabilitare con opzione
        while (!enumerationPolicy.isEmpty()) {
            Succession finalSuccession = enumerationPolicy.remove();
            this.notifySuccessionExtracted(finalSuccession);

            finalSuccession = preProcessor.process(finalSuccession);
            this.notifySuccessionPreProcessed(finalSuccession);

            graph.addSuccession(finalSuccession);
            this.notifyNodeAdded(finalSuccession);
        }

        return graph;
    }

    private void notifySuccessionCreated(Succession succession) {
        globalStopCriterion.notifySuccessionCreated(succession);
        localStopCriterion.notifySuccessionCreated(succession);
        for (AnalyzerObserver o : observers) {
            o.notifySuccessionCreated(succession);
        }
    }

    private void notifySuccessionPostProcessed(Succession succession) {
        globalStopCriterion.notifySuccessionPostProcessed(succession);
        localStopCriterion.notifySuccessionPostProcessed(succession);
        for (AnalyzerObserver o : observers) {
            o.notifySuccessionPostProcessed(succession);
        }
    }

    private void notifySuccessionInserted(Succession succession) {
        globalStopCriterion.notifySuccessionInserted(succession);
        localStopCriterion.notifySuccessionInserted(succession);
        for (AnalyzerObserver o : observers) {
            o.notifySuccessionInserted(succession);
        }
    }

    private void notifySuccessionExtracted(Succession succession) {
        globalStopCriterion.notifySuccessionExtracted(succession);
        localStopCriterion.notifySuccessionExtracted(succession);
        for (AnalyzerObserver o : observers) {
            o.notifySuccessionExtracted(succession);
        }
    }

    private void notifySuccessionPreProcessed(Succession succession) {
        globalStopCriterion.notifySuccessionPreProcessed(succession);
        localStopCriterion.notifySuccessionPreProcessed(succession);
        for (AnalyzerObserver o : observers) {
            o.notifySuccessionPreProcessed(succession);
        }
    }

    private void notifyNodeAdded(Succession succession) {
        globalStopCriterion.notifyNodeAdded(succession);
        localStopCriterion.notifyNodeAdded(succession);
        for (AnalyzerObserver o : observers) {
            o.notifyNodeAdded(succession);
        }
    }
}
