# Utopia Reflection - List of Changes

## v2.0-alpha-9 - 02.10.2022
This update mostly reflects changes in Flow v1.17 and Paradigm v1.1.  
There are also some pretty neat little changes that make a big difference in certain use-cases, 
like **ColorRole** no longer being sealed.
### Breaking Changes
- **TypeOrSearch** search function is now required to return a **Future**
- **ContainerContentDisplayer** now uses **EqualsFunction** instead of standard functions
### Deprecations
- In **Area**
  - Deprecated `.coordinateAlong(Axis2D)` and `.maxCoordinateAlong(Axis2D)` in favor of 
    `.position.along(Axis)` and `.maxAlong(Axis)`
  - Deprecated `.setCoordinate(Double, Axis2D)` and `.setLength(Double, Axis2D)` in favor of 
   ` .setCoordinate(Vector1D)` and `.setLength(Vector1D)`
  - Deprecated `.adjustCoordinate(Double, Axis2D)` in favor of `.translate(Dimensional)`
  - Deprecated `.adjustLength(Double, Axis2D)` in favor of `.size += axis(adjustment)`
### New Methods
- **Area**
  - Added a number of new methods
- **LocalStringLike**
  - Added `.nonEmptyOrElse(...)`
- **StackLength**
  - Added `.notExpanding` and `.notShrinking`
### Other Changes
- **ColorRole** is no longer sealed, enabling custom color roles
- **Area** now extends **Bounded**
- **SingleColorIcon** now extends **SizedLike**

## v2.0-alpha-8 - 18.08.2022
This update reflects changes in **Flow** v1.16 and **Genesis** v3.0. 
An important bugfix is also included, which previously caused deadlocks.
### Breaking Changes
- The following classes require an implicit **Logger** parameter
  - **LoadingWindow**
  - **ScrollCanvas**
  - **SingleFrameSetup** and **MultiFrameSetup**
  - `StackHierarchyManager.startRevalidationLoop(...)`
  - **TypeOrSearch**
### Bugfixes
- Fixed deadlock issues related to animated components

## v2.0-alpha-7 - 06.06.2022
This update reflects changes made in **Genesis** v2.7 update (which follow **Flow** v1.15 update)
### Breaking Changes
- **SingleFrameSetup** and **MultiFrameSetup** now require the implicit **ExecutionContext** parameter at constructor 
  instead of at `.start()`

## v2.0-alpha-6 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v2.0-alpha-5 - 04.11.2021
New build / supports changes in **Flow** v1.14

## v2.0-alpha-4 - 3.10.2021
This update reflects changes in the **Utopia Genesis** module.
### Breaking Changes
- Use of **Direction1D** was replaced with **Sign** (reflecting changes in **Genesis**)

## v2.0-alpha-3 (incomplete update) - 13.7.2021
This update doesn't add much concerning **Reflection**, it simply adds support for the breaking changes 
in **Utopia Flow** module v1.10.

## v2.0-alpha-2 (incomplete update) - 12.5.2021
While still working towards the v2.0 milestone, this update adds some new tools for component positioning. 
**CustomDrawer** implementations need to be altered slightly to support the changes in the **Reach** module.
### Breaking Changes
- **CustomDrawer** implementations must now specify `.opaque: Boolean`. 
  This is used in component transparency calculations
### New Methods
- **Alignment**
  - `.positionStreching(...)` as well as `.positionNextToWithin(...)` as new component positioning methods
- **StackInsets**
  - `.noMin`, `.noMax` and `.noLimits`
- **StackLength**.type and **StackSize**.type
  - `.combine(...)` for combining multiple stack lengths / sizes

## v2.0-alpha (incomplete update) - 17.4.2021
v2.0 update is still in development, but the module is completely usable.  

This release prepares for the introduction of the new **Reach** module which takes elements of 
**Reflection** even further.
### Breaking Changes
- **Area** now leaves `.bounds` implementation up to subclasses
- **CustomDrawer** class naming updated
    - Abstract drawer implementations are now named XLike (E.g. **TextDrawerLike**)
    - Immutable drawer implementations keep the previous name (E.g. **TextDrawer**)
    - Mutable drawer implementations are now prefixed with MutableX (E.g. **MutableTextDrawer**)
    - View drawers end in XViewDrawer (E.g. **TextViewDrawer**)
- Removed pointer support from mutable custom drawer implementations and added view versions of those drawers
- Removed `isAboveContent: Boolean` -parameter from **BorderDrawer** implementations and replaced it with 
  `drawLevel: DrawLevel` -parameter
    - The default draw level is now **Normal** and not **Foreground** 
- Immutable drawers are now all case classes
- Moved **SelectionCircleDrawer** from immutable package to view package and refactored and named it accordingly
- Moved **ButtonState** from utopia.reflection.component.swing.button -package to utopia.reflection.event -package
- Rearranged packages related to interaction windows
- Moved **Regex** from utopia.reflection.text to **Utopia Flow** module (utopia.flow.parse)
- `ColorContext.forTextComponents` no longer accepts parameters (except for the implicit **Localizer**)
- Changed **AnimatedLabel** creation parameters due to changes in **Image** and **Animation** classes
- Renamed multiple stack length and stack size modifiers. The new naming convention is:
  - For stack length modifiers: XLengthModifier
  - For stack size modifiers: XSizeModifier
- `Alignment.horizontalDirection` and `.verticalDirection` now return 
  **HorizontalDirection** / **VerticalDirection** respectively, instead of **Direction1D**
### New Methods
- **ColorShadeVariant**.type
  - `.forLuminosity(Double)`
- **LocalStringLike**
  - `.stripControlCharacters`
- **SingleColorIcon**
  - `.withShade(ColorShadeVariant)`
- **TextDrawContext**
  - - `.mapFont(...)` and `.mapColor(...)`
  - `.hasSameDimensionsAs(TextDrawContext)`
### Fixes
- `Alignment.position(...)` now properly takes parameter bounds position into account
- Fixed a small bug where **Window** content size wouldn't update correctly
### Other Changes
- Background custom drawer and some other custom drawers now optimize around drawer clipping zone

## v1.3 - 17.4.2021
### Breaking Changes
- Moved some of the clases in utopia.reflection.shape -package to separate sub-packages
- Removed `hideOnFocusLost: Boolean` -parameter from **Popup** and replaced it with 
  `autoCloseLogic: PopupAutoCloseLogic` parameter that accepts a wider range of options
- **Window**s now only propagate keyboard events while they're the focused window
- **ImageButton**, **TextButton** and **ImageAndTextButton** `.apply(...)` methods now accept a *call by name* 
  action instead of a function action
- Renamed component `.isVisible` (getter & setter) to `.visible`
- `TextField.contextual` constructor parameter `prompt` is now a **LocalizedString** instead of an **Option** and 
  an empty string represents the previous None value.
- Multiple breaking changes to **TextField**
- **AnimatedVisibilityChange** no longer extends **Actor** and now accepts an **ActorHandler** in `.start(...)`
### New Features
- Added **LoadingView** and **LoadingWindow**
    - Includes a new **ProgressState** class for representing background process progress
- Added **ExpandingLengthModifier** and **NoShrinkingConstraint**
- Added **RoundedBackgroundDrawer** (custom background drawer)
- Added **AwtComponentExtensions** for a few utility methods regarding awt Components
- Added hotkey support to various buttons
- Added **Slider** component
- Added **FramedImageButton** class
- Added **TagFraming** class which wraps a component in rounded frames
- Added **TagView** component for displaying word tags
- Added **TypeOrSearch** component for writing or selecting words
- Added **AwtEventThread** object for performing tasks inside the ui thread
- Added **ViewLabel** which behaves like an **ItemLabel**, except that it only displays the result of a changing 
  item and doesn't provide a mutable interface
### New Methods
- **AnimatedVisibility**
  - `.show()` and `.hide()`
- **AwtComponentRelated**
  - `.isInWindow`
- **AwtStackable**
  - `.inRoundedFraming(...)`
- **CollectionView**.type
  - A new constructor variation
- **ColorScheme**
  - `.success` and `.info`
- **ColorSet**
  - `.map(...)`
- **Framing**
  - `.addRoundedBackgroundDrawing(Color)`
- **ItemLabel**.type
  - A new constructor variation
- **LengthPriority**
  - A number of new methods
- **LocalStringLike**
  - `.isEmpty` and `.nonEmpty`
- **StackLengthModifier** and **StackSizeModifier**
  - `&&` and `.map`
- **TextContext**
  - a new variation of `.forButtons(...)`
- **Window**
  - `.isClosed`
### Deprecations
- Deprecated `StackSize.components` in favor of `StackSize.dimensions`
### Fixes
- Added missing focus gain to **InputWindow** when a component's value needs to be fixed
- Scrollable views now listen to global mouse release events, meaning they no longer get stuck in scrolling mode
- Scrollable views now only listen to action and mouse events when attached to the main stack hierarchy, 
which is more resource-effective
- **ComponentToImage** now properly draws the child components as well
- **Window** now supports (partially) transparent content by adjusting background color accordingly. This only works 
in undecorated windows and may get disabled by the OS.
- **SingleColorIcon** now properly adjusts luminosity when used as an image set in **ImageButton**
- Fixed priority handling when combining stack lengths
    - Current logic may choose the smaller optimal length if the larger length allows it via priority
- Fixed `StackSize.limitedTo(Size)` to primarily affect the maximum size only
- Many UI-affecting changes are now specifically performed in the awt event thread
### Other Changes
- `Frame.title` and `Dialog.title` now default to empty strings in constructors
- **MultiLineTextView** now extends **AwtContainerRelated**
- **ImageButton** constructors now contain an optional `isLowPriority: Boolean` -parameter
- Added rounding to `Margins.small`, `.large` and `.verySmall`
- Buttons are now triggered on key releases instead of key presses, and are highlighted before triggering so that 
the user knows which button they're activating.
- **StackSize** now extends **TwoDimensional**

## v1.2
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Major package restructuring
- ComponentContext and ComponentContextBuilder were replaced with new implementations. 
Existing components don't support old context types anymore.
- Container add methods now target specific indices
    - All custom containers need to be adjusted accordingly
- ContainerManager now only updates non-equal rows on content update 
and will insert rows where new content is inserted in the vector
    - All subclasses of ContainerManager need to be inspected since there are multiple 
    changes and new methods (most notably two different equality check levels).
    - All usages of ContainerManager, SelectionManager, and DropDownLike components 
    should also be re-examined in case equality checks or other features need to be adjusted.
        - Many classes received contentIsStateless -parameter which determines one or two 
        equality checks should be made (defaults to 1)
- DisplayFunction.interpolating -methods now use new string interpolation style and 
multiple parameter lists.
- LocalStringLike.interpolate renamed to .interpolated
    - Also, removed vararg-version of interpolation because of overlap 
- Rewrote CollectionView. The new implementation doesn't support negative directions and takes an 
Axis2D as a parameter instead of Direction2D.
- Removed useLowPriority parameter from AlignFrame. Now all frames expand the content when necessary.
- Moved Insets and Screen classes from Reflection to Genesis
- Changed constructor parameter ordering in Switch
- Refactored color handling in context classes
### Deprecations
- StackSelectionManager was deprecated. You should now use ContainerSelectionManager instead.
    - ContainerSelectionManager supports a wider range of containers.
- SegmentedRow and SegmentedGroup were deprecated since new implementations (SegmentGroup) were added
### Major Changes
- DropDownLike now uses animated stack instead of normal stack in its pop-up view.
### Fixes
- TextLabel now calls repaint upon layout update 
- Made StackHierarchyManager more thread-safe, although there are still probably non-throwing issues
### New Features
- SegmentGroup
    - Replaces old SegmentedRow and SegmentedGroup implementations by wrapping components instead of 
    imitating a stack. Wrapped components may then be placed within stacks or other containers.
    - Used by creating a new SegmentGroup and by calling .wrap(...) for each generated row of components
    - Segment dependency management is now automatic and you don't need to manually register or unregister 
    components from the segments.
- AnimatedStack
    - Animates new component additions and component removals
- AnimatedCollectionView
    - Animates new component additions and component removals
    - Row addition animations don't yet work as they should since the animated image is not updated.
- AnimatedSizeContainer
    - By wrapping components with this container, you will be able to 
    use animated content resizes.
- AnimatedVisibility
    - By wrapping a component in AnimatedVisibility, you can animate component 
    show & hide events.
- AnimatedTransition
    - An appearance / disappearance transition for a component. 
    Used in AnimatedVisibility, which is oftentimes easier to use. 
        - AnimatedTransition is better, however, when you only need a singular transition.
- MappingContainer, WrappingContainer & AnimatedChangesContainer for wrapping other containers and adding 
animations.
- CollectionViewLike trait for adding custom collection views for both Swing and non-swing approaches
- ContentDisplayer and ContainerContentDisplayer for features similar to ContentManager, except read-only.
- AnimationLabel
    - Can be used for animated drawing (Sprites, rotating images etc.)
- InteractionWindow traits and classes for window-based user interaction
- Added smoother animations to Switch and included same animations to ProgressBar
- Added AnimationLabel
- Added AnimatedSwitchPanel
### New Methods
- AwtComponentWrapper: toImage
    - Draws a component to an image. This feature is also available via 
    ComponentToImage object.
- LocalStringLike.interpolate(Map)
    - String interpolation is now available for key value pairs and new string syntax. 
### Other Changes
- ContainerContentManager now accepts any MultiContainer with Stackable and not just MultiStackContainers
- TextFields now downscale their prompts if they would not fit into bounds.
- Tweaks to component coloring (Eg. Button hover color)
- Component creation and context class default values are now defined in ComponentCreationDefaults
