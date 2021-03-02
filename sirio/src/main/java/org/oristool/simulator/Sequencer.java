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

package org.oristool.simulator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.oristool.analyzer.EnabledEventsBuilder;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.analyzer.state.State;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.function.EXP;
import org.oristool.math.function.Erlang;
import org.oristool.math.function.Function;
import org.oristool.math.function.PartitionedFunction;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.trees.EmpiricalTransitionFeature;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.samplers.*;
import org.oristool.simulator.stpn.SamplerFeature;

public class Sequencer {

    public enum SequencerEvent {
        RUN_START,
        RUN_END,
        FIRING_EXECUTED,
        SIMULATION_START,
        SIMULATION_END
    }

    private static Random random = new Random();

    private final List<SequencerObserver> observers = new ArrayList<>();
    private final List<SequencerObserver> currentRunObservers = new ArrayList<>();

    private final PetriNet net;
    private final Marking initialMarking;
    private final SimulatorComponentsFactory<PetriNet, Transition> componentsFactory;
    private final AnalysisLogger logger;

    private long currentRunNumber;
    private BigDecimal currentRunElapsedTime;
    private long currentRunFirings;
    private Succession lastSuccession;

    /**
     * Builds a new instance for a given Petri net and initial marking.
     *
     * @param net Petri net
     * @param initialMarking initial marking for the simulation runs
     * @param componentsFactory helper components driving the simulation
     * @param logger logger used during the simulation
     */
    public Sequencer(PetriNet net, Marking initialMarking,
            SimulatorComponentsFactory<PetriNet, Transition> componentsFactory,
            AnalysisLogger logger) {
        this.net = net;
        this.initialMarking = initialMarking;
        this.componentsFactory = componentsFactory;
        this.logger = logger;
    }

    /**
     * Starts the simulation, running until all observers have completed.
     */
    public void simulate() {

        for (Transition t : net.getTransitions())
            if (!t.hasFeature(SamplerFeature.class))
                if (t.hasFeature(StochasticTransitionFeature.class)) {
                    StochasticTransitionFeature s = t.getFeature(StochasticTransitionFeature.class);
                    OmegaBigDecimal eft = s.density().getDomainsEFT();
                    OmegaBigDecimal lft = s.density().getDomainsLFT();

                    if(t.hasFeature(EmpiricalTransitionFeature.class)){
                        EmpiricalTransitionFeature e = t.getFeature(EmpiricalTransitionFeature.class);
                        t.addFeature(new SamplerFeature(new EmpiricalTransitionSampler(e.getHistogramCDF(), e.getLower(), e.getUpper())));
                    } else {
                        if (s.density() instanceof EXP)
                            t.addFeature(new SamplerFeature(
                                    new ExponentialSampler(((EXP) s.density()))));

                        else if (s.density() instanceof Erlang)
                            t.addFeature(new SamplerFeature(
                                    new ErlangSampler((Erlang) s.density())));

                        else if (s.density().getDensities().size() == 1
                                && s.density().getDensities().get(0).isConstant())
                            // assumes a uniform distribution on the domain
                            t.addFeature(new SamplerFeature(
                                    new UniformSampler(eft.bigDecimalValue(), lft.bigDecimalValue())));

                        else if (s.density() instanceof Function)
                            t.addFeature(new SamplerFeature(
                                    new MetropolisHastings((Function) s.density())));

                        else if (s.density() instanceof PartitionedFunction)
                            t.addFeature(new SamplerFeature(
                                    new PartitionedFunctionSampler((PartitionedFunction) s.density())));

                        else
                            new IllegalArgumentException(
                                    "The transition " + t + " has unsupported type");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "The transition " + t + " must have a stochastic feature");
                }

        SimulatorSuccessorEvaluator successorEvaluator = componentsFactory.getSuccessorEvaluator();
        EnabledEventsBuilder<PetriNet, Transition> firableTransitionSetBuilder = componentsFactory
                .getFirableTransitionSetBuilder();

        currentRunNumber = 0;
        logger.debug("Simulation started...");
        notifyObservers(SequencerEvent.SIMULATION_START);

        // Starts a new run until at least one observer is present
        while (observers.size() > 0) {

            currentRunElapsedTime = BigDecimal.ZERO;
            currentRunFirings = 0;

            // Creates the initial state
            State currentSimulatorState = this.componentsFactory.getInitialStateBuilder().build(net,
                    initialMarking);
            logger.debug("Initial state: \n" + currentSimulatorState);

            // Notifies any observer
            logger.debug("Run " + currentRunNumber + " started...");
            notifyObservers(SequencerEvent.RUN_START);

            // Fires a transition until at least one current run observer is present
            while (currentRunObservers.size() > 0) {

                if (!currentSimulatorState.hasFeature(PetriStateFeature.class))
                    throw new IllegalStateException("Unexpected state without marking!");
                Marking m = currentSimulatorState.getFeature(PetriStateFeature.class).getMarking();

                Set<Transition> enabledTransitions = firableTransitionSetBuilder
                        .getEnabledEvents(net, currentSimulatorState);
                if (enabledTransitions.size() == 0) {
                    logger.debug("No firable transition found.");
                    break; // will notify SequencerEvent.RUN_END
                }
                logger.debug("Enabled transitions: " + enabledTransitions);

                BigDecimal minTimeToFire = null;
                List<Transition> minTimeToFireTransitions = null;

                for (Transition t : enabledTransitions) {
                    BigDecimal ttf = currentSimulatorState
                            .getFeature(TimedSimulatorStateFeature.class).getTimeToFire(t)
                            .divide(new BigDecimal(
                                    t.getFeature(StochasticTransitionFeature.class)
                                    .clockRate().evaluate(m)), MathContext.DECIMAL128);

                    if (minTimeToFire == null || minTimeToFire.compareTo(ttf) > 0) {
                        minTimeToFire = ttf;
                        minTimeToFireTransitions = new ArrayList<Transition>();
                    }

                    if (ttf.compareTo(minTimeToFire) == 0)
                        minTimeToFireTransitions.add(t);
                }
                logger.debug("Minimum time to fire: " + minTimeToFire);
                logger.debug("Transitions with minimum time to fire: " + minTimeToFireTransitions);

                int maxPriority = -1;
                for (Transition t : minTimeToFireTransitions) {
                    if (t.hasFeature(Priority.class)
                            && t.getFeature(Priority.class).value() > maxPriority)
                        maxPriority = t.getFeature(Priority.class).value();
                }
                logger.debug("Maximum priority: " + maxPriority);

                List<Transition> firableTransitions = new ArrayList<Transition>();
                for (Transition t : minTimeToFireTransitions) {
                    if ((t.hasFeature(Priority.class)
                            && t.getFeature(Priority.class).value() == maxPriority)
                            || (!t.hasFeature(Priority.class) && maxPriority == -1))
                        firableTransitions.add(t);
                }
                logger.debug("Firable transitions: " + firableTransitions);

                BigDecimal totalWeight = BigDecimal.ZERO;
                for (Transition t : firableTransitions)
                    totalWeight = totalWeight.add(getWeight(t, net, m));

                BigDecimal needle = totalWeight
                        .multiply(new BigDecimal(Sequencer.random.nextDouble()));

                totalWeight = BigDecimal.ZERO;
                Transition firedTransition = null;
                for (Transition t : firableTransitions) {
                    totalWeight = totalWeight.add(getWeight(t, net, m));
                    if (needle.compareTo(totalWeight) < 0) {
                        firedTransition = t;
                        break;
                    }
                }

                try {
                    lastSuccession = successorEvaluator.computeSuccessor(net, currentSimulatorState,
                            firedTransition);
                    logger.debug("Fired transition: " + firedTransition + "\n" + lastSuccession);

                } catch (Exception e) {
                    notifyObservers(SequencerEvent.SIMULATION_END);
                    return;
                }

                currentRunFirings++;
                currentRunElapsedTime = currentRunElapsedTime
                        .add(currentSimulatorState.getFeature(TimedSimulatorStateFeature.class)
                                .getTimeToFire(firedTransition));

                currentSimulatorState = lastSuccession.getChild();
                notifyCurrentRunObservers(SequencerEvent.FIRING_EXECUTED);
            }

            logger.debug("Run " + currentRunNumber + " ended.");
            notifyObservers(SequencerEvent.RUN_END);
            currentRunNumber++;
        }

        logger.debug("Simulation ended.");
    }

    private BigDecimal getWeight(Transition t, PetriNet n, Marking m) {
        return new BigDecimal(t.getFeature(StochasticTransitionFeature.class).weight().evaluate(m));
    }

    /**
     * Adds an observer of the simulation.
     *
     * @param observer simulation observer
     */
    public void addObserver(SequencerObserver observer) {

        if (!observers.contains(observer))
            observers.add(observer);
    }

    /**
     * Removes an observer of the simulation.
     *
     * @param observer simulation observer
     */
    public void removeObserver(SequencerObserver observer) {

        observers.remove(observer);
    }

    private void notifyObservers(SequencerEvent event) {

        List<SequencerObserver> observersCopy = new ArrayList<SequencerObserver>(observers);
        for (SequencerObserver o : observersCopy)
            o.update(event);

    }

    /**
     * Adds an observer of the current simulation run.
     *
     * @param observer simulation observer
     */
    public void addCurrentRunObserver(SequencerObserver observer) {

        if (!currentRunObservers.contains(observer))
            currentRunObservers.add(observer);
    }

    /**
     * Removes an observer of the current simulation run.
     *
     * @param observer simulation observer
     */
    public void removeCurrentRunObserver(SequencerObserver observer) {

        currentRunObservers.remove(observer);
    }

    private void notifyCurrentRunObservers(SequencerEvent event) {

        List<SequencerObserver> currentRunObserversCopy = new ArrayList<SequencerObserver>(
                currentRunObservers);
        for (SequencerObserver o : currentRunObserversCopy)
            o.update(event);
    }

    public BigDecimal getCurrentRunElapsedTime() {
        return currentRunElapsedTime;
    }

    public long getCurrentRunFirings() {
        return currentRunFirings;
    }

    public long getCurrentRunNumber() {
        return currentRunNumber;
    }

    public Succession getLastSuccession() {
        return lastSuccession;
    }

    public Marking getInitialMarking() {
        return initialMarking;
    }
}
