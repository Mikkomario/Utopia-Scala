# Utopia Metropolis - List of Changes

## v1.1 (in development)
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