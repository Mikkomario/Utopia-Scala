# Utopia Firmament - List of Changes

## v1.4 (in development)
Working on variable context
### Breaking changes
- All context classes, except for **WindowContext** now have a static and a variable version
  - Variable context classes replace use cases where context pointers were used, previously
  - In most situations, you will need to replace the usage of a context class (e.g. **TextContext**) with its 
    static variant (in this case, **StaticTextContext**). In cases where pointers were used, those should/must 
    be replaced with variable context classes. E.g. **Changing ColorContext** is replaced with **VariableColorContext**.
- Divided **FramedFactory** into 3 versions:
  - **FramedFactory**, which doesn't specify whether the implementation is static or uses pointers
  - **StaticFramedFactory**, which matches the previous **FramedFactory** implementation
  - **VariableFramedFactory**, which supports pointers
  - These also have contextual variants
- **ViewImageDrawer** now has `insetsPointer: Changing` instead of `insetsView: View`
### New features
- **Window** now supports preparation before size changes
  - This is utilized by passing a `prepareForSizeChange` -property in the constructor
- Added **HasContext** trait from **Reach**
### New methods
- **AwtEventThread**
  - Added `.later(...)`, which invokes `SwingUtilities.invokeLater(Runnable)`
- **TextDrawContext**
  - Added `.withColor(Color)`
- **Window**
  - Added `.boundsUpdatingFlag`
### Other changes
- **SingleColorIcon** (and its variants) now has more context support functions

## v1.3.1 - 04.10.2024
A small update which adds component logging support, since most Flow-originating pointers now require it. 
Also includes minor utility updates.
### New features
- Added **HasGuiState** trait
### New methods
- **GuiElementStatus**
  - Added multiple new utility getters & setters
### Other changes
- Built with Scala v2.13.14
- **ComponentCreationDefaults** now contains `implicit var componentLogger: Logger`, 
  which is used for logging errors within GUI-components and systems
  - This property is implicitly available to all instances of **Component**
- **Component** now contains a protected `relativizeMouseEventForChildren(...)` function.
  - Overriding this function affects how mouse events are distributed downwards.
- Modified caching logic in highlighting **ButtonImageEffect**

## v1.3 - 28.07.2024
For the most part, this update reflects changes in the **Genesis** module (v4.0 update). 
The new features in **Genesis** are utilized, for example in **HotKey** and **ImageDrawer**. 

Besides this, this update introduces a couple quality updates to **StackLength** + **StackInsets**, 
as well as some basic GUI improvements. 

### Breaking changes
- Moved **DrawLevel** to **Genesis**
- **HotKey** no longer contains property `.characters` and now uses **Key** instead of key index (Int)
- **ImageDrawer** is now a trait and not a class
- In some instances where **Vector** was used, **Seq** is now used
### Deprecations
- Deprecated **ImageDrawerLike** in favor of the new **ImageDrawer** trait / implementation
- Deprecated **ImageViewDrawer** in favor of the new **ViewImageDrawer**
### New features
- Image drawers now support transformations (most importantly rotation)
### New methods
- **StackInsets**
  - Added `.min(StackInsets)` and `.max(StackInsets)`
### Other changes
- This module is no longer dependent from the **Inception** module
- **StackLength**`.min(StackLength)` and `.max(StackLength)` now prioritize shrinking or expanding based on their direction
- `.setToCloseWhenClickedOutside()` in **Window** now only activates on mouse-presses, no longer on mouse-releases
- GUI element highlighting is now less pronounced after the first stage.
  - E.g. When button is pressed with a mouse, the pressed stage more resembles the hover stage,
    while still being noticeably different

## v1.2 - 22.01.2024
Redesigned & rewrote how image effects are applied to buttons. 
Also refactored **StackInsets** a lot. Added a new version of image-caching as well.
### Breaking changes
- Modified **StackInsets** class hierarchy, which may also affect some method calls  
### Deprecations
- Deprecated **ButtonImageSet** constructors
  - The image effects are now applied **after** the initial (identity) set has been created
    - E.g. `ButtonImageSet.lowAlphaOnDisabled(Image, Double)` is now written as 
      `ButtonImageSet(Image).lowAlphaOnDisabled`
- Renamed all **ButtonImageSet** properties
  - E.g. `.defaultImage` is now `.default`
- Renamed `.withLowPriority` to `.lowPriority` in **StackLength** and **StackSize**
- Deprecated **SingleColorIconCache** in favor of the new **ImageCache**
- Renamed `.withDefaultPriority` to `.normalPriority` in **StackLength**
- Deprecated `Border.square(Double, Color)`, as this was a duplicate of `.symmetric(Double, Color)`
### Bugfixes
- Fixed certain issues with image drawing that were due to rounding errors
### New features
- **ButtonImageSet** now supports combined image effects
  - This includes a new effect called **ChangeSize**, which modifies image/icon size based on button state
- Added **ImageCache** class, which supports a larger variety of image- and icon-reading processes 
  than the previously used **SingleColorIconCache**
- Added **FromColorFactory** trait
### New methods
- **Border** (object)
  - Added `.apply(Double, Color)`
- **ButtonImageSet**
  - Added mapping functions for individual images
- **ComponentCreationDefaults**
  - Added `.inButtonImageEffects` and `.asButtonImageEffects`
- **SingleColorIcon**
  - Added more color-based apply functions to button-related factories
- **StackLength**
  - Added `&&(StackLength)`
### Other changes
- The image effects listed in **ComponentCreationDefaults** now affect **SingleColorIcon** **ButtonImageSet** 
  conversion functions
- **Window**`.fullyVisibleFlag` now becomes permanently false when the window is closed
- Slightly refactored **Window**`.openFlag` so that it becomes permanently false once the window is closed
- Scala version updated to 2.13.12

## v1.1 - 27.09.2023
This update introduces a new size-altering system (**StandardSizeAdjustable**), 
as well as certain important **Window**-related bugfixes.  
The AWT event thread interface was also refactored for better stability. 
### Breaking Changes
- **SingleColorIcon** default construction parameters changed
  - Please also note that **SingleColorIcon** is now a case class and the `new` keyword should no more be used
- The icon parameter **WindowButtonBlueprint** is no longer an **Option**. An empty **SingleColorIcon** acts as "no icon"
- **Margins** now accepts **Adjustment** as the second parameter, not **Double**
- **StackLike** no longer extends **AreaOfItems**. Please use **StackItemAreas** instead.
- The functions `.larger` and `.smaller` in **BaseContextLike** now require an implicit **Adjustment** parameter
- **SingleColorIconCache** now requires an implicit **Logger** parameter
- **TextContextLike** now requires additional properties
- **ButtonBackgroundViewDrawer** now accepts the borderWidth as a pointer and not a static value
### Bug Fixes
- The adjustment modifier in **Margins** didn't work as described in the documentary
- **Window** didn't always properly update layout or repaint content - fixed
### Deprecations
- In **BaseContext**, deprecated methods that referred to stack margins as plural in favor of new renamed functions
  - E.g. `.withStackMargins(...)` is now `.withStackMargin(...)`
### New Features
- Added new **SizeCategory** and **StandardSizeAdjustable** classes
  - **SingleColorIcon** now extends **StandardSizeAdjustable**, offering a number of new utility functions
- **TextContext** classes now support automated line-splitting
  - Please note, however, that not all components necessarily follow this property, especially so in **Reflection**
- You can now specify a custom **Logger** implementation for the **AwtEventThread** -interface
  - See `AwtEventThread.logger`
### New Methods
- **BaseContext** & **TextContext**
  - Added multiple utility functions that utilize the new **SizeCategory** class
- **DisplayFunction** (object)
  - Added `.option(DisplayFunction, LocalizedString)`
- **FocusListener** (object)
  - Added new constructors
- **LocalizedString** (object)
  - Added `.alwaysEmpty`
- **Margins**
  - Added multiple new methods that utilize **SizeCategory**
- **SingleColorIcon** (object)
  - Added `.alwaysEmpty`
- **Size** (**LengthExtensions**)
  - Added methods for converting **Sizes** to **StackSizes**
### Other Changes
- **Window** mouse move events are now only fired while the window has focus, except in windows that can't gain focus.
- Button hotkeys are now only triggered while the applicable window has focus
  - This is configurable for each hotkey (see `.triggeringWithoutWindowFocus` in **HotKey**)
  - Non-focusable windows are excluded from this rule
- Modified how **Window** bounds get updated and when the **Window**'s `openedFlag`, `openFlag` and `openFuture` resolve
- Added better exception-handling to the **AwtEventThread**
  - Exceptions are no longer propagated to the AWT event thread itself, except for **InterruptedExceptions**
- **SingleColorIconCache** now logs errors and appends ".png" to file names in case the file extension is not specified
- **SingleColorIcon** now extends **MayBeEmpty**
- **CachingStackable** now rounds stack size values before caching them (this update may be removed in the future)
- **BorderDrawers** now round the border sizes before drawing
  - This should reduce the number of drawing problems in components that use borders.

## v1.0 - 01.05.2023
Initial version  
The following features were moved over from **Reflection**
- Generic component traits
- Generic container traits
- Stacking
- Component creation context classes (which were rewritten)
- Localization classes
- Icon support
- Custom drawing tools and traits
- Generic container implementation templates
- Container content management tools and traits
- Awt utilities

Basically everything that was not exclusive to **Reflection** was moved over to upper modules.  
This is in order to separate **Reach** from **Reflection**.
