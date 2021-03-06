# Utopia Exodus - List of Changes

## v2.0 - 13.7.2021
This release separates most of the database interaction into the new **Utopia Citadel** module and also makes 
many of the existing REST resources customizable from the sub-projects.  
Another major improvement is the addition of simple model styling with the **X-Style** header 
(or **style** query parameter) support. This makes request responses potentially much more readable.  
There are also some pretty important bug-fixes included.
### Breaking Changes
- Copied most of the database classes to **Utopia Citadel** module while kept some 
  authentication-related features in this module. This may cause problems in the 
  dependent projects. Most of these problems can be resolved by switching to the appropriate 
  **Citadel** classes, however.
- **AuthorizedContext** is now a trait and not a class. Also, removed the `errorHandler` -parameter from the 
  object constructor (apply); The class now uses `ExodusContext.handleError(...)` instead.
- Moved most of the rest node classes from rest.resource.user to rest.resource.user.me
- **PublicDescriptionsNode** trait now requires some support for **SimplyDescribed** trait
- Renamed nested classes and objects in **DbSingleUser**
- users/me/invitations now returns status code 200 and an empty array when there are no invitations 
  (Previously returned 204 No Content)
### Deprecations
- All classes and features which were copied to **Citadel** are now deprecated in **Exodus**
- Deprecated `ExodusResources.all` in favor of `.default`
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
- Added **ExodusDataDeletionRules** object to simplify deprecated / expired data deletion
- Sessions and some requests now support preferred model style parameter
  - Specify *X-Style* -header or *style* query parameter with *full* or *simple* either during login or 
    on specific requests.
- The following resources now support *simple* model style:
  - DescriptionRoles, Languages and LanguageFamiliarities
  - Tasks and UserRoles
  - MyLanguages, MyOrganizations, MyInvitations and MySettings
  - Organization Descriptions and Organization Members
  - Device
- **MyOrganizationsNode** now checks for the *If-Modified-Since* -header and also respects the 
  *Accept-Language* header / user account preference when fetching organization descriptions.
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