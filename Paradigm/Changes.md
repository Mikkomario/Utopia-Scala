# Utopia Paradigm - List of Changes

## v1.1 (in development)
### Breaking Changes
- **Dimensional** -trait changed in ways that require changes from the implementing classes:
  - `.zeroDimension` is now **public** instead of protected
  - `.dimensions` is now required to return an **IndexedSeq**
  - `.buildCopy(...)` now accepts an **IndexedSeq** instead of just a **Seq**
- `Axis.apply(Double)` now returns a **Vector1D**
  - And so does `.apply(Double)` in **Direction2D**
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
### New Features
- Added **Vector1D** class
- Added **Sized** -trait that provides utility functions for all shapes that have a size and may be copied
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
- **Size**
  - Added `.fillToSquare`
### Other Changes
- **Size** now extends **Sized**, which introduces a number of new methods
- **Axis** now extends **RichComparable**

## v1.0 - 18.08.2022
Initial version. Took a number of classes from **Genesis**.
