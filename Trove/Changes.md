# Utopia Trove - List of Changes

## v1.1 (in development)
### Breaking Changes
- Replaced **VersionNumber** with **Version** from **Flow**
### Bugfixes
- Target database is now dropped before installing a new version even when no version was registered before
  - Previously it was possible for the program to get stuck when only partially applying the first update 
    (due to an SQL error)

## v1.0.1 - 13.7.2021
This update doesn't add anything new to **Trove**, but is simply to support the breaking 
changes introduced by **Utopia Vault** update v1.8.

## v1.0 - 17.4.2021
Initial release. See README.md for a list of main features.