# Utopia Terra - list of changes

## v1.1.1 (in development)
### Bugfixes
- 0.0 North is now considered equal to 0.0 South (and same with East & West)
### Other changes
- **GridArea**`.origin` is now public

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
