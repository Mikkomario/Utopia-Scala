# Utopia Annex - List of Changes

## v1.2 (in development)
New Build / Supports changes in **Flow** v1.16
### Breaking Changes
- **ContainerUpdateLoop**`.merge(...)` is now required to return a tuple instead of just one value. 
  The new, second value indicates the custom wait time until next request.
- The following classes require an implicit **Logger** parameter:
  - **ContainerUpdateLoop**
  - **PersistingRequestQueue**

## v1.1 - 06.06.2022
This update reflects changes made in **Flow** update v1.15.
### Breaking Changes
- **ContainerUpdateLoop** now extends **LoopingProcess** instead of **Loop**
  - This means that `.startAsync()` is no longer available (replaced with `.runAsync()`)

## v1.0.4 - 27.01.2022
Dependency changes, according to Disciple update v1.5
### Scala
This module now uses Scala v2.13.7

## v1.0.3 - 04.11.2021
Supports changes in Flow v1.14

## v1.0.2 - 4.9.2021
This update simply supports changes in **Utopia Disciple** v1.4.2 update that added its own 
**RequestFailedException** class.
### Deprecations
- Deprecated **RequestFailedException** in favor of another exception with the same name in **Utopia Disciple**

## v1.0.1 - 13.7.2021
No changes in features, simply added support for the latest (breaking) changes in the 
**Access** (v1.4) and **Flow** (v1.10) modules.

## v1.0 - 17.4.2021
Initial release with the main features (see README)
