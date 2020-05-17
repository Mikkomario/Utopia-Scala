# Utopia Vault - List of Changes

## v1.6 (Beta)
### Breaking Changes
- Renamed multiple factory traits:
    - StorableFactory -> FromRowModelFactory
    - StorableFactoryWithValidation -> FromValidatedRowModelFactory
    - LinkedStorableFactory -> LinkedFactory
    - MultiLinkedStorableFactory -> MultiLinkedFactory
    - RowFactoryWithTimestamps -> FromRowFactoryWithTimestamps
- Added a new abstract property joinType to FromResultFactory. This will have to be defined in sub-classes.
    - FromRowModelFactory, LinkedFactory, PossiblyLinkedFactory and PossiblyMultiLinkedFactory 
    already specify this property. 
- Storable.toUpdateStatement and .updateWhere now take an optional SqlTarget as the first parameter. 
This allows you to use these update methods in updates that require joins.
- RowFactory.foreach renamed to foreachWhere
### New Features
- Added PossiblyLinkedFactory and PossiblyMultiLinkedFactory to support situations where there is a 
possibility of 0 linked items.
- Added RowModelAccess, SingleRowModel access and ManyRowModelAccess for FromRowFactory -utilizing 
access classes
### New Methods
- Condition object now contains .alwaysTrue and .alwaysFalse properties
### Other Changes
- Changed mergeCondition(...) methods in Accessor from protected to public

## v1.5
### Major Changes
- Module is now based on Scala **v2.13.1** and no longer Scala 2.12.18
### Fixes
- UniqueAccess and ManyAccess implicit auto-access was not marked as implicit previously - fixed
### Deprecations
- **UniqueAccess.get** was replaced with **UniqueAccess.pull**
### New Feature Prototypes
- **ClearOldData** -class added for time and condition based deletion of table data 
### New Methods
- References
    - tablesAffectedBy(Table)
    - referenceTree(Table)
- UniqueAccess
    - pull
- Storable
    - updateWhere(...)
### Small Changes
- Exceptions thrown when trying to access a non-existing property or column in a table now have more informative 
messages