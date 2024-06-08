# Utopia Nexus - List of Changes

## v1.9.3 (in development)
Supports **Flow v2.4**
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v1.9.2 - 22.01.2024
Supports **Flow v2.3**

## v1.9.1 - 27.09.2023
Rebuild due to parent module changes.
### Breaking changes
- Removed deprecated functions
### Other changes
- Scala version updated to 2.13.12

## v1.9 - 02.02.2023
This update mostly reflects changes made in **Flow** v2.0, including similar naming and type updates.  
In addition to this, more convenience is added with the importing of **PostContext** and with the addition of 
**ItemsByIdResource**, two very practical features.
### Breaking Changes
- The message / description property / parameter in **ResourceSearchResult.Error** and **Result** is now of 
  type **String** instead of **Option**. An empty string, not **None**, represents an empty message / description.
- Renamed the xml and json -based **ResultParser** implementations from ...XML and ...JSON to ...Xml and ...Json
### New Features
- Added **PostContext**, an abstract **Context** class that provides utility functions for post body processing
  - These functions were separated and moved to **Nexus** from the **Exodus** module
- Added **ItemsByIdResource** trait, which simplifies resource by id -accessing

## v1.8.1 - 02.10.2022
This update is mostly there to support changes introduced in **Flow** v1.17.
### New Methods
- **Request**
  - Added `.pathString`

## v1.8 - 18.08.2022
This update reflects changes in **Flow** v1.16, namely utilizing the new logging system.
### Breaking Changes
- **RequestHandler** now requires an implicit logger parameter within its constructor
### Other Changes
- **RequestHandler** now logs encountered errors (in addition to returning 505, like before)

## v1.7 - 06.06.2022
This update mostly concerns modular rest resources, refactoring the associated base traits. 
In addition, this update also includes some smaller quality-of-life improvements.
### Breaking Changes
- Modular resources were updated so that use case implementations no longer specify methods. Instead, use cases within 
  the modular resources are now stored in Maps.
### New Features
- Added **NotImplementedResource** trait (from **Exodus**)
### Other Changes
- Added default value `Value.empty` to `Result.Success(...)` constructor

## v1.6.2 - 27.01.2022
This update introduces an important bugfix concerning modular resources
### Scala
This module now uses Scala v2.13.7
### New Methods
- **ExtendableResource**
  - Added `.addChild(=> Resource)`, which is a utility variation of `.extendWith(FollowImplementation)`
### Bugfixes
- **Important**: There was a logic error in **ModularResource** which applied the first implementation 
  regardless of its method. Current version properly filters by method used.

## v1.6.1 - 04.11.2021
Supports changes in Flow v1.14

## v1.6 - 13.7.2021
This is a major upgrade on the **Nexus** module. Most importantly, the **RequestHandler** now supports 
versioning. Secondly, this update adds support for extendable resources, which is utilized in the 
**Utopia Exodus** project, for example.
### Breaking Changes
- **RequestHandler** was rewritten to support versioning
- **ResourceSearchResult.Ready** now takes the ready resource as the first parameter
  - Also, all **ResourceSearchResult** types now use type parameter C for context.
### New Features
- **RequestHandler** now supports versioning (using different resources on different API versions)
- Added **ModularResource** trait and abstract **ExtendableResource** class that support 
  custom implementations with **UseCaseImplementation** and **FollowImplementation**
  - Also added **ExtendableResourceFactory** class that allows extensions on resource classes that take parameters
- Added new **Result** type: **Redirect**
### Other Changes
- **Request** now contains public constructor parameter `.created` 
  which holds the creation time of that **Request**
- **RequestHandler**'s type parameter **C** is now contravariant (**-C**)

## v1.5.1 - 17.4.2021
This small update adds utility traits to make Rest resource implementation easier.
### New Features
- Added **ResourceWithChildren** and **LeafResource** traits to make Rest resource 
  implementation easier

## v1.5
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Renamed NoOperation result to NotModified
### Deprecations
- StreamedBody.bufferedJSON, .bufferedJSONModel and .bufferedJSONArray were deprecated in favor of new 
.bufferedJson, .bufferedJsonObject and .bufferedJsonArray. The new implementations take an implicit 
JsonParser, so they are no longer locked to JSONReader only.
