# Utopia Trove - List of Changes

## v1.1.1 - 27.01.2022
Important bugfixes and additions
### Scala
This module now uses Scala v2.13.7
### Bugfixes
- Previous implementation attempted to restore backed up database versions before a versions table even existed
### Other Changes
- `LocalDatabase.setup(...)` and `.setupWithListener(...)(...)` now accept optional `defaultCharset` and 
  `defaultCollate` -parameters
  - This affects database creation, as well as the settings used in database connections

## v1.1 - 04.11.2021
This update includes an important bugfix concerning the initial database setup. 
If you're using **Trove**, I highly recommend you to get this update (even though it does involve a breaking change).
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
