# Utopia Annex - List of Changes

## v1.4 (in development)
Schrödinger update
### New Features
- **NoConnection** and **Response.Failure** now extend a new trait **RequestFailure**. 
  This makes **RequestResult** pattern matching easier, because all failure states may be matched into a single case.
### New Methods
- **ResponseBody**
  - Added `.parseMany(FromModelFactory)`

## v1.3.1 (In Development)
Supports changes in **Flow** v2.0

## v1.3 - 02.10.2022
This update introduces some smaller utility additions which aim to make this module more scalable.  
Most of the changes relate to the **ContainerUpdateLoop** class.
### Breaking Changes
- **Response** (including **Success** and **Failure**) now contains headers
- **ContainerUpdateLoop** now requires  the `.requestTimeContainer` as a **FileContainer** of **Instant** **Options**
  - See new **ValueConvertibleOptionFileContainer** class in **Flow**
### Other Changes
- **ContainerUpdateLoop** now primarily uses a **Date** header from the response as a new request time threshold, and only 
  secondarily uses local time. This is to account for situations where server clock is in different time than client clock.
- **Failure** response message parsing now first seeks for model properties "error", "description" or "message" before 
  including the whole response body as the message

## v1.2 - 18.08.2022
This update adds logging support to automated container updates, reflecting **Flow** v1.16 changes.
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
