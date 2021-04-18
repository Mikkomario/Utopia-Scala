# Utopia Reflection - List of Changes

## v2.4.1 (in development)
### Deprecations
- Deprecated `Color.isTransparent` in favor of `Color.transparent`
### New Methods
- **Color**
  - `.transparent` and `.opaque`

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