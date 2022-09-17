# Utopia Paradigm - List of Changes

## v1.1 (in development)
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
