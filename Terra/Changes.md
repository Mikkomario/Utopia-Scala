# Utopia Terra - list of changes

## v1.1 (in development)
TODO: Document Rotation changes, WorldPointOps changes etc.
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

## v1.0 - 27.09.2023
Initial release
