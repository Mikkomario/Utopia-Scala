# Utopia Paradigm
The Utopia Paradigm -module deals with 2D and 3D shapes and mathematics, containing also other useful models.

## Parent Modules
- Utopia Flow

## Main Features

2D Shapes, Vectors and Matrices
- Greatly simplifies advanced vector mathematics
- Scala-friendly Support for basic everyday tools like **Point**, **Size** and **Bounds**
- More advanced shapes from Line to triangular and rectangular shapes to advanced 2D polygonic shapes
- Affine transformations (translation, rotation, scaling, shear) that fully support all of these shapes
- Vector projection fully supported with shape intersection and collision handling in mind
    - This feature is extended in the **Utopia Conflict** project
- Typeless value support for the basic shapes
- Includes classes for velocity and acceleration that can be used in movement vectors and physics

New Color representation
- Built-in support for both RGB and HSL color styles
- Various color transformations
- Implicit conversions between different styles
- Alpha (transparency) support

Animation support
- **Animation** and **TimedAnimation** traits for creating new animations
- **AnimatedAffineTransformation** and **TimedTransform** traits for animated mapping functions

## Implementation Hints

### What you should know before using Paradigm
At the application startup, you need to call `ParadigmDataType.setup()`. 
There's no need to call `DataType.setup()` afterwards.

### Extensions you should be aware of
- utopia.paradigm.generic.**ParadigmValue**
  - Adds additional functions to **Value**

### You should get familiar with these classes
- **Vector2D** & **Point** - Your standard way of representing points in 2D space.
- **Size** & **Bounds** - These size representations are used especially often when dealing with UI components
- **Axis** & **Axis2D** - Many shapes and features refer to this enumeration
- **AffineTransformation** & **LinearTransformation** - When you need to deal with affine
  transformations (translation, rotation, scaling, etc.)
- **Color** - When you need to deal with colors (replaces java.awt.Color)