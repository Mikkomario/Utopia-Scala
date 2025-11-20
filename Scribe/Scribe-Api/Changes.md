# Utopia Scribe Api - list of changes

## v1.1.1 (in development)
Supports **Flow v2.8** and **Vault v2.1**

## v1.1 - 01.11.2025
Following **Vault v2.0** update, this version introduces new targeting-based database interaction classes. 
The outward-facing **Scribe** interface remains identical, however.

Besides refactoring, the console application received a large number of updates. 
Do note that these new features require a database update in order to function.
### Breaking changes
- Renamed **DetailedIssue** to **IssueWithDetailedVariants**
### Deprecations
- Deprecated all previous database classes (i.e. database model classes, database factory classes and the access classes)
  - These are replaced with newly generated versions
### New features
- Added the following new console commands:
  - `comments`: Read comments on the open issue
  - `comment`: Write a comment on the open issue
  - `fixed`: Mark the issue as fixed
  - `silence`: Silence the issue for some time
  - `follow`: Requests a notification when the issue appears the next time
  - `alias`: Given an issue an alias
  - `severity`: Change issue severity
  - `queue`: Queue issues for `see next`
  - `close`: Mark notifications as read
- Added database interfaces for managing issues
### Other changes
- Rewrote the `status` command

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
