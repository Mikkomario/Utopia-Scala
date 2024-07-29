# Utopia Trove - List of Changes

## v1.1.9 - 28.07.2024
This minor update reflects changes in **Flow v2.4**.
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v1.1.8 - 22.01.2024
Cleaned up the code a little.
### Breaking changes
- Removed classes that were deprecated at version v1.1 or earlier
### Other changes
- Scala version updated to 2.13.12

## v1.1.7 - 27.09.2023
Rebuild due to parent module changes

## v1.1.6 - 01.05.2023
Rebuild due to the changes in other modules.

## v1.1.5 - 02.02.2023
This update supports the changes introduced in **Flow** v2.0.

## v1.1.4 - 02.10.2022
New build compatible with **Flow** v1.17

## v1.1.3
### Other Changes
- Updated **DatabaseVersion**`.toString` implementation

## v1.1.2 - 06.06.2022
This small update contains important bugfixes for use-cases that weren't previously encountered.
### Bugfixes
- Used database name is now specified in more places to avoid related errors
### Other Changes
- When reading database structure & update documents, the default type is now `Update` in cases where 
  `Origin` or `From` has been defined.

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
