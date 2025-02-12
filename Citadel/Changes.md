# Utopia Disciple - List of changes

## v2.1.9 - 23.01.2025
A new build compatible with the latest **Flow** and **Vault**

## v2.1.8 - 04.10.2024
A small update accommodating changes in Vault.
### Other changes
- Built with Scala v2.13.14
- Supports Vault v1.20
- Refactored **MemberRoleWithRightsFactory**. It is possible that this refactoring resolves and/or causes some errors. 
  This update has not been tested.

## v2.1.7 - 28.07.2024
Conforms to changes introduced in **Flow v2.4** and **Vault v1.19**
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v2.1.6 - 22.01.2024
Scala version update & cleanup.
### Breaking changes
- Removed all deprecated classes and functions
### Other changes
- Scala version updated to 2.13.12

## v2.1.5 - 27.09.2023
Supports latest **Vault v1.17**

## v2.1.4 - 01.05.2023
This update supports **Vault v1.16**

## v2.1.3 - 2.2.2023
This update supports the changes introduced in **Flow** v2.0.

## v2.1.2 - 02.10.2022
New build compatible with **Flow** v1.17

## v2.1.1 - 18.08.2022
New build - Supports changes in **Flow** v1.16

## v2.1 - 06.06.2022
All client device -related features were deprecated (to be removed) in this release, 
making this a relatively major update. 
There were also some smaller user-related changes.
### Breaking Changes
- `DbUser.insert(...)` now accepts different parameters and returns a **DetailedUser**, not **UserWithLinks**
- In some functions, `creatorId` parameter is now optional where it was required previously
- Renamed **StandardUserRole** to **CitadelUserRole**
### Deprecations
- Deprecated all client device -related classes and functions

## v2.0.1 - 27.01.2022
Important bugfix and some minor changes
### Scala
This module now uses Scala v2.13.7
### Deprecations
- Deprecated `CitadelDataDeletionRules.unreferencedDeletionTables` in favor of `.unreferencedRules`
### New Methods
- Added new membership access methods
### Bugfixes
- **DbDescriptionRoles** previously caused a StackOverFlowException when used - now fixed
### Other Changes
- Description properties with _null_ value are omitted when generating simply described models

## v2.0 - 04.11.2021
This update involves a complete overhaul of the database interfaces and especially the description-related interfaces. 
All database interaction objects were rewritten using the **Vault-Coder**.  

This module is now much more flexible. However, migrating will require a number of refactorings.
### Breaking Changes
- Database interaction classes have been completely rewritten with the **Vault-Coder**
  - This involves numerous breaking changes, such as:
    - Creation time and deprecation recording was moved from the description link tables to the description table
      - Description link tables now only require two columns / properties, the two references that form the link
    - **DescriptionLink** model now only contains the link portion. New **LinkedDescription** class contains the 
      link + description (i.e. the same data as the previous **DescriptionLink** implementation)
      - All previous description interactions should be checked and references to deprecated classes and objects 
        should be removed. These interfaces might, and likely won't, function correctly after this update.
      - This also affects deletion rules around descriptions. Only the Description class is now subject to deletions, 
        as the links should cascade with it.
### Deprecations
- Some previous model and database interaction classes have been deprecated. 
  They will be removed entirely in a future release.
  - See **Metropolis** changes for more details
- Tables introduced in this project should now be referenced from **CitadelTables** instead of **Tables**
  - The previous properties remain in the Tables object, but are deprecated
### New Features
- **DescriptionRoles** can now be cached so that accessing them doesn't require constant database queries
  - The cache duration can be specified when setting up **CitadelContext**
- By importing **utopia.citadel.util.MetropolisAccessExtensions**, stored model versions gain various 
  xAccess -properties which refer directly to the matching access points in the database
  - e.g. **InvitationResponse** contains `.invitationAccess`, property which refers to 
    `DbSingleInvitation(invitationId)`
  - Each stored instance also contains the `.access` property that references their own database accessor
    - e.g. a **User**'s `.access` property refers to `DbSingleUser(id)`
### New Methods
- **SingleIdDescribedAccess**
  - Added a number of utility methods for accessing (partially) described instance copies
### Bugfixes
- **DbLanguage**/single`.isoCode` returned a **Value** previously. Now returns a string option.
### Other Changes
- Added some utility description access methods

## v1.3 - 18.10.2021
This relatively big update heavily updates description-related features, making description accessing more easy and 
readable. This update comes with some refactoring requirements, however, especially with the updates on certain 
access class packages, which were due sooner or later anyway.
### Breaking Changes
- Moved **DbUser**, **DbUsers**, **DbDevice** and **DbDevices** to new sub-packages (user & device)
- Refactored descriptions accessing a lot, so there may be multiple errors related to that when you apply this update
- Renamed `.myOrganizations(...)` to `.myOrganizationsIfModifiedSince(...) `in **DbUser**/memberships
  - `.myOrganizations` is now a computed property that accepts an implicit **LanguageIds** parameter
### Deprecations
- Deprecated `DbOrganizations.insert(...)` and `DbDevices.insert(...)` and copied those methods to 
  **DbOrganization** and **DbDevice**
- Deprecated `DescriptionModel.descriptionRoleIdAttName` and the associated column property in favor of shorter 
  `.roleIdAttName` and `.roleIdColumn` properties
- Deprecated **StandardDescriptionRoleId** in favor of **CitadelDescriptionRole**
### New Features
- Added **SingleIdDescribedAccess** trait for more convenient accessing of described model variants and 
  descriptions belonging to an individual model
- Added **ManyDescribedAccess** and **ManyDescribedAccessByIds** traits for convenient accessing of described model 
  variants
- Added **DbManyUserSettings**
### New Methods
- **DbLanguageId**
  - Added `.getOrInsert()` under `.forCode(String)`
- **DbUser** (single)
  - Added `.languageIdsList`
- Added a number of (utility) methods related to description database interactions

## v1.2 - 3.10.2021
This update reflects changes in **Flow** and **Vault**. Also, module logic was altered concerning use of 
email address vs. username.
### Breaking Changes
- User email address is now considered optional in various database interactions
  - Because of this change, username is required to be unique under some conditions / specifications
- `.validateProposedProficiencies(...)` in **DbLanguage** now returns **Pair**s instead of tuples
  - This should require only minor refactoring
### Deprecations
- **TimeDeprecatable**, **Expiring**, **NullDeprecatable** and **DeprecatableAfter** were copied to **Vault** 
  and deprecated here

## v1.1 - 4.9.2021
This update introduces a few small utility additions. The "breaking" changes to **DbOrganization** are 
quite unlikely to affect you.
### Breaking Changes
- Changed sub-class and sub-object names in **DbOrganization**
### New Features
- Added **DbTask** access point for accessing / viewing individual tasks' data
### New Methods
- **DbUser**
  - Added `.email` and `.name` accessors to `.settings` -access point
### Other Changes
- **DbSingleOrganization** now contains `val organizationId`

## v1.0 - 13.7.2021
Please refer to the README for more details.  
Most of the features of this release have been separated from **Utopia Exodus** v1.0.
