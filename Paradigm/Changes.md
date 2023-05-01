# Utopia Paradigm - List of Changes

## v1.3 (in development)
This update introduces new color classes (which are originally from **Reflection**), as well as some 
changes to the **Alignment** classes.
### Breaking Changes
- **Alignment**`.position(Size, Bounds)` now returns **Bounds** instead of **Point**
- **Dimensions**`.zeroValue` is now calculated lazily. This causes some breaking changes:
  1. **Dimensions** is no longer a case class
  2. **Dimensions** first constructor parameter is different
  3. **DimensionsBuilder** constructor parameter is different
### New Features
- New color features were moved over from **Reflection** and rewritten. 
  - This includes **ColorScheme**, **ColorLevel** and **ColorRole**
## New Methods
- Added a number of new positioning methods to **Alignment** and **LinearAlignment**
### Other Changes
- **Alignment** now extends **Dimensional**

## v1.2 - 02.02.2023
This update reflects in style the **Flow** v2.0 update. Multiple classes and concepts have been rewritten 
and a more intuitive naming logic is applied.
### New Methods
- **Bounds** (type)
  - Added `.between(Pair)`

While there are a fair number of breaking changes, you shouldn't encounter too many, unless you have created 
your own shape classes which extend from the base traits. 
I.e. the external interfaces to many updated classes remain very similar.
### Breaking Changes
- Rewrote **Dimensional**, **VectorLike** and similar traits completely
  - **Dimensional** is now **HasDimensions** (a new trait with name **Dimensional** was also introduced)
  - **VectorLike** is now **DoubleVectorLike**
- **Bounds** now extends **Dimensional**, causing `.x` and `.y` to function differently
- Moved all **ParadigmDataType** objects under the **ParadigmDataType** object
- Renamed **Sized** to **HasSize** and **Bounded** to **HasBounds**
  - Renamed **SizedLike** to **Sized** and **BoundedLike** to **Bounded**
- Made a number of changes to **InsetsLike**:
  - **InsetsFactory** now only takes two type parameters
  - Renamed `.makeZero` to `.zeroLength`
  - Renamed `.makeCopy(...)` to `.withAmounts(...)`
  - Renamed `.combine(...)` to `.plus(...)`
  - Renamed `.sides` to `.lengths`
  - **InsetsLike** now extends **Dimensional**
### Deprecations
- Deprecated `.darkened(Double)` and `.lightened(Double)` in **HslLike** 
  in favor of `.darkened`, `.lightened`, `.darkenedBy(Double)` and `.lightenedBy(Double)` in **Color**
### New Methods
- **Angle**
  - Object
    - Added new `.ofCircles(Double)` constructor
  - Class
    - Added `.relativeTo(Angle)`
- **Axis**
  - Added `.sign` -property
- **Bounded**
  - Added `.shiftedInto(Bounds)` and `.fittedInto(Bounds)` -functions
- **Circle** (type)
  - Added `.zero` -property
- **Color**
  - Added darkening, lightening and highlighting functions
- **HasBounds**
  - Added `.centerLeft`, `.centerRight`, `.centerTop`, `.centerBottom`, `.centerX` and `.centerY` -properties
- **HslLike**
  - Added `.darkness` and -related functions (darkness is just inverse of luminance)
- **Insets**
  - Added `.mapWithDirection(...)`
- **Line** (type)
  - Added `.lenDir(HasInclusiveEnds[Double], Angle)` constructor
### Other Changes
- **Circle** now has a default value for origin

## v1.1 - 02.10.2022
This is a relatively major update, most important addition in which is the **Vector1D** class.  
This update also includes a number of size and bounds -related utility functions.  
Also, a number of methods were renamed, with the previous versions deprecated.
### Breaking Changes
- **Dimensional** -trait changed in ways that require changes from the implementing classes:
  - `.zeroDimension` is now **public** instead of protected
  - `.dimensions` is now required to return an **IndexedSeq**
  - `.buildCopy(...)` now accepts an **IndexedSeq** instead of just a **Seq**
- `Axis.apply(Double)` now returns a **Vector1D**
  - And so does `.apply(Double)` in **Direction2D**
- Abstract **Polygonic**`.corners` is no longer of type **Vector** but of type **IndexedSeq**
- There were multiple breaking changes to **Parallelogramic**:
  - Renamed abstract `.topLeft` to `.topLeftCorner`
    - Similarly, renamed `.topRight`, `.bottomRight` and `.bottomLeft` which now appear via **Bounded** and have a 
      **different meaning**
  - Renamed `.topEdge`, `.leftEdge`, etc. to `.topSide`, `.leftSide`, etc. matching naming in **Polygonic**
  - Renamed abstract `.top` to `.topEdge`
  - Replaced abstract `.left` with `.rightEdge`
    - The left edge is now considered to be the edge from the **bottom**-left corner **to** the **top**-left corner, 
      not the other way around, as it was previously
- Similarly, there were some breaking changes to **Rectangular**
  - Renamed abstract `.leftLength` to `.rightEdgeLength`
  - `.width` and `.height` now refer to the width and height of the rectangle's **bounding box**, not the rectangle itself
- Changed **Rectangle** model conversion logic
### Deprecations
- Deprecated **Axis**`.toUnitVector` and `.toUnitVector3D` in favor of `.unit: Vector1D` (notice the different return type)
- in **Dimensional**:
  - Deprecated `.indexForAxis(Axis)` and `.axisForIndex(Int)` in favor of `.index` in **Axis** and `Axis.apply(Int)`
  - Deprecated `.compareEqualityWith(Dimensional)(...)` in favor of `.testEqualityWith(Dimensional)(...)`
- in **Size**:
  - Deprecated `.withLength(Double, Axis2D)` in favor of `.withLength(Vector1D, Boolean)` and `.withDimension(Vector1D)`
    - Notice also the changed functionality!
  - Deprecated `.fittedInto(Size, Boolean)` in favor of `.fittingWithin(Size, Boolean)` and `.croppedToFit(Size)`
    - Notice also the changed functionality!
  - Deprecated `.fitsInto(Size)` in favor of `.fitsWithin(Vector2DLike)`
- Deprecated `.minDimension` and `.maxDimension` in **Rectangular** in favor of `.minEdgeLength` and `.maxEdgeLength`
- In **Bounds**:
  - Deprecated `.within(Bounds)` in favor of `.intersectionWith(Bounds)`
  - Deprecated `.fittedInto(Bounds)` in favor of `.fittedWithin(...)` and `.positionedWithin(...)`
  - Deprecated `.shrinked(Size)` in favor of `.shrunk(Dimensional)`
  - Deprecated `.translatedBy(Double, Double)` in favor of `.translatedBy(Dimensional)`
### New Features
- Added **Vector1D** class
- Added **Sized** and **SizedLike** -traits that provides utility functions for all shapes that have a size 
  (and, in the case of **SizedLike**, may be copied)
- Added **BoundedLike** -trait for items that have bounds and may be copied
  - **Bounded** also contains a number of new utility functions
### New Methods
- **Axis**
  - instances
    - Added `.index`
  - object
    - Added `.apply(Int)` that returns an axis matching an index
- **Dimensional**
  - Added `.zipDimensionsWith(Dimensional)`, `.zipDimensionsIteratorWith(Dimensional)`, 
    `.forAllDimensionsWith(Dimensional)` and `.existsDimensionWith(Dimensional)`
  - Added `.compareDimensions(Dimensional)(...)` that returns an **UncertainBoolean**
- **Direction2D**
  - Added `.toUnitVector: Vector1D`
- **Polygonic**
  - Added `.maxEdgeLength` and `.minEdgeLength`
- **Size**
  - Added `.fillToSquare`
### Other Changes
- **Size** now extends **SizedLike**, which introduces a number of new methods
- Similarly, **Bounds** now extends **BoundedLike**
- **Line** now extends **Bounded**
- **Axis** now extends **RichComparable**

## v1.0 - 18.08.2022
Initial version. Took a number of classes from **Genesis**.
