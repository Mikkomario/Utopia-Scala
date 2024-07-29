# Utopia Paradigm
The Utopia Paradigm -module deals with 2D and 3D shapes and mathematics, also containing other useful models.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)

## Main Features

2D Shapes, Vectors and Matrices
- Greatly simplifies (advanced) vector mathematics
- Scala-friendly support for basic everyday tools like 
  [Point](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/vector/point/Point.scala), 
  [Size](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/vector/size/Size.scala) 
  and [Bounds](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/area/polygon/c4/bounds/Bounds.scala)
- More advanced shapes from [Line](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/line/Line.scala) 
  to triangular and rectangular shapes and advanced 2D polygonic shapes
- Affine transformations (translation, rotation, scaling, shear) that fully support all of these shapes
- Vector projection fully supported with shape intersection and collision handling in mind
    - This feature is extended in the [Utopia Conflict project](https://github.com/Mikkomario/Utopia-Scala/tree/master/Conflict)
- Typeless value support for the basic shapes
- Includes classes for velocity and acceleration that can be used in movement vectors and physics

[Angle](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/angular/Angle.scala) and 
[Rotation](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/angular/Rotation.scala) representations
- Automatic conversion between radians and degrees
- Automatic wrapping between 0 and 360 degree angles
- Directional and non-directional **Rotation** classes

Enumerations for [Alignment](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/enumeration/Alignment.scala) 
and direction
- Supports both 1D and 2D **Alignment** variants

Utility tools for converting between metric and imperial system distances, as well as on-screen pixels

New [Color](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/color/Color.scala) representation
- Built-in support for both RGB and HSL color styles
- Various color transformations
- Implicit conversions between different styles
- Alpha (transparency) support
- Support for color sets and color schemes useful for creating aesthetic GUIs

Animation support
- [Animation](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/animation/Animation.scala) 
  and **TimedAnimation** traits for creating new animations
- **AnimatedAffineTransformation** and **TimedTransform** traits for animated mapping functions

## Implementation Hints
At the application startup, you need to call `ParadigmDataType.setup()`.

### Extensions you should be aware of
- [utopia.paradigm.generic.ParadigmValue](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/generic/ParadigmValue.scala)
  - Adds additional functions to **Value**
- [utopia.paradigm.measurement.DistanceExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/measurement/DistanceExtensions.scala)
  - Enables the creation of various distances with simple syntax (e.g. `3.5.m`, which signifies 3 and a half meters)

### You should get familiar with these classes
- [Vector2D](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/vector/Vector2D.scala) & 
  **Point** - Your standard way of representing points in 2D space.
- **Size** & **Bounds** - These size representations are used especially often when dealing with GUI components
- [Axis](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/enumeration/Axis.scala) & 
  **Axis2D** - Many shapes and features refer to this enumeration
- [AffineTransformation](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/transform/AffineTransformation.scala) & 
  [LinearTransformation](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/transform/LinearTransformation.scala) - 
  When you need to deal with affine transformations (translation, rotation, scaling, etc.)
- **Color** - When you need to deal with colors (replaces java.awt.Color)
- **Angle**, **Rotation** & 
  [DirectionalRotation](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/angular/DirectionalRotation.scala)