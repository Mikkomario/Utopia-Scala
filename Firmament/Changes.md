# Utopia Firmament - List of Changes

## v1.2 (in development)
Supports **Flow v2.3**
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
- Fixed certain issues with cropped image drawing due to rounding errors
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
