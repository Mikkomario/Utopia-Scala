# Utopia Reach - List of Changes

## v1.4 (in development)
### Breaking changes
- **ReachComponentLike** now requires an abstract property `mouseDragHandler: MouseDragHandler`
  - This doesn't affect most use-cases, because **ReachComponent** and **ReachComponentWrapper** both implement this
- **ComponentHierarchy** now requires an abstract property `coordinateTransform: Option[CoordinateTransform]`
  - In the vast majority of the use-cases, this is set to `None`
### New features
- Components now support mouse drag events
- Added a **Slider** component
- Added **Rotated** container which rotates the wrapped component 90 degrees clockwise or counter-clockwise
- **ComponentHierarchy** now supports coordinate transformations
- Added **HasGuiState** trait
### New methods
- **OpenComponent**
  - Added new variant of `.attachTo(...)`, which supports hierarchy-replacing
- **PartOfComponentHierarchy**
  - Added `.linkedFlag`
### Other changes
- Built with Scala v2.13.14

## v1.3 - 28.07.2024
This update follows changes introduced in **Genesis v4.0**, which mostly affect the component 
(mouse & keyboard) event handling, as well as image-drawing.

Besides this, larger updates were applied to **ViewTextButton** and the **Focusable** traits.

### Breaking changes
- Refactored **ViewTextButton** constructors
  - Removed the non-contextual factory altogether
  - The contextual constructor is now pointer-based
  - The contextual factory now uses **ButtonSettings** and extends **CustomDrawableFactory**
- All **FocusableWithState** implementations are now required to implement the `focusPointer: FlagLike` property
- **ButtonLike** implementations are now required to implement `.enabledPointer: FlagLike`
- **ComponentHierarchy**`.linkPointer` is now type **FlagLike** instead of **Changing**
- Moved **FramedFactory** to the **Firmament** module
- Moved **PaintManager** and **Priority** to the **Genesis** module
- **ImageLabelSettingsLike** and related settings-like traits now require a transformation to be specified 
- In some instances where **Vector** was used, **Seq** is now used
### Deprecations
- Deprecated **FocusableWithPointer**, as it is now redundant
### Bugfixes
- Fixed an issue where image + text component insets would not receive the expanding or shrinking -property
- Previously **DragTo** would always reposition the component, regardless of settings used
- **RadioButtons** now correctly support **HotKey**s
### New features
- Image labels now support transformations
### Other changes
- This module is no longer dependent from the **Inception** module
- **ReachComponent** is now a trait - however, it is still intended to be used like an abstract class

## v1.2 - 22.01.2024
This update focuses on image-drawing, especially in buttons. 
Following the changes in **Firmament**, button image effects are applied differently. 
Also, the generated **ImageAndTextButtons** will now look different because of the insets updates.

Other new features may also be of interest to you: Support for borderless window-resizing & 
repositioning with **DragTo**, cursors with automatically drawn edges and a **Layers** container for 
overlapping components (such as inner dialogs (coming later)).
### Breaking changes
- **Image** to **ButtonImageSet** -related effects are now separately added to image button (-like) factories
  - The default effects applied are determined by **ComponentCreationDefaults**
- **ImageAndTextLabel** and related classes now support common insets, 
  as well as custom margin between the image and the text
  - The factory classes were modified in order to support this, 
    so both the construction syntax and the outcome may vary from previous
- Refactored **ImageAndTextButton** (including the view-based version) to utilize the new label syntax
  - Construction syntax, as well as the output will vary from the previous version
- Renamed **LayeredView** trait to **Layers**
  - Also, the trait no longer accepts a generic type parameter
### Bugfixes
- **ReachCanvas** in Swing context now properly fires mouse events
- Fixed cursor shade calculation in **MutableImageButton**, **ViewImageButton** and **SelectionList**
- Fixed an issue where drop-downs would expand too much vertically
### New features
- Added **DragTo** utility tool, which allows the user to 
  resize and reposition borderless windows and other components using the mouse
- Added support for cursors with edges
  - To use this, specify the `drawEdgesFor` -parameter in `CursorSet.loadIcons(...)` or use 
    `Cursor(SingleColorIcon, Boolean)`
- Added a new **Layers** -container
  - This container is used for presenting multiple components on top of each other
  - Currently only the static container version is available
- **ImageLabel** factories can now more easily be converted to **ViewImageLabel** factories by calling `.toViewFactory`
  - Similar addition was made to **ImageAndTextLabel**, as well as **ImageAndTextButton**
- Added **MousePositionDrawer**, which may be used for debugging mouse-related bugs
### New methods
- **Stack** (factory)
  - Added `.withMargin(Option[SizeCategory])`
### Other changes
- Cursor shade-detection algorithm is now much simpler, resulting in faster code and smaller memory use, 
  with the cost of less accurate cursor color
- **SelectableTextLabel** now selects the words only 
  when clicking twice on the same spot within a short (0.5 s) period of time.
- Added a couple of new utility functions to button factories
- Multiple view-based components now use slightly more optimized pointer-mapping 
  by checking the hierarchy link condition before applying the effects
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
