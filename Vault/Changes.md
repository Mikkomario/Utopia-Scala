# Utopia Vault - List of Changes

## v1.9 - 4.9.2021
This update mostly contains some non-breaking refactoring and utility updates. However, the bugfix to the `exists` 
function in **FactoryView** / **View** is of major importance. I do recommend applying this update on a high priority.
### Breaking Changes
- **UniqueModelAccess** trait no longer defines the index property. This property had naming conflicts with 
  the **Indexed** trait.
- **View** trait now requires `target: SqlTarget` property. In most cases this shouldn't cause a build 
  error since most of the utilized sub-traits of this trait already required that property.
### New Methods
- **View**
  - `.contains(Column)` and `.containsNull(Column)` for checking if an accessible column is null or not
    - Variation based on these: `.containsAttribute(String)` and `.containsNullAttribute(String) `
### Bugfixes
- `View.exists` now properly takes the `.globalCondition` property into account whereas the previous implementation 
  (in **FactoryView** didn't)
  - **This is a very important bugfix** since the previous bug can cause all kinds of logic errors.
### Other Changes
- Moved some methods, like `.exists`, `.isEmpty` and `.delete()` from sub-traits of **View** to the **View** trait.
- Moved the `.find(Condition, Option[OrderBy])` -method from **SingleAccess** and **ManyAccess** to **Access**.

## v1.8 - 13.7.2021
This update adds some long-delayed refactorings on project package structure and is therefore 
quite error-inducing. However, this adds a lot of traits that reduce the need for copying and pasting and 
simplify your code a lot when utilized. Most important of these updates are the new **LinkedFactory** traits, 
and the new **View** traits.
### Breaking Changes
- Refactored package structure in nosql package (access & factory packages)
- **DistinctModelAccess** now requires computed property: `defaultOrdering: Option[OrderBy]`
- **SingleAccess** and **ManyAccess** no longer extends **FilterableAccess**
- **MultiLinkedFactory** trait now requires implementation of `.isAlwaysLinked: Boolean` 
  instead of `.joinType`
- **MultiLinkedFactory** now expects a **Vector** and not just a **Seq** in `.apply(...)`
- **DeletionRule** no longer uses optional duration as standard live duration and only supports 
  finite durations in conditional live durations
### Deprecations
- Deprecated **Extensions** object in utopia.vault.sql package. 
  Identical **SqlExtensions** object should be used instead.
- Deprecated **FilterableAccess**, **UnconditionalAccess**, **NonDeprecatedAccess** and **RowModelAccess** 
  in favor of new **View**-based traits
### New Features
- Added **ClearUnreferencedData** class for easier deletion of rows that are not referenced by other tables
- Added combining factory traits (based on linked factories) that make linked factory implementation 
  even more streamlined in cases where two factory implementations are combined
- Added **View** traits that can be extended by various **Access** classes or classes that function like 
  **Access** points without providing read access to data. The new **View** classes make it easier to 
  apply additional traits to **Access** classes because they don't take that many type parameters (1 at most)
- Added **ColumnAccess** that works like **IdAccess**, except that it allows one to access different columns
- Added **DistincReadModelAccess** trait that provides access to .pull -methods without requiring 
  .put method support
- Added **LatestModelAccess** trait that works like **UniqueModelAccess**, except that it targets 
  the latest row and doesn't support .put methods
- Added **FilteredAccess** trait to simplify the creation of nested access points
### New Methods
- Linked factory classes got a few new methods since they now extend the new **LinkedFactoryLike** trait
- **ClearOldData**.type
  - Added `.once(...)` and `.dailyLoop(...)` functions to make the use of this class easier
- **FromResultFactory**
  - `.getManyWithJoin(...)`
- **FromRowFactory**
  - `.getWithJoin(...)`
### Other Changes
- Added **SqlExtensions** object, which is a copy of the **Extensions** object and will replace 
  the latter in a future release
- **StoredModelConvertible** now adds the id constant to the beginning of the resulting model
- **DataInserted** trait now extends **Indexed**, providing the `.index` property

## v1.7.1 - 12.5.2021
This update focuses on utility features around SQL queries, especially on iterative, 
sequential queries that target very large data sets. 
This update also contains an important bugfix concerning `Result.updatedRowCount`.
### New Features
- Added **DataInserted** trait to skip the repetitions coding of `.insert(...)` methods
- Added **QueryIterator** class for easy **Limit** + **Offset** queries
  - Added `.iterator(SqlSegment, Int)` and `.rowIterator(SqlSegment, Int)` to **Connection**
  - Added `.iterator` and `.orderedIterator(OrderBy)` to **ManyModelAccess**
  - Added `.iterator` to **ManyIdAccess**
- Inserts can now generate warnings or errors when attempting to insert values that don't belong to a table
  - Specify `ErrorHandling.insertClipPrinciple` in order to utilize this feature
### New Methods
- **Condition**.type
  - `.and(Seq[Condition])` and `.or(Seq[Condition])` that combine multiple conditions together
- **Delete**.type
  - `.apply(Table)` (new variation to support combination with **Limit**)
  - `.inParts(Table, Int, Option[Condition])` for deleting very large amounts of rows
### Fixes
- `.updatedRowCount` in **Result** is now set correctly within `connection.apply(...)` (was broken previously)
- If a database query returns multiple result sets, they are now correctly combined 
  (previously only the first result was read)

## v1.7 - 17.4.2021
This update focuses on the **Access** classes as well as database creation / management.
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