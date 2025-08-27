# Utopia Scribe Api - list of changes

## v1.1 (in development)
Adds support for **Scribe-Core v1.2 and Vault v1.22**
### Breaking changes
- Renamed **DetailedIssue** to **IssueWithDetailedVariants**
### Deprecations
- Deprecated all previous database classes (i.e. database model classes, database factory classes and the access classes)
  - These are replaced with newly generated versions
### New features
- Added `queue` command to the console app
- Added database interfaces for managing issues

## v1.0.5 - 26.05.2025
Adds support for **Access v1.6**

## v1.0.4 - 23.01.2025
This update merely adds support for the latest **Flow** version.

## v1.0.3 - 04.10.2024
A minor update supporting changes in Vault.
### Other changes
- Built with Scala v2.13.14
- Supports Vault v1.20

## v1.0.2 - 28.07.2024
This update adds support **Vault v1.19**, as well as a few minor changes.
### New features
- The list command in the console app now shows top error message, also
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v1.0.1 - 22.01.2024
Supports **Flow v2.3** and **Vault v1.18**
### Other changes
- Scala version updated to 2.13.12

## v1.0 - 27.09.2023
Initial release
