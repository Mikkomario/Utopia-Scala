# Utopia Logos - List of Changes

## v0.5 (in development)
### Breaking changes
- **UniqueTextPlacementAccessLike** now extends **UniqueModelAccess**, which changes the abstract property from 
  `accessCondition: Option[Condition]` to `condition: Condition`
### New methods
- **PlacedAccessLike**
  - Added `.starting`

## v0.4 - 23.01.2025
This update changes how words are stored in the database, making this a rather breaking update when compared to v0.3. 
Besides changing this storage logic, this update adds some previously missing interfaces / features.
### Breaking changes
- Words are now stored in a standardized format. 
  **WordPlacement**'s `displayStyle` is then used to properly stylize these words in each use-case.
  - **DbStatement**'s `.store(...)` now accepts instances of **PreparedWordOrLinkPlacement** instead of 
    **(String, Boolean)**
### Deprecations
- Deprecated `DbWord.matching(String)`, as that did not account for different capitalization entries
- Deprecated **Statement**`.wordsAndLinks`
### New features
- Added **DetailedStatement** class
- Added **CombinedStatement** trait
- Added emoji filtering to statement storing logic
- Added 2 new display styles: **Capitalized** and **AllCaps**
### New methods
- **DisplayStyle**
  - Added `.addIdentifyLogic(...)` for automated display style identification for custom display styles
  - Added `.of(String)`
- **Statement**
  - Added `.standardizedWords` and `.links`
- **WordOrLink**
  - Added `.style` and `.standardizedText`
### Other changes
- **StatedWord** now extends **Placed**
- **PlacedStatement** now extends **CombinedStatement**
- Updated delimiters to be more inclusive 

## v0.3 - 04.10.2024
A major refactoring update with the aim of standardizing the model and package structure. 
This update focuses especially on the generic statement-linking traits.
### Breaking changes
- Renamed packages `word` to `text`
- Renamed **Statement** (stored instance) to **StoredStatement** and **StatementText** to **Statement**
- Renamed **Word** (stored instance) to **StoredWord**
- Renamed **Link** (stored instance) to **StoredLink**
- Renamed **WordOrLinkText** to **WordOrLink**
- Renamed all database models to -DbModel
- Multiple changes in access packages:
  - Moved `url.link_placement` to `url.link.placement`
  - Moved `url.reques_path` to `url.path`
- Renamed **Link**'s `requestPathId` to `pathId`
- Renamed some access / filter functions
- There may be other changes caused by Vault Coder generation changes as well
### Deprecations
- Replaced **TextStatementLink** and related classes with **TextPlacement** + related classes
- Replaced **LinkedStatement** and related classes with **PlacedStatement** and related classes
- Deprecated **StatementLinkDbConfig** in favor of **TextPlacementDbProps**
### New features
- Added factory wrapper traits
- Added **TextPlacement** traits for classes placing something at specific places in some texts
### Other changes
- Built with Scala v2.13.14
- Supports Vault v1.20

## v0.2 - 28.07.2024
The first beta release. Expect breaking changes in future updates.
