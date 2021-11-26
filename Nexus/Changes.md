# Utopia Nexus - List of Changes

## v1.6.2 (in development)
### New Methods
- **ExtendableResource**
  - Added `.addChild(=> Resource)`, which is a utility variation of `.extendWith(FollowImplementation)`

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
