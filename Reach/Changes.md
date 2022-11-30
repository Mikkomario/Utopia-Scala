# Utopia Reach - List of Changes

## v0.5 (in development)
### Breaking Changes
- **InteractionWindowFactory**`.display(...)` and `.displayOver(...)` now return a tuple containing: 
  - 1: The window that was opened and
  - 2: The window close result future (i.e. the earlier return value)
- Some constructors now require an **EqualsFunction** instead of a regular function when asking for "same item -checks"
- Renamed `additionalCustomDrawers` / `additionalDrawers` -parameter to `customDrawers` in some component 
  factory functions
- Modified **SelectionList** constructors
### Bugfixes
- Some fixes to **SelectionList** visuals
### New Features
- **SelectionList** now supports two new constructor parameters:
  - `highlightModifier: Double` - A modified applied to all visual highlighting
  - `alternativeKeyCondition: => Boolean` - An alternative condition for enabling arrow key events
- Added **MessageWindowFactory** trait

## v0.4 - 02.10.2022
This update focuses on TextField customizability, adding a number of new ways to affect text highlighting and 
input processing in **TextField**s.
### Breaking Changes
- **TextField** input validation function is now required to return an **InputValidationResult** instead of a 
  **LocalizedString**
- Renamed the custom drawer construction parameter to `customDrawers` **ImageLabel** and **ImageAndTextLabel** 
### Bugfixes
- Fixed a bug where all text was drawn with wrong alignment
### New Features
- **TextField** input validation is only applied when the field loses focus. 
  Also, The validation is now allowed to return custom highlighting, 
  suitable for displaying a success or a warning message, etc.
### Other Changes
- **Field** now highlights the hint text as well when custom highlighting is applied
- **InputWindowFactory**`.showWarningFor(...)` is now **protected** instead of private

## v0.3.4 - 18.08.2022
This update reflects changes in **Flow** v1.16 and **Genesis** v3.0

## v0.3.3 - 06.06.2022
Reactions to changes in other modules (**Flow** & **Genesis**)

## v0.3.2 - 27.01.2022
Minor supporting changes in response to Flow v1.14.1 deprecations, as well as a scala version update
### Scala
This module now uses Scala v2.13.7

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
