# Utopia Nexus - List of Changes

## v1.6 (in development)
### Breaking Changes
- **RequestHandler** was rewritten to support versioning
- **ResourceSearchResult.Ready** now takes the ready resource as the first parameter
  - Also, all **ResourceSearchResult** types now use type parameter C for context.
### New Features
- **RequestHandler** now supports versioning (using different resources on different API versions)
- Added **ExtendableResource** trait that supports custom implementations with 
  **UseCaseImplementation** and **FollowImplementation**
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