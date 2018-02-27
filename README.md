# ORIS Tool: The Sirio Library

[![Build Status](https://travis-ci.org/oris-tool/sirio.svg?branch=master)](https://travis-ci.org/oris-tool/sirio)

## Installation

Currently, we recommend version `2.0.0-SNAPSHOT` of Sirio. To add this 
dependency to your Maven project, just insert the following lines into 
your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>oss-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.oris-tool</groupId>
    <artifactId>sirio</artifactId>
    <version>2.0.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

Since Sirio requires Java 9, you will also need to set the following
properties in `pom.xml`:

```xml
<properties>
  <maven.compiler.source>9</maven.compiler.source>
  <maven.compiler.target>9</maven.compiler.target>
</properties>
```

If you are looking for a **ready-to-use project to import into Eclipse**, 
please check the
[sirio-examples](https://github.com/oris-tool/sirio-examples)
repository.

## Introduction

Sirio is a library for the analysis of **stochastic time Petri nets**
(STPNs), a probabilistic model where:
- The state is modeled by **tokens** contained in **places**. A
  **marking** assigns a token count to each place, providing a full
  description of the current discrete state.
- State changes are modeled by **transitions** that are enabled by
  specific **enabling conditions** (predicates on token
  counts). Simple enabling conditions can also be specified using
  **precodition arcs** (the connected place must be nonempty) and
  **inhibitor arcs** (the connected place must be empty).
- When a transition is enabled, it **samples a random timer**: among
  all enabled transitions, the one with minimum timer value triggers
  the next discrete event, modifying token counts and possibly
  enabling/disabling other transitions.

The available analysis methods can compute **transient and
steady-state probabilities** (and instantaneous/cumulative rewards)
when the underlying stochastic process of the STPN is:
- A Continuous-Time Markov Chain (CTMC), i.e., all transitions are
  exponential or immediate. In this case, uniformization is used to
  compute transient probabilities.
- A Markov-Regenerative Process (MRP) under enabling restriction,
  i.e., such that **at most one general transition is enabled** in
  each state. In this case, uniformization is used to analyze CTMCs
  subordinated to the enabling of each general transition.
- A Markov-Regenerative Process (MRP) with **many general
  transitions**, such that, eventually, they are all reset within a
  bounded number of a transition firing. The resulting condition
  (i.e., all general transitions are disabled or newly-enabled) is
  called a **regeneration**.

Sirio also allows the analysis of non-deterministic reductions of STPNs:
- Time Petri Nets (TPNs), where each transition has only a minimum
  and maximum value for its timer (instead of a probability
  distribution).
- Petri Nets (PNs), where transitions have no timers (each enabled
  transition can trigger the next event).

At the moment, all implementations use an explicit state encoding.


## Quick Start

### Building a model

All models (PN, TPN, STPN) include the fundamental elements of Petri
nets, places and transitions (classes `Place` and `Transition`). These
are added to `PetriNet` instances together with `Precondition`,
`Postcondition` and `InihibitorArc` instances.

```java
PetriNet pn = new PetriNet();     // creates an empty Petri net

Place p0 = pn.addPlace("p0");     // adds places and transitions
Place p1 = pn.addPlace("p1");
Place p3 = pn.addPlace("p3");
Transition t0 = pn.addTransition("t0");
Transition t1 = pn.addTransition("t1");

pn.addPrecondition(p0, t0);       // t0 moves a token from p0 to p1
pn.addPostcondition(t0, p1)

pn.addPrecondition(p1, t1);       // t1 moves a token back from p1 to p0
pn.addPostcondition(t1, p0)

pn.addInhibitorArc(p3, t1);       // p3 must be empty for t1 to be enabled

Marking marking = new Marking();  // a new marking (initially empty)
marking.addTokens(p0, 1);
```

Additional requirements for the enabling of a transition can be
specified using an `EnablingFunction` feature:

```java
t3.addFeature(new EnablingFunction("p4 == p5+p6"));
```

Additional token moves after the firing of a transition can be added
using a `PostUpdater` feature:

```java
t3.addFeature(new PostUpdater("p5 = p1+p2; p2 = 0", pn));
```

### Time Petri Nets

Time Petri nets impose an interval [a,b] of required values for the
firing of each transition. The semantics is strong: each transition
must be fired when the upper bound of its firing interval is
reached. It is also non-deterministic: any value can be selected from
the firing interval. This allows different interleavings when multiple
transitions are enabled with overlapping firing intervals.

Firing intervals are added to a transition as a `TimedTransitionFeature`:

```java
t3.addFeature(new TimedTransitionFeature("5", "15"));
```

Note how the earliest firing time (5) and latest firing time (15) are
specified as strings (and not `int` or `double` instances) to avoid
approximations. The firing interval can also be unbounded:

```java
t3.addFeature(new TimedTransitionFeature("5", "inf"));  // equivalent:
t3.addFeature(new TimedTransitionFeature(new OmegaBigDecimal("5"),
    OmegaBigDecimal.POSITIVE_INFINITY));
```

The library supports the analysis of the **state class graph** of a
TPN. Each node of the graph encodes a continuous set of states with
the same marking and different values for transition timers (encoded
as a Difference Bound Matrix); edges are associated with transition
firings and successor nodes encode all the states that can be reached
through the firing (from some state of the predecessor).

The analysis can be configure through the builder of `TimedAnalysis` objects:

``` java
TimedAnalysis analysis = TimedAnalysis.builder().includeAge(true).build();
SuccessionGraph graph = analysis.compute(pn, marking);  // runs the analysis

System.out.printf("The graph has %d state classes and %d edges.%n",
    graph.getStates().size(), graph.getSuccessions().size());

State root = graph.getState(graph.getRoot());
Marking rootMarking = root.getFeature(PetriStateFeature.class).getMarking();
DBMZone rootTimings = root.getFeature(TimedStateFeature.class).getDomain();
System.out.printf("The root node has marking %s and timer values: %n%s",
    rootMarking, rootTimings);
```

The available options when building `TimedAnalysis` are:
- `includeAge(boolean)`: whether or not to add `Variable.AGE` to track
  the absolute time of the last firing (false by default).
- `markRegenerations(boolean)`: whether or not to mark regenerations
  on the state class graph (for STPN models, false by default).
- `excludeZeroProb(boolean)`: whether or not to exclude firings with
  zero probability (for STPN models, false by default).
- `policy(Supplier<EnumerationPolicy>)`: to use a custom policy to
  select the next state (FIFO by default).
- `stopOn(Supplier<StopCriterion>)`: to stop the analysis on some
  nodes (never by default).


### Stochastic Time Petri Nets

STPNs also define a probability density function (PDF) over the
interval [a,b] of required values for the firing of each transition.

Initially, each enabled transition **samples** a firing time according
to its PDF. The firing time acts as a "timeout": the transition with
firing time is selected. Its firing time is equal to the sojourn time
in the current state.

After the firing, the marking is updated according to the usual token
moves (and `PostUpdater` function).
- Newly-enabled or reset transitions sample a new firing time
- Persistent transitions (enabled before the firing and during token
  moves) have a firing time reduced by the sojourn time in the
  previous state.

A firing time PDF is added to a transition as a `StochasticTransitionFeature`:

```java
t3.addFeature(StochasticTransitionFeature.newUniformInstance("5", "15"));
```

Static factory methods are available
in
[`StochasticTransitionFeature`](http://www.oris-tool.org/apidoc/org/oristool/models/stpn/StochasticTransitionFeature.html) to
to create firing times with many PDF, such as uniform, deterministic,
exponential, expolynomial (among others).

The library supports many analysis methods, for both transient and
steady-state probabilities (and rewards).  The applicability of these
methods depends on the properties of the underlying stochastic
process.

#### TreeTransient

The most general (but also computationally expensive) analysis method
is `TreeTransient`, which builds a tree of transient stochastic state
classes. Each node encodes a marking and the joint PDF of enabled
transitions. These are used to compute the probability that the STPN
is in any node of the tree (and thus it has its marking) at a given
time instant.

The analysis can be configure through the builder of `TreeTransient`
objects:

``` java
TreeTransient analysis = TreeTransient.builder()
    .timeBound(new BigDecimal("5"))
    .timeStep(new BigDecimal("0.1"))
    .build();

TransientSolution<Marking, Marking> result = analysis.compute(pn, marking);

System.out.println("The transient probabilities at time 1.0:");
for (int j = 0; j < result.getColumnStates().size(); j++) {
    System.out.printf("%1.6f -- %s%n", result.getSolution()[10][0][j],
            result.getColumnStates().get(j));
}
```

The available options when building `TreeTransient` are:
- `timeBound(BigDecimal)`: specifies the maximum time used to select
  the time points where transient probabilities are computed
  (required).
- `timeStep(BigDecimal)`: specifies the time step used to select time
  points in `[0, timeBound]` (required).
- `policy(Supplier<EnumerationPolicy>)`: to use a custom policy to
  select the next state to explore in the tree (FIFO by default).
- `stopOn(Supplier<StopCriterion>)`: to stop the analysis on some
  nodes (never by default).

Note that FIFO and LIFO policies expand all transition firings in the
tree until an absorbing state (where no transition is enabled) is
reached. In most circumstances, it is useful to exclude nodes that can
be reached only after `timeBound`, or to ensure that the probability
of reaching unexplored nodes before `timeBound` is lower than some
error.

You can easily select this policy using the builder method
`greedyPolicy(timeBound, error)`:

``` java
TreeTransient analysis = TreeTransient.builder()
    .greedyPolicy(new BigDecimal("5"), BigDecimal.ZERO)
    .timeStep(new BigDecimal("0.1"))
    .build();
```

Nodes with higher **reaching probability** (the probability of the
firing sequence from the root node) are explored first; the analysis
ends when the total reaching probability of unexplored nodes is lower
than the allowed error.

#### RegTransient

The analysis method `RegTransient` can be applied when the underlying
stochastic process finds regenerations (i.e., states where all general
timers are newly-enabled or with deterministic enabling time).

This method builds a tree of transient stochastic state classes from
each regeneration until the next reachable regeneration. Each node of
the tree encodes a marking and the joint PDF of enabled
transitions. These are used to compute the global and local kernels of
the underlying Markov regenerative process.

In turn, the kernels are used to solve (numerically) Markov renewal
equations that provide the transient probability of all markings.

The analysis can be configured through the builder of `RegTransient`
objects:

``` java
RegTransient analysis = RegTransient.builder()
    .timeBound(new BigDecimal("5"))
    .timeStep(new BigDecimal("0.1"))
    .build();

TransientSolution<DeterministicEnablingState, Marking> result =
    analysis.compute(pn, marking);

System.out.println("The transient probabilities at time 1.0:");
for (int j = 0; j < result.getColumnStates().size(); j++) {
    System.out.printf("%1.6f -- %s%n", result.getSolution()[10][0][j],
            result.getColumnStates().get(j));
}
```

The available options when building `RegTransient` are:
- `timeBound(BigDecimal)`: specifies the maximum time used to select
  the time points where transient probabilities are computed
  (required).
- `timeStep(BigDecimal)`: specifies the time step used to select time
  points in `[0, timeBound]` (required).
- `policy(Supplier<EnumerationPolicy>)`: to use a custom policy to
  select the next state to explore in the tree (FIFO by default).
- `stopOn(Supplier<StopCriterion>)`: to stop the analysis on some
  nodes (never by default).
- `normalizeKernels(boolean)`: whether to normalize of global kernel
  (false by default). Without normalization, defective kernel rows
  produce probabilities that sum to less than 1, but they are
  guaranteed to be lower bounds of the exact values. With
  normalization, the output probabilities always sum to 1, but they
  can include an error (increasing over time) that overestimate or
  underestimate the exact values.

To exclude nodes that can be reached only after `timeBound` in each
tree, or to ensure that the probability of reaching unexplored nodes
before `timeBound` is lower than some error, you can select a
greedy policy using the builder method `greedyPolicy(timeBound, error)`:

``` java
RegTransient analysis = TreeTransient.builder()
    .greedyPolicy(new BigDecimal("5"), BigDecimal.ZERO)
    .timeStep(new BigDecimal("0.1"))
    .build();
```

Nodes with higher **reaching probability** (the probability of the
firing sequence from the root node) are explored first; the analysis
ends when the total reaching probability of unexplored nodes is lower
than the allowed error.

#### RegSteadyState

The analysis method `RegSteadyState` can be applied when the
underlying stochastic process finds regenerations in a bounded number
of discrete events (e.g., within at most 20 transition firings).

This method builds a tree of transient stochastic state classes from
each regeneration until the next reachable regeneration. Each node of
the tree encodes a marking and the joint PDF of enabled
transitions. The DTMC embedded at regeneration points is solved to
find equilibrium probabilities of regenerations; these are combined
through sojourn times in each marking to compute the final result.

Note that the current implementation assumes that the state space is
irreducible.

The analysis can be launched using instances of `RegSteadyState` (in
most cases, without any configuration):

``` java
RegSteadyState analysis = RegSteadyState.builder().build();

SteadyStateSolution<Marking> result = analysis.compute(pn, marking);
Map<Marking, BigDecimal> probs = result.getSteadyState();

System.out.println("Steady-state probabilities:");
for (Marking m : probs.keySet()) {
    System.out.printf("%1.6f -- %s%n", probs.get(m), m);
}
```
