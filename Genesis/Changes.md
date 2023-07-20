# Utopia Genesis - List of Changes

## v3.4 (in development)
### Breaking Changes
- New parameter added to **MeasuredText** constructor 
### Bugfixes
- Bugfix to **MeasuredText**`.caretIndexToCharacterIndex(Int)`
### New Features
- Added automatic line-splitting -feature to **MeasuredText**

## v3.3 - 01.05.2023
This version introduces a completely rewritten **Drawer** implementation with a new interface.
### Breaking Changes
- Rewrote **Drawer**
  - This change will require a lot of refactoring
- **MutableTransformable** now uses **AffineTransformation** instead of **Transformation**
- **Animator** is now an abstract class
### New Features
- Added mouse drag -tracking. See **MouseDragListener** and **GlobalMouseEventHandler**
- **Drawer** now supports antialiasing
### New Methods
- **Image**
  - Added `.cropped`, which removes all surrounding fully transparent pixel rows and columns

## v3.2 - 02.02.2023
This update introduces the rewritten version of **PixelTable**, named **Pixels**.  
There are a number of breaking method name changes included, 
but the new interface is overall more flexible than the previous and should be of more use.
### Breaking Changes
- Rewrote **PixelTable** as **Pixels**, which is now used by the **Image** classes
- Renamed **Image**`.mapPixels(...)` to `.mapEachPixel(...)`
- Renamed **MutableImage**`.updatePixels(...)` to `.updateEachPixel(...)`
- **Image**`.mapPixelsWithIndex(...)` and **MutableImage**`.updatePixelsWithIndex(...)` 
  now provide a **Pair** instead of a **Point** for the specified function.
  - Added new `.mapPixelPoints(...)` and `.updatePixelPoints(...)`, respectively, to compensate for this change
- Removed `.preCalculatedPixels` from **ImageLike**, as **Pixels** now supports lazy initialization
### Deprecations
- Deprecated **PixelTable** in favor of **Pixels**
- Deprecated **Image**`.mapPixelTable(...)` in favor of `.mapPixels(...)`
- Deprecated **MutableImage**`.updatePixelTable(...)` in favor of `.updatePixels(...)`
### New Methods
- Added new `.fromPixels(Pixels)` constructor to **Image**

## v3.1 - 02.10.2022
This update reflects the changes in **Flow** v1.17 and **Paradigm** v1.1.  
Most important updates concern the **Image** class, which had some of its methods renamed / replaced.
### Breaking Changes
- **Image**`.filling(Size)` no longer forces downscaling if the image already fills the specified area
### Bugfixes
- Empty **MeasuredText** now has a positive height, like in the previous **Reflection** implementation
### Deprecations
- In **Image**:
  - Deprecated .fitting(Size) in favor of .fittingWithin(Vector2DLike, Boolean)
    - Notice the change in functionality!
  - Deprecated `.smallerThan(Size)` and `.largerThan(Size)` in favor of 
    `.fittingWithin(Vector2DLike)` and `.filling(Dimensional)`
  - Deprecated `.limitedAlong(Axis2D, Double)` in favor of `.fittingWithin(Vector1D)`
    - Also deprecated `.withLimitedWidth(Double)` and `.withLimitedHeight(Double)` in favor of 
      `.fittingWithinWidth(Double)` and `.fittingWithinHeight(Double)`
      - Notice the change in functionality!
### Other Changes
- **ImageLike** now extends **Sized**, and **Image** extends **SizedLike**

## v3.0 - 18.08.2022
A number of shape classes were moved to the new **Paradigm** module in this update. Applying this update will require 
quite a lot of refactoring, unfortunately.
### Breaking Changes
- Moved a number of classes to the new **Paradigm** module
- Removed the **Extensions** object
- The following classes require an implicit **Logger** -parameter:
  - **ActorLoop** and **RepaintLoop**
  - **Canvas**`.startAutoRefresh(...)`
  - **DefaultSetup**

## v2.7 - 06.06.2022
This update mostly reflects changes in **Flow** update v1.15.  
Alignment functionality was also moved to **Genesis** from **Reflection** and refactored somewhat.
### Breaking Changes
- **ActorLoop** and **RepaintLoop** now extend **LoopingProcess** instead of **Loop**
  - This means that `.startAsync()` is no longer available (replaced with `.runAsync()`)
  - Also, the implicit **ExecutionContext** parameter is now required during instance construction, 
    instead of at loop start
### New Features
- Added **Alignment** and **LinearAlignment** classes, based on existing **Reflection** solutions
### New Methods
- **Bounds**.type
  - Added a new constructor variant

## v2.6.2 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v2.6.1 - 04.11.2021
Supports changes in Flow v1.14

## v2.6 - 3.10.2021
In this major (refactoring) update, many of the previously used traits were deprecated and new versions were added 
to the **Flow** module. Shape classes were also refactored to use the new **Pair** class from **Flow**. These 
updates will most likely require refactoring from your part, at least if you've extended some traits 
introduced in this module.
### Breaking Changes
- **Direction1D** was replaced with **Sign** in **Utopia Flow**
  - The new enumeration works almost identically (with some additions)
  - Existing classes **won't** accept **Direction1D** anymore, causing build errors before they are refactored
- **Scalable**, **Combinable**, **Arithmetic**, **Signed** and **DistanceLike** were replaced 
  with **Utopia Flow** counterparts
  - This has many implications, although for the most part, renaming and replacing extensions should be enough
- **TwoDimensional** was replaced with **MultiDimensional**. Another **TwoDimensional** trait was added that 
  applies to items with **exactly** two dimensions (**Vector2D**, **Point**, etc.).
### Other Changes
- Two-dimensional structures are now based on a **Pair** instance instead of two separate values. 
  The interface remains the same, however.
- Many shape classes have received new methods due to added traits from **Flow**

## v2.5.1 - 13.7.2021
This is a relatively small update for the **Genesis** module, since the planned update on drawing 
didn't make it to this release.
### Features in development
- Contents of the `utopia.genesis.graphics` package are **incomplete and not fit for use** at this time. 
  Please wait for the complete release before utilizing these features.
### Other Changes
- Added **Bounded** trait that defines function `.bounds`. Both **Polygonic** and **Circle** extend this trait.
- `Bounds.bounds` now returns the bounds itself, which is more efficient than recalculating the same bounds.

## v2.5 12.5.2021
This update adds a relatively major refactoring on **Animation** trait class structure, 
and also adds a number of new utility methods and features.
### Breaking Changes
- **TimedAnimation** and **Path** no longer extend **Animation** but instead extend new trait **AnimationLike**
- There are other animation -related breaking changes also
### Deprecations
- Deprecated `Color.isTransparent` in favor of `Color.transparent`
### New Features
- **Images** can now be saved to files with `.writeToFile(Path)`
- **SinePath** added
### New Methods
- **Angle**.type
  - `.average(Iterable[Angle])`
- **Color**
  - `.transparent` and `.opaque`
- **Line**
  - `.xForY` and `.yForX` function attributes for using lines as linear functions
- **Rotation**.type
  - `.average(Iterable[Rotation])`

## v2.4 - 17.4.2021
With the introduction of the new **Reach** module, multiple additions and changes were necessary in 
**Genesis** as well. These include a wide range of practical and useful updates, 
although many of them also require you to update your code.
### Breaking Changes
- **MouseEventGenerator** and **CanvasMouseEventGenerator** no longer accept listeners as parameters. 
  Instead, they now create new mutable mouse event handlers.
    - Also, **MouseEventGenerator**s now take an implicit execution context as a parameter
- Added absoluteMousePosition -parameter to all **MouseEvent**s
- **Path** no longer extends **DistanceLike**
- Renamed **RGB**, **RGBLike**, **RGBChannel** and **RGBTransform** to **Rgb**... (camel case)
- Renamed HSL and HSLLike to **Hsl** and **HslLike**
- **KeyTypedEvent**s now contain the associated key index
- Added origin to **Image**, therefore removing separate origin features from animation and image drawing classes
- Changed `Bounds.translate` -methods since there was an ambiguous overlap when **Vector2D** was used 
- `Setup.start()` no longer accepts an implicit execution context. Usually such context is now requested 
  as a constructor parameter instead.
- Moved velocity and acceleration -related classes to package utopia.genesis.shape.shape2D.movement
- Replaced **Transformation** and **AnimatedTransformation** classes with **Matrix2D**, **Matrix3D**, 
  **LinearTransformation**,**AffineTransformation**, **AnimatedLinearTransformation** and 
  **AnimatedAffineTransformation**
  - Renamed **Transformable** to **MutableTransformable** and deprecated it
  - Deprecated **TransformProjectable** in favor of **LinearTransformable**, **AffineTransformable** 
    and **Transformable**
### New Features
- Added global mouse and keyboard event handling with **GlobalMouseEventHandler** and 
  **GlobalKeyboardEventHandler** objects
    - Asynchronous keyboard event handling is possible after calling 
    `GlobalKeyboardEventHandler.specifyExecutionContext(ExecutionContext)`
- Added **SegmentedPath** for path / animation based on a set of values
- **Drawer** now supports splitting drawn text into multiple lines
    - Also, added a couple new string drawing methods
- Added transformation support to image drawing
- Added color contrast calculation and related classes (See `.contrastAgainst(RgbLike)` in **RgbLike** / **Color**)
### Deprecations
- **ConvertingKeyListener** was deprecated in favor of **GlobalKeyboardEventHandler**
### New Methods
- **Bounds**
  - `.ceil`
  - `.minAlong(Axis2D)` and `.maxAlong(Axis2D)`
  - `.overlapsWith(Bounds)`
  - `.toRoundedRectangleWithRadius(Double)`
- **Color**.type
  - `.averageLuminosityOf(...)`
- **Direction1D**
  - `.opposite`
- **Direction2D**
  - A number of new utility methods
- **Drawer**
  - `.clipBounds`
  - `.copyArea(...)`
  - `.translated(Vector2DLike)`
- **Image**
  - Object
    - `.draw(...)`, which can be used for drawing custom images
  - Class
    - `.withOverlay(Image, Point)` (can also be called with +)
- **InsetsFactory**
  - new variation of `.symmetric(...)`
- **KeyStateEvent**
  - `.arrow`
- **KeyStateListener**.type
  - `.oneTimeListener(Filter)(...)`
- **MovementHistoryLike** and **VelocityTracker**
  - A couple new utility methods
- **PixelTable**
  - A couple new utility methods
- **Rgb**
  - new variations of `.average(...)`
    - Also added copies of those methods to **Color**
- **TwoDimensional**
  - `.toMap2D`
### Fixes
- `Dimensional.toMap` now properly returns a **Map**
### Other Changes
- **Bounds** and **Point** `.toAwt` conversion now rounds the double values to the closest integers
- **Direction1D** now extends **SelfComparable**
- **Bounds** + -method now accepts **VectorLike** instead of only **Point** 
- Added implicit conversion from a function to a **KeyTypedListener**
- **Parallelogramic** now produces **Parallelogramic** instead of **Parallelogram** when transformed
- **PixelTable** now extends **Iterable(Color)**
- Added implicit conversion from awt **Rectangle** to **Bounds**
- **Direction2D** now contains **HorizontalDirection** and **VerticalDirection** sub-traits

## v2.3
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Major package restructuring in shape -package
- Added Vector2D, which now replaces Vector3D in many places
- Divided Velocity into Velocity2D and Velocity3D
- Divided Acceleration into Acceleration2D and Acceleration3D
- Multiple changes in Vector-related traits
- **Drawer.drawTextPositioned** now accepts a Bounds instead of Position from the function return 
value. Added appropriate text scaling to the method.
- Renamed FPS to Fps
- Renamed Distance to DistanceLike
- Multiple changes to Angle and Rotation, main idea of which is to make sure the value of 
both always stays positive
    - In the case of Angle, the internal value is always from 0 to 360 degrees
- HSLLike items (HSL, Color) now use Angle instead of Double as hue
### New Features
- ProjectilePath & SPath
    - Simple classes which allows you to easily create smooth, non-linear paths between (0,0) and (1,1)
- Direction1D
    - A very simple enumeration for one dimensional direction (forward / backward)
- BezierFunction
    - Allows you to create a smooth x-y function based on specified points
- Distance & Ppi
    - Allow you to define lengths in centimeters, inches, etc. and then convert those to pixels using 
    screen ppi (pixels per inch)
### Deprecations
- Direction2D.isPositiveDirection
    - Replaced with .sign and Direction1D
    - Some related methods have also been deprecated
- Various deprecations in Angle and Rotation
- Direction1D.signModifier is now .modifier. Previous .signModifier is now deprecated.
### Other Changes
- Drawer.drawTextCentered now properly downscales the text if it was to go out of bounds.
- Moved Insets and Screen classes from Reflection to Genesis
- Added some new utility functions to Angle and Rotation
- Added a couple of new size methods to Image
