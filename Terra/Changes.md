# Utopia Terra - list of changes

## v1.1 (in development)
### Breaking changes
- Changed `CircleOfEarth.unitDistance` in order to match the Azimuthal Equidistant projection 
- **WorldView** now extends two new traits: **VectorFromLatLongFactory** and **LatLongFromVectorFactory**
- **CompassDirection**`.degrees(Double)` now returns a **CompassRotation** instance instead of a **Rotation** instance
  - This shouldn't cause too much breaking, since there's an implicit conversion from **CompassRotation** to **Rotation**
### Bugfixes
- Bugfixes to **CircleOfEarth** vector and latitude-longitude conversions
### New features
- Added a grid-based world view: **GridArea**
### Other changes
- Added `.toString` implementations to **LatLongRotation** and **CompassRotation**

## v1.0 - 27.09.2023
Initial release
