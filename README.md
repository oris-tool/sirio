# ORIS Tool: The Sirio Library

## Installation

Currently, we recommend version `2.0.3` of Sirio. To add this
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
    <version>2.0.3</version>
  </dependency>
</dependencies>
```

Since Sirio requires Java 11, you will also need to set the following
properties in `pom.xml`:

```xml
<properties>
  <maven.compiler.release>11</maven.compiler.release>
</properties>
```

If you are looking for a **ready-to-use project to import into Eclipse**,
please check the
[sirio-examples](https://github.com/oris-tool/sirio-examples)
repository.


## Introduction

Sirio is a library for the analysis of **stochastic time Petri nets**
(STPNs), a probabilistic model where:
- The state is modeled by **tokens** contained inside **places**.
- State changes are modeled by **transitions** enabled by specific
  enabling conditions (predicates over token counts). Simple enabling
  conditions can also be specified using precodition arcs (the
  connected place must be nonempty) and inhibitor arcs (the connected
  place must be empty).
- When a transition is enabled, it **samples a random timer**: among
  all enabled transitions, the one with minimum timer value triggers
  the next discrete event, modifying token counts inside places and
  possibly enabling/disabling other transitions.

The analysis methods available in Sirio can compute **transient and
steady-state probabilities** (and instantaneous/cumulative rewards)
when the underlying stochastic process of the STPN is:
- A **continuous-time Markov chain** (CTMC), i.e., all transitions are
  exponential or immediate. In this case, uniformization is used to
  compute transient probabilities.
- A **Markov-regenerative process** (MRP) either:
  + Under enabling restriction, i.e., such that **at most one general
    transition is enabled** in each state. In this case,
    uniformization is used to analyze CTMCs subordinated to the
    enabling of each general transition.
  + With **many general transitions**, such that, eventually, they are
    all reset within a bounded number of a transition firing.

The latter class of MRPs includes **semi-Markov processes** (SMPs) as
a special case. Transient analysis of **generalized semi-Markov
processes** (GSMPs) is also possible (but computationally very
expensive).

Finally, Sirio also allows the analysis of **time Petri nets** (TPNs),
where each transition has only a minimum and maximum value for its
timer (instead of a probability distribution).


## Documentation

- To include Sirio in a Java project, modify your `pom.xml` according
  to the "Installation" section above.
- To quickly get started with Sirio, you can import a ready-to-use
  Maven project into your Eclipse workspace from
  the [sirio-examples](https://github.com/oris-tool/sirio-examples) repository.  The project includes Sirio as a
  dependency and provides an example of how to create an STPN and
  compute its transient probabilities.
- To learn more about the analysis functions available in Sirio, you
  can read the [wiki of this project](https://github.com/oris-tool/sirio/wiki).
- To learn more about the API, you can consult the [online Sirio Javadoc](https://www.oris-tool.org/apidoc).

If you encounter a problem with Sirio,
<a href="mailto:paolieri@usc.edu">write to us</a>.
We also keep track of open issues and improvement proposals on our
<a href="https://github.com/oris-tool/sirio/issues">issue tracker</a>.
