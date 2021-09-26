# Utopia Vault Coder

## v1.1 (in development)
(working on enum support & abstract functions)
### Breaking Changes
- Renamed this module to **Utopia Vault Coder** - Also renamed code occurrences and removed **Metropolis** dependencies
  - Because of this, the **Tables** object implementation is left partially open.
### New Features
- Added support for enumerations (see README for more details)
- Added support for deprecation (deprecation data type)
- Class name can be parsed from associated table name (if specified)
- Property name can now be guessed based on its data type (E.g. creation time properties are named "created" by default)
- Property documentation can be generated automatically for some data types
### Other Changes
- Supports a wider range of key names in the input json document (e.g. "props" in addition to "properties")
- Supports latest **Flow** and **Vault** changes
- Supports abstract functions internally

## v1.0 - 4.9.2021
Initial release. See Readme.md for mode details.