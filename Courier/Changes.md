# Utopia Courier - List of Changes

## v1.1.1 (in development)
### Bugfixes
- Email reading would previously fail (at least in some situations) because of a message-indexing failure

## v1.1 - 22.01.2024
A major update for the email-reading side. 
This update introduces more advanced folder-targeting and email deletion support. 
Some new fields and data types are also added.
### Breaking changes
- Targeted folder or folders are now specified using a **TargetFolders** instance 
  instead of simply using a folder name, as before
- Removed "all reading succeeded" -condition from deletion rules
- Iterative email reader functions don't have their iterator terminate on failures anymore
- Removed max read count -parameter from iterative mail read methods
- Replaced **EmailHeadersLike** with **EmailHeaders**
- Added new (required) email headers: **Message-ID**, **In-Reply-To** and **References**
- Email addresses are now represented using class **EmailAddress** instead of a **String**
  - Implicit conversions from **String** to **EmailAddress** are available to make the migration easier
- Added pointer-based email deletion option to **EmailReader**
  - This feature breaks use-cases where `new EmailReader(...)` is called directly.
### New features
- It is now possible to target multiple folders when reading email
### New Methods
- **EmailBuilder**
  - Added `.mapResult(...)`
### Other Changes
- UTF-8 encoded email addresses are now automatically decoded
- Attachment names are now normalized before saving them on the disk
- EmailBuilder now uses ISO-8859 as backup decoding
- Added implicit conversion from a **Vector** of **Strings** to **Recipients**
- Scala version updated to 2.13.12

## v1.0.7 - 01.05.2023
Rebuild due to the changes in other modules.

## v1.0.6 - 02.02.2023
This update supports the changes introduced in **Flow** v2.0.

## v1.0.5 - 02.10.2022
Supports changes in **Flow** v1.17

## v1.0.4 - 18.08.2022
New Build for **Flow** v1.16

## v1.0.3 - 06.06.2022
This release introduces two very important bugfixes to email reading.
### Bugfixes
- Email iteration was not progressing but instead continuously handled the same message over and over again
- Attachments were not always recognized because of a case-related problem (fixed)

## v1.0.2 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v1.0.1 - 04.11.2021
New build / supports changes in **Flow** v1.14

## v1.0 - 3.10.2021
See README.md for list of features
