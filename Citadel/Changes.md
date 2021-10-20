# Utopia Disciple - List of changes

## v1.3.1 (in development)
### New Methods
- **SingleIdDescribedAccess**
  - Added a number of utility methods for accessing (partially) described instance copies
### Bugfixes
- **DbLanguage**/single`.isoCode` returned a **Value** previously. Now returns a string option.

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
