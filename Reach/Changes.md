# Utopia Reach - List of Changes

## v1.1.1 (in development)
Supports **Flow v2.3**
### Bugfixes
- Fixed cursor shade calculation in **MutableImageButton**, **ViewImageButton** and **SelectionList**
### New features
- Added support for cursors with edges
  - To use this, specify the `drawEdgesFor` -parameter in `CursorSet.loadIcons(...)` or use 
    `Cursor(SingleColorIcon, Boolean)`
### Other changes
- Cursor shade-detection algorithm is now much simpler, resulting in faster code and smaller memory use, 
  with the cost of less accurate cursor color
- Added a couple of new utility functions to button factories
- **Checkbox** cursor shade calculation is now faster
- Scala version updated to 2.13.12

## v1.1 - 27.09.2023
This update continues the major refactoring process of v1.0 update.  
Almost every component constructor is rewritten to support a new builder-style approach.  
This update also includes important bugfixes related to certain component-hierarchy edge-cases.
### Breaking Changes
- Renamed `utopia.reach.util` to `utopia.reach.drawing`
- Moved some classes from the `factory` package to `factory.contextual`
- Updated how containers are built
  - The `.build(...)(...)` function is now performed after specifying the other parameters. 
    These parameters are no longer included in this function.
  - The factories support incremental parameter definition. Multiple parameters may be defined using `.copy(...)`
- Updated contextual constructors of various labels
- Updated **InteractionWindowFactory** in following ways:
  - The abstract property `.windowContext` is now required to be of type **ReachContentWindowContext**
  - Removed `.standardContext`, which is now replaced by the `.windowContext`
  - `.createContent(...)` now accepts **TextContext** instead of **ColorContext**
  - `.display(...)` now returns a **WindowCreationResult** instead of a **ComponentCreationResult**
- Updated **InputWindowFactory** in following ways:
  - Replaced `.fieldCreationContext` and `.makeFieldNameAndFieldContext(ColorContext)` with `.contentContext`
  - `.buildLayout(...)` now accepts **TextContext** instead of **ColorContext**
- Removed all **Option**-wrappings of **SingleColorIcons**. 
  - Use `SingleColorIcon.empty` to represent None and `icon.notEmpty` if you want to wrap it in an **Option**.
- Updated **FieldWithSelectionPopup** and **DropDown** constructors:
  - `makeDisplay` now accepts 4 parameters instead of 2
  - Replaced `noOptionsView: Option[OpenComponent]` with `makeNoOptionsView: Option[...]`
  - Added `makeAdditionalView: Option` -parameter
- **CursorSet**`.loadIcons(...)` now returns a **TryCatch** instead of a **Try**
- **ViewStack** constructors now return a **Stack**
  - This was added in order to automatically optimize between the **ViewStack** and the **Stack** options
- **ImageLabel** is now a trait
- Removed the `customDrawers` -option from **ReachWindowContext** constructor, that was left there by accident
- Rewrote most component constructors
### Deprecations
- Deprecated existing constructors in various label classes
- Deprecated **ImageLabelLike** in favor of **ImageLabel**
- Deprecated **BuilderFactory** and **SimpleFilledBuilderFactory**
### Bugfixes
- Fixed issues with `ReachCanvas.forSwing(...)`
  - The previous implementation didn't properly follow component hierarchy attachment status
- `.isThisLevelLinked` in **SeedHierarchyBlock** didn't always return the correct value (fixed)
### New Features
- **FieldWithSelectionPopup** and **DropDown** now support an additional field at the end of the selection list 
  (optional feature)
- Added new constructor style to various component factories
- Added component factory traits that utilize a variable component creation context (i.e. a context-pointer)
- Added easier methods for adding background-drawing to certain components and containers
  - In containers, the new `.withBackground(...)` functions are available implicitly (see **GenericContextualFactory**)
  - In component factories, these become available after importing **ContextualBackgroundAssignableFactory**
- **Switches** can now be commonly scaled by modifying `ComponentCreationDefaults.switchScalingFactor`
### New Methods
- **CachingViewSwapper** (factory)
  - Added the missing `.build(...)` method
- **ReachWindow**
  - Added a new `.withContext(ReachContentWindowContext)` -variant
### Other Changes
- **ViewStack** constructors now optimize resources by constructing a **Stack** if the content is static
- **Switches** now require a higher color contrast against the background

## v1.0 - 01.05.2023
This is intended as the initial release of the full version of **Reach**, which is, from this version onwards, 
a module completely independent of the **Utopia Reflection** module.

A lot of code was rewritten for this version, not only because of the module separation, but also to 
fix certain design issues that were present in the earlier versions.

If you've been using **Reach** already, please take a look at the **Reach-in-Reflection** module and pay 
close attention to these changes, as well as to the README document.
### Breaking Changes
- This module now extends Utopia **Firmament** instead of Utopia **Reflection**, 
  and reflects all changes made when converting from **Reflection** to **Firmament**
  - This includes:
    - Different context class hierarchy (notice the new context types)
    - **GuiElementStatus** instead of **ButtonState**
- Rewrote most of the factory and builder traits, including **Open** and **Mixed**
  - The factories are now divided into two categories:
    - Ones supporting generic context type (for containers) and 
    - Ones using a static context type (e.g. TextContext)
  - Previously all factories were of this more generic type, which caused some challenges with type-inference
  - Generic type parameters in different kinds of factories were also greatly altered, 
    in order to make them as simple as possible
  - Please check the instructions in README on how to build these new factories, and/or check the source code
- Rewrote **ReachCanvas**
  - This new **ReachCanvas** doesn't have any **Reflection** dependencies or interfaces available. 
    If you want to use a **ReachCanvas** instance inside **Reflection**, check the **Reach-in-Reflection** module
  - This new **ReachCanvas** performs component repainting automatically upon `.revalidate()` calls
- **InputRowBluePrint** now only accepts **InputFields** instead of various kinds of **ManagedFields**
  - Removed much of generic type support from this class because the compiler couldn't keep up with the generic types
    - **ManagedField** was only used in **InputWindowFactory**, and there it only supported type 
      **ReachComponent with FocusRequestable**, hence the removal of generic type support in this instance
  - **ManagedField** had a lot of problems with the compiler as well, hence the new **InputField** trait / approach
- In some component construction methods, renamed `additionalDrawers` to `customDrawers`
### Bugfixes
- **RadioButtonGroup** now uses correct alignment / insets
### New Features
- Added **ReachWindow**, which enables 100% **Reach** component layouts with no **Reflection** components being used
  - This object uses the new **ReachWindowContext** and **ReachContentWindowContext** classes
- Drag-and-drop support for external files. See **DragAndDropManager** and **DragAndDropTarget**.
- **Switches** now support light and dark mode separately
### Other Changes
- **RadioButtonLine** now requests focus when the label is clicked
- **Switch** and **RadioButton** now use antialiasing
- **Switches** are larger than previously
- `CursorDefining.defineCursorFor(...)` now allows for more precise cursor customization

## v0.5 - 02.02.2023
This update contains some style updates in cases where the original implementation wasn't very beautiful. 
Most functional updates focus on the **SelectionList** component.  
Another neat update is support for focus-based window resizing logic, 
which makes resizing windows more reasonable to use.
### Breaking Changes
- **ComponentColor**`.highlightedBy(Double)` now behaves differently. The expected default amount is now 1.0 
  (instead of 0.2 or 0.4 or something, like before)
- **InteractionWindowFactory**`.display(...)` and `.displayOver(...)` now return a tuple containing: 
  - 1: The window that was opened and
  - 2: The window close result future (i.e. the earlier return value)
- Some constructors now require an **EqualsFunction** instead of a regular function when asking for "same item -checks"
- Renamed `additionalCustomDrawers` / `additionalDrawers` -parameter to `customDrawers` in some component 
  factory functions
- Modified **SelectionList** constructors
### Bugfixes
- Some fixes to **SelectionList** visuals
- Some fixes to **FieldWithSelectionPopup** functionality
- Fix to scroll bar drawing
### New Features
- **SelectionList** now supports two new constructor parameters:
  - `highlightModifier: Double` - A modified applied to all visual highlighting
  - `alternativeKeyCondition: => Boolean` - An alternative condition for enabling arrow key events
- Added **MessageWindowFactory** trait
### New Methods
- **FocusRequestable** (type)
  - Added `.wrap(...)` and `.delegate(...)` for wrapping Reach components into FocusRequestable Reach components
- **ReachCanvas**
  - Added `.anchorPosition(...)` method, which may be used in a window constructor in order to enable 
    focus-based anchoring / window positioning.
### Other
- Visual highlighting updates to buttons and some fields
- **SelectionList** now exposes the selected area pointer
- InteractionWindowFactory now asks for a **View** instead of **Changing** as the "defaultActionEnabledPointer"

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
