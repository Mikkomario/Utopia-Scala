# Utopia Reflection - List of Changes
## v2.4 (beta)
### Breaking Changes
- MouseEventGenerator and CanvasMouseEventGenerator no longer accept listeners as parameters. Instead, they now create 
new mutable mouse event handlers.
- Added absoluteMousePosition to all MouseEvents
### New Features
- Added global mouse and keyboard event handling with GlobalMouseEventHandler and GlobalKeyboardEventHandler objects
### Deprecations
- ConvertingKeyListener was deprecated in favor of GlobalKeyboardEventHandler
### New Methods
- Added .toRoundedRectangleWithRadius(Double) to Bounds
- Added .oneTimeListener(Filter)(...) to KeyStateListener
### Other Methods
- Bounds and Point .toAwt conversion now rounds the double values to closest integers

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