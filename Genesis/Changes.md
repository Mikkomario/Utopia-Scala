# Utopia Reflection - List of Changes
## v2.3 (beta)
### Breaking Changes
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
### Other Changes
- Drawer.drawTextCentered now properly downscales the text if it was to go out of bounds.
- Moved Insets and Screen classes from Reflection to Genesis