# Utopia Vault - List of Changes
## v1.5
### Major Changes
- Module is now based on Scala **v2.13.1** and no longer Scala 2.12.18
### Fixes
- UniqueAccess and ManyAccess implicit auto-access was not marked as implicit previously - fixed
### Deprecations
- **UniqueAccess.get** was replaced with **UniqueAccess.pull**
### New Methods
- References
    - tablesAffectedBy(Table)
    - referenceTree(Table)
- UniqueAccess
    - pull
### Small Changes
- Exceptions thrown when trying to access a non-existing property or column in a table now have more informative 
messages