# Utopia Exodus - List of Changes

## v4.1.6 (in development)
Conforms to changes introduced in **Flow v2.4** and **Vault v1.19**
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v4.1.5 - 22.01.2024
Supports **Flow v2.3** and **Vault v1.18**
### Breaking changes
- Deleted all classes and functions that were deprecated at v4.0 or earlier
### Other changes
- Scala version updated to 2.13.12

## v4.1.4 - 27.09.2023
Rebuild due to parent module changes.

## v4.1.3 - 01.05.2023
This update supports **Vault v1.16**

## v4.1.2 - 2.2.2023
This version supports the latest changes in **Flow** v2.0 and **Nexus** v1.9.

## v4.1.1 - 02.10.2022
Supports changes in **Flow** v1.17
### New Methods
- **AuthorizedContext**
  - Added `.handleInterceptedPost(...)` and `.handleInterceptedValuePost(...)`
### Other Changes
- Removed the device-related REST resources that were deprecated in version v4.0 

## v4.1 - 18.08.2022
This update reflects changes in **Flow** v1.16, adding new logging
### Breaking Changes
- Changed `ExodusContext.setup(...)` -parameters to accept an implicit **Logger** instead of a function
### Deprecations
- Deprecated `ExodusContext.handleError(Throwable, String)` in favor of `ExodusContext.logger`

## v4.0 - 06.06.2022
This update represents a major overhaul to the **Exodus** library. The authorization system is completely rewritten. 
Also, all device-related features are removed.
### Breaking Changes
- Overhauled the authorization system:
  - Replaced **SessionToken**, **DeviceToken**, **ApiKey**, **EmailValidatedSession** and **EmailValidationAttempt** 
    (partially) with **Token**
  - New token system supports authentication scopes
    - This change applies to all existing rest nodes - they now make sure the request is authorized in the 
      applicable scope
      - Cases where email validation was used as an authentication method are now rewritten using access scopes. 
        This change affects request body parsing, also.
  - The difference between "public" and session-authenticated resources was removed. All resources are now authorized 
    using access tokens. Only the required scopes differ.
    - This includes all rest resources within the `description` package
  - The **AuthorizedContext** interface is also different now
- Rewrote the **EmailValidator** trait and how email validations are handled
  - Removed email validation resend feature altogether
- Removed **ClientDevice** dependencies from all classes, functions and rest nodes 
  - Deprecated the remaining device-related rest nodes 
- Rewrote user creation (`POST users`)
  - Response content is now different and supports styling
  - Authentication is based on scopes (only)
- Removed invitations rest node hierarchy from **ExodusResources**
- Removed user creation from `POST invitations/open/responses` (use `POST users` instead)
- **ExodusContext** now requires a new parameter in `.setup(...)`, which lists the scopes granted to all users by 
  default
  - This is applied during login, user creation and refresh token acquisition
- **ExodusResources** now only contains one valid property: `.all`
- Removed `.emailAuthorized(...)` from **AuthorizedContext**
- Replaced **PublicDescriptionsNode** with new **GeneralDataNode**
- Renamed **StandardTask** to **ExodusTask**
### Deprecations
- Deprecated all nodes under `invitations`
  - These are replaced with `users/me/invitations`
- Deprecated all rest nodes under `devices`
- Deprecated all classes which are replaced with new versions (**ApiKey**, **DeviceKey**, **UserSession**, ...)
- Deprecated following **AuthorizedContext** methods:
  - `apiTokenAuthorized(...)`
  - `sessionTokenAuthorized(...)`
  - `deviceTokenAuthorized(...)`
  - `basicOrDeviceTokenAuthorized(...)`
### New Features
- Added session management through `users/me/sessions`
- Added authentication scope system
- Added `POST users/me/invitations/responses`, which answers all pending invitations at once 
  (based on previous `POST invitations/open/responses`)
### Other Changes
- Access tokens are now hashed in the database (using SHA256), so that they can no longer be read and used
- `GET users/me/invitations` now lists invitations even when the requesting user doesn't have an account yet, 
  provided that the request is made using an email-validated token

## v3.1 - 27.01.2022
An update that introduces multiple fixes and functional changes to the user management interface, 
but unfortunately also a number of breaking changes.
### Scala
This module now uses Scala v2.13.7
### Breaking Changes
- Replaced path `quests/me/session-key` with `quests/me/session-token`
- `GET organizations/<organizationId>/users` now omits the requesting user
- Added email validation to organization invitations
  - This includes a new email validation purpose
  - Also added invitation responding using an email validation token (see Postman documentation for details)
- Email validations may now be switched to temporary email session tokens which function just like the original 
  email validation tokens.
  - This allows the client to validate (and possibly extend) the authentication before 
    requesting user for additional data.
  - This is a breaking change because it requires SQL changes
- **AuthorizedContext** is now an abstract class instead of a trait, which may require changes in its subclasses
  - This is to avoid reading request body twice (see **Functional Changes**)
- Added `defaultModelStyle: ModelStyle` as the fourth parameter to `ExodusContext.setup(...)`. 
  This has a chance to cause build errors initially, but is not difficult or cumbersome to fix.
### Functional Changes
- Organization member roles may now be modified by a same level user, provided the targeted user isn't 
  an organization owner. The same applies to removing organization members.
  - The reasoning behind this is that the situation may always be rectified by a higher level organization member, 
    in case the action was performed accidentally or with wrong intents.
  - Also, the user level is not checked by user role but by user access rights
- **AuthorizedContext** now caches request body value after it has been parsed. This enables subsequent / multiple 
  calls of handlePossibleValuePost / handleValuePost etc.
### New Features
- Default model style is now specified in `ExodusContext.setup(...)`. The value is **Full** by default, 
  attempting to match the previous versions.
  - This means that deviceless sessions no longer use **Simple** model style by default, but it also means that 
    all sessions may now receive default style of **Simple** if it is specified in `ExodusContext.setup(...)`
- An organization member is now allowed to yield some of their roles or to replace them with lower roles
  - An exception to this is the situation where an organization owner would yield their ownership without leaving 
    another owner behind.
- **OrganizationInvitationsNode** now supports extensions
- **AuthorizedContext** now supports **X-Accept-Language-Ids** -header as a replacement for **Accept-Language**
### Bugfixes
- `users/me/languages` **POST** and **PUT** request handling fixed
  - Previous versions would generate duplicates because of invalid id matching
- `users/me/settings` **PUT** request handling fixed
  - Previous versions would remove email address if it wasn't updated
- Unique username requirement is now enforced in user creation when so specified in **ExodusContext**

## v3.0 - 04.11.2021
This update **Exodus**' models and database interfaces were completely rewritten by utilizing the **Vault-Coder**. 
This will most surely require changes in the dependent modules and applications as well.
### Breaking Changes
- There are some changes in various REST nodes concerning some basic features like user- or organization creation
  - It is also noteworthy that, since description model structure changed, 
    all responses containing descriptions were also affected
- All models and database interaction interfaces were rewritten using the **Vault-Coder**
  - This involves changes in property naming and property ordering, as well as some functional changes
- Wherever session-, device- or email validation keys were referenced, they are now called **tokens**
  - This includes REST interface routing (e.g. `device-token` instead of `device-key`)
- Packaging was updated so that all models and database interaction interfaces are stored in sub-packages
  - I.e. **DeviceKey** (now **ClientDeviceToken**), **EmailValidation** (now **EmailValidationAttempt**) and 
    **UserSession** (now **SessionToken**) were moved to sub-package auth
- Renamed a number of models and database interaction interfaces
  - **DeviceKey** is now **ClientDeviceToken**
  - **EmailValidation** is now **EmailValidationAttempt**
  - **UserAuth** is now **UserPassword**
  - **UserSession** is now **SessionToken**
- Removed some previously deprecated models and database interaction classes
### Deprecations
- Previous model versions that were renamed in the new build are now deprecated
  - I highly encourage you to update the references when migrating to this version. These model versions are 
    unlikely to work with the new database structure, and they will be removed in a future release.

## v2.2 - 18.10.2021
This update mostly reflects changes in **Citadel** and **Vault** modules, but includes one bugfix besides those 
which may be of importance to you.
### Breaking Changes
- Refactored **PublicDescriptionsNode** to support **Citadel** v1.3 changes - This affects all subclasses
### Bugfixes
- **PublicDescriptionsNode** was not performing authorization before
### Other Changes
- Supports changes in **Citadel** v1.3 and **Vault** v1.11
- **AuthorizedContext**`.languageIdListFor(=> Int)` now returns **LanguageIds** instead of a Vector
- Deleted some deprecated access objects

## v2.1 - 3.10.2021
This update reflects changes in the **Citadel** module, where email address was made optional. This feature / setting 
is specified with `ExodusContext.setup(...)`.
### Breaking Changes
- `ExodusContext.setup(...)` now accepts a boolean property `requireUserEmail`, which specifies whether 
  email address is considered a required field in **UserSettings** and related functions.
  - Unlike the earlier versions where email address was always required, the default value of this property is false
    - If you want to keep the email requirement, you will have to update the `ExodusContext.setup()` -call.
  - When user email address is considered optional, usernames are required to be unique
- Some deprecated user-related classes were removed
### New Methods
- **ExodusContext**
  - `.userEmailIsRequired` and `.uniqueUserNamesAreRequired` which are mutually exclusive (see breaking change above)

## v2.0.1 - 4.9.2021
This update focuses on extending the custom extension capabilities of the base **Exodus** implementation.
### New Features
- **TasksNode** now provides access to individual **TaskNode**s which support custom extensions
- Added **ExtendableSessionResourceFactory** class for easier session-based extensions
### New Methods
- **AuthorizedContext**
  - Added `.handlePossibleValuePost(...)` that allows users to recover from cases where no request body was specified
- **SessionUseCaseImplementation**
  - Added `.factory(...)` -method to support session-based extendable resource factories

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
