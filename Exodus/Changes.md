# Utopia Exodus - List of Changes

## v2.0 (in development)
### Breaking Changes
- Renamed nested classes and objects in **DbSingleUser**
- Most of the database classes were copied to **Utopia Citadel** module while some 
  authentication-related features were kept in this module. This may cause problems in the 
  dependent projects. Most of these problems can be resolved by switching to the appropriate 
  **Citadel** class, however.
### Deprecations
- Deprecated `ExodusResources.all` in favor of `.default`
- All classes and features which were copied to **Citadel** are now deprecated in **Exodus**
### New Features
- **MeNode** and **MySettingsNode** now extend the new **ExtendableResource** class 
  and can therefore be extended from outside this project
- **OrganizationNode**.type now extends the new **ExtendableResourceFactory** class and can therefore 
  be extended from outside this project
- Added **SessionUseCaseImplementation** object that makes it easier to create session authorized 
  use case implementations for modular resources
- Added **ExtendableSessionResource** class that makes it easier to create extendable 
  session authorized **Resource** *objects*
- Added **OrganizationUseCaseImplementation** object that makes it easier to create organization-specific 
  session authorized use case implementations for modular resources
- Added **ExtendableOrganizationResourceFactory** class that makes it easier to create extendable 
  organization-specific session authorized resource factory objects
### Bugfixes
- Separated `ExodusResources.public` to `.publicDescriptions` and `.customAuthorized`. The previous implementation 
  was a programming / refactoring mistake and returned a **Vector** of **Object**s, not **Resource**s.
  - Also, **QuestSessionsNode** is now properly returned in the `.authorized` resource set
### New Methods
- **ExodusResources**
  - `.apply(Boolean)` which creates a set of resources using a single parameter
### Other Changes
- `ExodusContext.setup(...)` now calls `Status.setup()`

## v1.0 - 17.4.2021
Initial release. See README for list of main features.