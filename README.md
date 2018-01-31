# ORIS Tool: The Sirio Library

## Introduction

**Sirio** is a library for the analysis of **stochastic time Petri
nets** (STPNs), a probabilistic model where:
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
steady-state probabilities** (and expected instantaneous/cumulative
**rewards**) when the underlying stochastic process of the STPN is:
- A **Continuous-Time Markov Chain** (CTMC), i.e., all transitions are
  exponential or immediate. In this case, uniformization is used to
  compute transient probabilities.
- A **Markov-Regenerative Process** (MRP) under enabling restriction,
  i.e., such that **at most one general transition is enabled** in
  each state. In this case, uniformization is used to analyze CTMCs
  subordinated to the enabling of each general transition.
- A **Markov-Regenerative Process** (MRP) with **many general
  transitions**, such that, eventually, they are all reset within a
  bounded number of a transition firing. The resulting condition
  (i.e., all general transitions are disabled or newly-enabled) is
  called a **regeneration**.

Sirio also allows the analysis of non-deterministic reductions of STPNs:
- **Time Petri Nets** (TPNs), where each transition has only a minimum
  and maximum value for its timer (instead of a probability
  distribution).
- *Petri Nets* (PNs), where transitions have no timers (each enabled
  transition can trigger the next event).

At the moment, all implementations use an explicit state encoding.
