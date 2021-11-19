# Utopia Reach - List of Changes

## v0.3.2 (in development)
Minor supporting changes in response to Flow v1.14.1 deprecations

## v0.3.1 - 04.11.2021
Supports changes in Flow v1.14

## v0.3 (alpha) - 3.10.2021
This update simply reflects changes in the **Flow**, **Genesis** and **Reflection** modules, utilizing the new 
traits and classes.
### Breaking Changes
- Use of **Direction1D** was replaced with **Sign** (according to **Genesis** and **Reflection** changes)
- **StackFactory**'s `.forPair(...)` -variants now expect a **Pair** instead of a tuple

## v0.2.1 (an alpha release) - 13.7.2021
Not much of an update. Simply added support for the breaking **Cache**-related changes in **Utopia Flow** v1.10.

## v0.2 (an alpha release) - 12.5.2021
An update in the middle of developing layered view implementations. 
Improves paint efficiency but breaks existing package structure 
(which is normal at this stage of development).
### Breaking Changes
- Updated package structuring in utopia.reach.container
- Components must now specify `.transparent: Boolean`, which is used in drawing optimizations
### Other Changes
- Optimized component painting to ignore background elements under opaque components

## v0.1 (an alpha release) - 17.4.2021
See README.md for a list of main features.  
This library can be used as is, but is lacking some key features and will 
experience major changes in the near development.
