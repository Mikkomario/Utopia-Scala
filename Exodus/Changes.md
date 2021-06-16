# Utopia Exodus - List of Changes

## v1.0.1 (in development)
### Deprecations
- Deprecated `ExodusResources.all` in favor of `.default`
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