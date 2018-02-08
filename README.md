# ORIS Tool: The Sirio Library

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
