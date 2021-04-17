# Utopia Vault - List of Changes

## v1.7 - 17.4.2021
### Breaking Changes
- **ModelAccess** now requires three type parameters instead of two, to support value accessing
- **ManyIdAccess** no longer provides implicit access to `.all` - this may be re-added later, however
- Changed **IndexedAccess** to **Indexed** since the generic type parameter made it cumbersome to use this trait
### Deprecations
- Deprecated **UniqueAccess**. **UniqueModelAccess** and **UniqueIdAccess** are now favored.
### New Features
- Added easier value access methods to **ModelAccess**
- Added **DistinctModelAccess** and **UniqueModelAccess** utility traits, 
  which allow read and write access to a distinct set of models / values
### New Methods
- **Connection**
  - `.createDatabase(...)`
- **ModelAccess**
  - `.delete()`, which deletes all accessible rows in the primary table
- **SingleIdAccess**
  - `.min` and `.max`
- **SingleModelAccess**
  - `.findColumn(...)` and `.findAttribute(...)`
- **Update**.type
  - `.columns(...)`
  - `.apply(SqlTarget, Column, Value)` which updates the value of an individual column
### Fixes
- Zero row inserts are ignored and now return an empty result without interacting with the database
### Other Changes
- `Connection.dropDatabase(...)` now accepts a parameter `checkIfExists: Boolean` (true by default)
- `.apply(...)` in **Insert**, which accepted multiple models, now accepts **Seq** instead of just **Vector**
- `.in(...)` in **ConditionElement** now transforms the condition into *FALSE* or equals condition when 
  there are 0 or 1 items in the set.
  - However, the method now requires **Iterable** instead of **IterableOnce**

## v1.6
### Scala
- Module is now based on Scala v2.13.3
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
- Moved exists method from UniqueAccess to ModelAccess
- ClearOldData now accepts DataDeletionRules as parameters for more readable and easier configuration
### New Features
- Added PossiblyLinkedFactory and PossiblyMultiLinkedFactory to support situations where there is a 
possibility of 0 linked items.
- Added RowModelAccess, SingleRowModel access and ManyRowModelAccess for FromRowFactory -utilizing 
access classes
- Added .existsDatabaseWithName(String) and .existsTableWithName(String, String) to Connection
- Added database dropping feature to Connection
- Added Count sql statement support
### New Methods
- Condition object now contains .alwaysTrue and .alwaysFalse properties
### Other Changes
- Changed mergeCondition(...) methods in Accessor from protected to public
- Accessor.mergeCondition(...) now accepts a Storable instance that will automatically be converted to a condition
- ConditionElement.in(Iterable) now returns a "FALSE" statement when specified set is empty
- Non-table data may now be read from Rows

## v1.5
### Major Changes
- Module is now based on Scala **v2.13.1** and no longer Scala 2.12.18
### Fixes
- UniqueAccess and ManyAccess implicit auto-access was not marked as implicit previously - fixed
- Bugfixes in ClearOldData
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