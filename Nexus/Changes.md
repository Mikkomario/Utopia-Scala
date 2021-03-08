# Utopia Nexus - List of Changes
## v1.5.1 (beta)
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