# Utopia Metropolis - List of Changes

## v2.2.4 (in development)
Built with Scala v2.13.14

## v2.2.3 - 28.07.2024
Supports **Flow v2.4**
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v2.2.2 - 22.01.2024
Supports **Flow v2.3**
### Breaking changes
- Removed all deprecated classes and functions
### Other changes
- Scala version updated to 2.13.12

## v2.2.1 - 27.09.2023
Added from model -conversions to a couple of classes

## v2.2 - 01.05.2023
This minor update reflects changes in earlier **Flow** updates.
### Breaking Changes
- **Described**`.apply(...)` -variations now return **String** instead of an **Option**

## v2.1.3 - 02.02.2023
This update supports the changes introduced in **Flow** v2.0.

## v2.1.2 - 02.10.2022
Supports changes in **Flow** v1.17

## v2.1.1 - 18.08.2022
New build - Supports changes in **Flow** v1.16

## v2.1 - 06.06.2022
This major update reflects changes that were made in **Exodus** update v4.0. Namely, all device-related functions 
were deprecated for removal and some authorization-related features were altered. In addition, many user-related 
models changed.
### Breaking Changes
- **NewUser** no longer accepts device -related parameters
- Removed email validation support from **UserSettingsUpdate**
- Removed email validation support from **PasswordChange**
  - Also, renamed `old_password` to `current_password` (`old_password` is still supported when parsing model data)
### Deprecations
- Deprecated all client device -related models
- Deprecated **UserWithLinks** in favor of **DetailedUser**
- Deprecated **UserCreationResult**, as it is no longer valid since **Exodus** v4.0
- Deprecated `NewInvitation.validated`, since data validity is now checked when parsing model data
### New Features
- Added **MetropolisRegex** object, which contains a regular expression for validating email addresses
- Added **IdWrapper** trait, as well as **TaskIdWrapper** and **UserRoleIdWrapper** traits
### Other Changes
- Email address formatting is now checked when parsing post models
- Changed `.toSimpleModel` implementation in **FullUserLanguage**
- **NewUser**`.toModel` now contains property `request_refresh_token` instead of `remember_me`. `remember_me` is still 
  supported in **Exodus** v4.0, however.

## v2.0.1 - 27.01.2022
A minor style update, plus a scala version update
### Scala
This module now uses Scala v2.13.7
### Other Changes
- *Null* description properties are omitted in **SimplyDescribed**`.toSimpleModelUsing(...)`

## v2.0 - 04.11.2021
This update completely rewrites the model structure in this project. 
This obviously includes a number of breaking changes.
### Breaking Changes
- All models were rewritten using the **Vault-Coder**, resulting in various changes, including model name changes, 
  property name changes and property ordering changes.
- Updated **Description** and **DescriptionLink** models so that creation and deprecation are handled in the 
  **Description** and not in the **DescriptionLink**.
  - It is also notable that **DescriptionLink** model now only contains the link portion. 
    The previous link + description combination is now accessible as **LinkedDescription**.
- Some model name changes:
  - **Deletion** is now **OrganizationDeletion**
  - **DeletionCancel** is now **OrganizationDeletionCancellation**
  - **RoleRight** is now **UserRoleRight**
  - **UserDevice** is now **ClientDeviceUser**
  - **UserLanguage** is now **UserLanguageLink**
- Some functional changes within models:
  - **User** model no longer includes settings
  - **UserSettingsData** now includes userId -property
- Moved **UserRole** from `user` package to `organization` package.
- Moved **LanguageIds** to package `cached`
- Deleted some previously deprecated enumerations
### Deprecations
- Deprecated some model classes in favor of new implementations
  - I highly recommend you to update your code to use the non-deprecated classes only. 
    The deprecated models will be removed in a future release.
  - This includes the following models:
    - **Deletion** (now **OrganizationDeletion**)
    - **DeletionCancel** (now **OrganizationDeletionCancellation**)
    - **DeletionWithCancellations** (now **OrganizationDeletionWithCancellations**)
    - **RoleRight** (now **UserRoleRight**)
    - **UserDevice** (now **ClientDeviceUser**)
- Deprecated some combined model classes in favor of new implementations:
  - **DeletionWithCancellations** is replaced with **OrganizationDeletionWithCancellations**
  - **DescribedRole** is replaced with **DetailedUserRole**
  - **FullDevice** is replaced with **DetailedClientDevice**
### New Methods
- **DescribedLanguage**
  - Added `.descriptionOrCode(...)`

## v1.3 - 18.10.2021
This update adds a lot of utility for description handling, unfortunately also breaking the 
**DescribedFromModelFactory** implementations, although not badly.
### Breaking Changes
- Switched **DescribedFromModelFactory**'s `A` and `D` type parameters around (from `[D, A]` to `[A, D]`)
### New Features
- Added **DescriptionRoleIdWrapper** trait to support easier description reading
- Added **LanguageIds** model for easier preferred language management
### New Methods
- **Described**
  - Added multiple new utility functions for accessing various descriptions
### Other Changes
- **LanguageFamiliarity** now extends **SelfComparable**
- Added **DescribedFactory** trait (parent trait for **DescribedFromModelFactory**)

## v1.2 - 3.10.2021
This update, which is closely connected to **Citadel** and **Exodus** updates, makes the user's email address 
an optional field. This is to support a wider range of use cases (e.g. when data protection requires one to omit such 
personal information).
### Breaking Changes
- **UserSettings**`.email` is now optional
  - This change is also reflected in **NewUser** and **UserSettingsUpdate**

## v1.1.1 - 4.9.2021
This very small update fixes a problem that occurred in some **SimplyDescribed** implementations.
### Other Changes
- **SimplyDescribed** no longer overwrites existing properties with **empty** (null) description properties.

## v1.1 - 13.7.2021
The most important update in this release is the addition of simple model styling, 
which is utilized in **Utopia Exodus**, making request responses more user-friendly for 
non-**Journey** users.
### Breaking Changes
- Renamed the **Described** trait to **DescribedWrapper**
### New Features
- Added simple model support (**ModelStyle** and **StyledModelConvertible**)
    - Also added simple styling support for described items 
      (using **DescribedSimpleModelConvertible**, **SimplyDescribed** and 
      **SimplyDescribedWrapper** traits)
    - Multiple existing classes now support this new simple model format
### Other Changes
- Added a few models from the **Exodus Module**
- **DeletionCancel** now contains property `.created`

## v1.0 - 17.4.2021
Initial release. See README.md for a list of main features.
