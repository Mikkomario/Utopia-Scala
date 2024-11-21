# Utopia Paradigm - List of Changes

## v1.7.1 (in development)
Latest Flow support
### Bugfixes
- Attempting to calculate a value for a **Change** with duration of zero would previously cause a 
  **StackOverFlowException** in situations where the **Change**'s `.toString` involved such value calculations.
  - For example, such a case would be with **LinearVelocity**

## v1.7 - 04.10.2024
This update focuses on the **Polygonic** trait (now named **Polygon**), refactoring many of the existing functions. 
Specifically, the convex parts -algorithm was completely redesigned and rewritten.

Besides these, added better support for angular calculations by introducing the **AngleRange** class. 
### Breaking changes
- `.cutBetween(...)` in **Polygonic** now returns a **Pair** instead of a **Tuple**
- **Polygon**`.angles` now functions differently (more according to the function's name)
### Bugfixes
- Bugfix to def `VelocityLike.apply(Duration, LinearAcceleration, Boolean)`, so that with zero or near-zero values, 
  it now returns zero instead of NaN values (when preserveDirection is set to true).
- Redesigned and rewrote `.convexParts` in **Polygonic**, as the previous version had some issues
- **Polygon**`.center` now properly calculates the polygon centroid (previous version used average)
- Similarly, **Polygonic**`.circleWithin` now actually places the circle completely within the polygon 
- Bugfix to **Parallelogramic**`.area`, which previously only calculated correct values in case of rectangles 
### Deprecations
- Deprecated `.mapCorners(...)` in **Triangle** in favor of the new `.map(...)`
- Deprecated **Polygonic**, which is now **Polygon**
- Deprecated **Parallelogramic**, which is now **Parallelogram**
### New features
- Added **AngleRange** class
### New methods
- **Angle**
  - Added `.opposite`
- **Animation** (object)
  - Added a couple of utility constructors
- **Line**
  - Added `.intersectsWith(Line)`
- **Matrix2D** (object)
  - Added `.quarterRotationTowards(RotationDirection)`
- **Polygon**
  - Added `.sidesIterator` and `.edgesIterator`
  - Added `.toTriangles`
  - Added `.filledToConvex`
  - Added `.innerAngles` and `.outerAngles`
  - Added `.map(...)`
  - Added `+(HasDoubleDimension)` for translation
- **TimedAnimation** (object)
  - Added `.fixed(...)` constructor
### Other changes
- Built with Scala v2.13.14
- **Polygon**, **Triangle** and **Parallelogram** are now traits, replacing their previous trait counterparts
- **BoundsFactoryLike**`.between(...)` now accepts **HasDoubleDimensions** instead of just **P**
- **AnyAnimation** is now covariant
- **Dimensions** and **DimensionsBuilder** now use **OptimizedIndexedSeq** instead of **Vector**
- Vectors now throw if `.withLength(...)` is called for a zero vector
- **Change** now throws if its duration is zero and amounts are being calculated
- Changed equality logic in Dimensions, Vector2D and Vector3D
  - Now, for example, `Vector3D(1)` equals `Vector3D(1, 0, 0)`
- Added some optimizations to more specific polygon classes
- Optimized / refactored various functions in **Polygonic**

## v1.6 - 28.07.2024
This is a smaller update, focusing on:
- Enclosing **Circle** algorithms
- **Bounds** and related classes: Utility updates
- **RelativePoint**: Utility updates & improvements
- Transformation classes: Minor optimization

This update also includes some relatively important bugfixes, so be sure to pick it up.

### Breaking changes
- **LinearTransformable** now requires implementation of `.identity`. Similarly, 
  **AffineTransformable** requires the implementation of `.affineIdentity`.
### Bugfixes
- Fixed **DirectionalRotationLike** `+(C)` implementation, which would previously combine opposite rotations in cases 
  where the first rotation was negative
- Fixed (removed) **Parallelogramic**`.bounds` implementation
- Fixed some issues with **Polygonic**`.convexParts`
- Fixed a bug where `Color.average(Iterable)` would fail when input was of type **Set**
### New features
- Added **ProjectionPath** trait extended by **Line**
  - This trait facilitates acquiring projected / matching points on a path 
- Added **HasMutableSize** trait
### New methods
- **Angle** (object)
  - Added `.random`
- **Bounded**
  - Added `.withCenter`
- **Bounds** (object)
  - Added `.aroundPoints(IterableOnce[HasDoubleDimensions])`
- **Circle** (object)
  - Added `.enclosing(Seq)` and `.enclosingCircles(Iterable)` 
    which create a circle that encloses a number of points or circles
- **HasBounds**
  - Added `.center`
- **RelativePoint**
  - Object
    - Added `.origin`
  - Class
    - Added `.onlyAbsolute`
    - Added `.apply(OriginType)`
- **Size** (object)
  - Added `.fullHd`
### Other changes
- In some instances where **Vector** was used, **Seq** is now used
- Removed certain unnecessary transformations by adding a check for identity transforms in 
  **LinearTransformable** and **AffineTransformable**.
- Slightly refactored **RelativePoint** mapping logic
- **Bounds**`.relativize(...)` now accepts any kind of **DoubleVector** instead of just a **Point**
- **Bounds** now extends **ApproxSelfEquals**
- Added `.toString` to **RelativePoint**

## v1.5 - 22.01.2024
This update contains some major refactoring in the following classes / concepts:
- **Rotation**, which was separated into directional and non-directional rotation classes
- **Insets**, which were partially rewritten and split into multiple traits
- **Distance**, which received some utility updates, as well as testing

Other updates are mostly new utility functions to existing classes.
### Breaking changes
- Practically rewrote the **Rotation** class
  - The previous **Rotation** implementation is now named **DirectionalRotation**
  - The new **Rotation** class is unidirectional (i.e. simply a wrapper for a positive or negative radians count) 
    - It does not necessarily specify direction. The interpreted direction is context-specific.
- The default positive direction in **Rotation**`.arcLengthOver(...)` is now **Clockwise** instead of 
  the direction of the **Rotation** instance.
  - In order to match previous functionality, you may have to use `.absoluteArcLengthOver(Double)`
- Renamed **LineLike**`.map(...)` to `.mapEnds(...)` because of a name clash / compiler problems in **Line**
- Rewrote parts of **InsetsLike**, replacing it with a number of new **SidesLike** classes
- Renamed **InsetsFactory**`.apply(Map)` to **InsetsFactory**`.withSides(Map)` because of name conflicts
  - Also, **InsetsFactory** now extends **DimensionalFactory**, which might cause some name clashes
- Removed classes and functions that were deprecated before v1.2
### Deprecations
- Deprecated most of the existing **Rotation** (/**DirectionalRotation**) functions in favor of the new syntax
- Deprecated **InsetsFactoryLike** in favor of the new **SidesFactoryLike**
- Deprecated **DistanceUnit**`.conversionModifierFor(DistanceUnit)` in favor of a more clearly named 
  `.conversionModifierFrom(DistanceUnit)`
### Bugfixes
- Fixed certain `+` functions in **Insets**
### New features
- Added more extensive metric unit support (see **MetricScale**, **MetricUnit** & **MeterUnit**), 
  including some new **DistanceUnits** and
- Added **SidesBuilder** class (accessible via **SidesFactoryLike**`.newBuilder`)
### New methods
- **Alignment**
  - Added `.apply(Direction2D)` and `.movesTowards(Direction2D)`
  - Added `.surroundWith(Size)`, which may be used for constructing insets
- **Bounded**
  - Added new method variants: `.enlarged(Double)` and `.shrunk(Double)`
- **Bounds**
  - Added `.apply(Direction2D)`
  - Added `.relativize(...)` and `.relativeToAbsolute(...)` methods that perform **Bounds**-based coordinate conversions
- **Color**
  - Class
    - Added `.visible` and `.invisible`
    - Added `.highlightedBy(Double)`
  - Object
    - Added `.weighedAverage(Iterable)`
- **ColorSet**
  - Added a new constructor variant: `.apply(Color, Double)`
- **DimensionalFactory**
  - Added a new `.twice(D)` constructor
- **Distance** (object)
  - Added `.ofKilometers(Double)`
- **NumericVectorLike**
  - Added `.mapLength(...)`
- **Rgb** (object)
  - Added `.average(Iterable)` and `.weighedAverage(Iterable)`
- **Sized**
  - Added `.roundSize`
### Other changes
- **Direction2D** values lists are now **Pairs** instead of **Vectors**. 
  - Similarly, **Axis2D**`.directions` returns a **Pair** instead of a **Vector** now.
- **Polygonic**`.toShape` now applies rounding
- **Distance** now extends **MayBeAboutZero**
- Improved **Distance** `.toString`
- Refactored `Color.average(Iterable)`
- Scala version updated to 2.13.12

## v1.4 - 27.09.2023
This version applies major under-the-hood refactoring to vector classes, 
in preparation of upcoming rounding shape classes (which are partially implemented here). 
The most noticeable difference is the updated package structure.  
There are also some utility improvements related to the **Rotation** class.
### Breaking Changes
- Refactored **shape** package structure
- Renamed **VectorFactory** to **DoubleVectorFactory**
- **VectorProjectable**`.projectedOver(...)` now accepts **DoubleVector** instead of **DoubleVectorLike**
- Renamed **Projectable** to **LineProjectable**, and changed the `.projectedOver(...)` 
  parameter type from **Vector2D** to **DoubleVector**.
- **LinearAlignment**`.direction` now returns **SignOrZero** instead of **Option[Sign]**
- **LinearTransformable** now longer requires the implementation of `.self`
- Renamed **VectorProjectable2** to **VectorProjectable** (leftover from an earlier update)
### Bugfixes
- Bugfixes to distance conversions when converting from a **Meter** to a smaller unit
### Deprecations
- Deprecated `.toRotation` in **Angle**
  - Please use `.toShortestRotation` or `.toClockwiseRotation` instead
### New Features
- Added **Adjustment** and **SizeAdjustable** for more convenient size change functions
- Added **FromDirectionFactory** and **FromAlignmentFactory** -traits
- Added certain rounding shapes: **RoundingDouble**, **RoundingVector**, **RoundingVector1D**, 
  **RoundingSpan** and **RoundingVector2D**
  - The rounding shape support is not yet fully implemented, however
- Added more generic numeric vector traits
  - **NumericVectorLike** trait, based on the **DoubleVectorLike** -trait
  - **NumericVectorFactory**, based on **VectorFactory**
### New Methods
- **Angle**
  - Added `.toShortestRotation` and `.toClockwiseRotation` (which matches previous `.toRotation`)
- **Dimensions**
  - Added `.mapWithZero(...)(...)`
- **Distance** (object)
  - Added `.zero`
- **Insets**
  - Added `.round`
- **InsetsLike**
  - Added `-(Axis2D)`
- **Rotation**
  - Added `.degreesTowards(RotationDirection)` and `.radiansTowards(RotationDirection)`
  - Added couple of methods related to arc lengths
### Other Changes
- Added new generic traits for vector classes and other related classes
- **Distance** now extends **SignedOrZero**
- Value conversion from **Angle** to **Rotation** now uses `.toShortestRotation` instead of `.toRotation`. 
  As a consequence, the resulting rotation amounts may be smaller.
- Rewrote `Color.fromInt(Int)` implementation
- Added new `-` override for **AccelerationLike**

## v1.3 - 01.05.2023
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
