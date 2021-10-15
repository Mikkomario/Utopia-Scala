# Utopia Metropolis - List of Changes

## v1.3 (in development)
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