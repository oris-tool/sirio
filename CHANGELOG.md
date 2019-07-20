# Changelog

## 2.0.0 (to be released)

### Functional changes

- Analysis methods now use builders for configuration.
- Class `Synchronization` is now inside package `math` instead of `models.stpn`.


### Bug fixed

- Implementation of `EnablingFunction.equals(Object)`


### Requirements/dependencies

- Upgrade to Java 11
- Add dependency on `com.google.auto.value:auto-value:jar:1.5.3`
- Include a style check using [Checkstyle](http://checkstyle.sourceforge.net/) (Google Java style using 4 spaces)
- Include static analysis checks using [PMD](https://pmd.github.io/pmd-6.0.1/pmd_rules_java.html)
- Include a copyright and license notice (AGPL)
