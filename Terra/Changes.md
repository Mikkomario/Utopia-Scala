# Utopia Terra - list of changes

## v1.2.1 - 23.01.2025
This update adds functions for hidden height (i.e. physical horizon) calculations, (assuming a perfectly spherical Earth).
### New features
- Added `.calculateHiddenHeight(...)` function to **GlobeMath**, 
  which calculates how much of a viewed object should be hidden behind a physical horizon.
### Other changes
- Added `.toString` implementation to **AerialPoint**

## v1.2 - 04.10.2024
Made it easier to support more generic world views by introducing a couple new traits.
### Breaking changes
- **WorldView** now accepts 5 type parameters instead of 4
  - This addition was made in order to support covariance in lat-long to vector conversion and contravariance in 
    conversions from 2D vectors.
### New features
- Added two new traits for making more general world view support easier:
  - **SurfaceVectorConversions**, which combines conversions between (2D) vectors and latitude-longitude coordinates, 
    as well as distance conversions
  - **FlatWorldView**, which removes 3 of the 5 type parameters, assuming a flat earth representation
### Other changes
- Built with Scala v2.13.14

## v1.1.1 - 28.07.2024
A minor update adding support for **Flow v2.4**.
### Bugfixes
- 0.0 North is now considered equal to 0.0 South (and same with East & West)
### Other changes
- **GridArea**`.origin` is now public
- In some instances where **Vector** was used, **Seq** is now used

## v1.1 - 22.01.2024
As expected, the first post-release update contains major refactoring, as well as some bugfixes. 
Expect heavy refactoring if you were using v1.0 before. 
Other than that, you might enjoy the new interface, as well as the new **GridArea** **WorldView**, which 
is suitable for small-scale GPS-tracking (e.g. tracking positions around a city or something like that).
### Breaking changes
- Greatly refactored most of the model classes
  - Renamed **CirclePoint** to **AerialCirclePoint**
  - Removed **SphereSurfacePoint** class altogether
  - **CompassRotation** classes now extend the new **DirectionalRotationLike** trait instead of wrapping a 
    **Rotation** instance
  - Rotations are now constructed using new factory classes, 
    and **RotationDirection** is now only applicable for east-to-west rotation.
  - Both **LatLong** and **LatLongRotation** now extend **Dimensional[Rotation]** and share a common parent trait
  - **Distance** and **Double** (vector distance) are now largely replaced with the new **WorldDistance** class
  - Travel distance calculation is now performed in **Travel** classes, not directly in the **Ops** classes
- Changed `CircleOfEarth.unitDistance` in order to match the Azimuthal Equidistant projection 
- **WorldView** now extends two new traits: **VectorFromLatLongFactory** and **LatLongFromVectorFactory**
### Bugfixes
- Bugfixes to **CircleOfEarth** vector and latitude-longitude conversions
### New features
- Added a grid-based world view: **GridArea**
### Other changes
- Added `.toString` implementations to **LatLongRotation** and **CompassRotation**
- Scala version updated to 2.13.12

## v1.0 - 27.09.2023
Initial release
