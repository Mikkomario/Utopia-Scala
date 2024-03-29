# Utopia Terra
Terra is a Utopia module designed to help you work with GPS and location data within various world models.

## Parent modules
The following Utopia modules must appear on your class path in order to use Utopia Terra:
- Flow
- Paradigm

## Main features
Models for representing locations under different world views
- Angular (latitude & longitude), as well as vector-based representations
- Built-in support for fully spherical globes and the "Circle of Earth" -model based around the 
  [Azimuthal Equidistant Projection Map](https://pro.arcgis.com/en/pro-app/latest/help/mapping/properties/azimuthal-equidistant.htm)

Distance calculation implementations for different worldviews and models
- Including haversine distance calculation, as well as linear vector-based calculations

Vector projection capabilities for converting world locations to 2D map locations
- You simply need to specify 3-4 reference coordinates

## Main classes
Here are the main classes to start with:
- Angular location representation
  - **LatLong**
  - **CompassRotation** & **LatLongRotation**
- Spherical Earth -worldview
  - **SpherePoint** & **SphereSurfacePoint**
  - **SphericalEarth**
- Circle of Earth -worldview
  - **CirclePoint** (3D) & **CircleSurfacePoint** (2D)
  - **CircleOfEarth**
- 2D map projection
  - **MapPoint**
  - **PointMap2D** & **PointMap3D**