# Utopia Vault - List of Changes

## v1.19 (in development)
Supports **Flow v2.4**
### Breaking changes
- **LatestModelAccess** now extends **FilterableView**
- `.earliest` in **SingleChronoRowModelAccess** now returns an access point, not the pulled item
### Deprecations
- Deprecated **FromRowFactoryWithTimestamps** in favor of the new version: **FromTimelineRowFactory**
### New features
- Added **VaultContext** and **VaultContextWrapper** traits in order to facilitate Vault-based library creation
- Added **FromIdFactory** and **HasId** traits
- Added **StorableFactory** trait which uses the new **DbPropertyDeclaration** class and 
  facilitates access to database-properties
### New methods
- **ManyModelAccess**
  - Added `.findColumnMap(...)` and `.findColumnMultiMap(...)`
### Other changes
- **View**`.exists(...)` now supports joins
- Joins are now omitted in instances where target already contains the joined table

## v1.18 - 22.01.2024
This update focuses on improving grouped (i.e. multi-joined) result parsing.
### Breaking changes
- Renamed **View**`.globalCondition` to `.accessCondition`
- Removed all classes and functions that were deprecated at v1.14 or earlier
### Deprecations
- Deprecated the `.grouped(...)` functions in **Result** in favor of the new, more clear functions
- Deprecated **MultiLinkedFactory** and **PossiblyMultiLinkedFactory** traits since the 
  new **Result** functions offer similar functionality
### New methods
- **DataInserter**
  - Added `.insertFrom(...)`
- **FromRowFactory**
  - Added `.tryParse(Row)` that functions like `.parseIfPresent(Row)`, but assumes that data is found
- **Result**
  - Added multiple advanced row-processing functions:
    - `parseAnd(...)`
    - `combine(...)`
    - `group(...)` and `groupAnd(...)`
    - `deepGroup(...)` and `deepGroupAnd(...)`
### Other changes
- Scala version updated to 2.13.12

## v1.17 - 27.09.2023
This update introduces important bugfixes, as well as better support for DELETE statements.
### Breaking Changes
- `DatabaseTableReader.apply(...)` now accepts a different function for column- to property name -mapping. 
  This is because some mapping logic implementations will need to scan the whole table at once.
  - Also updated the default mapping logic to look for common prefixes between the column names. 
    These prefixes are not included in the resulting property names.
### Deprecations
- Renamed `.putAttribute(...)` to `.putProperty(...)` in **DistinctModelAccess**
### Bugfixes
- Bugfix to conditional join syntax
- Fixed a bug in **ClearOldData** reference processing logic
- Generated alter table -statements now properly wrap the table name in backquotes
### New Features
- Added **DatabaseActionQueue** class
- Added specific table-targeting and joining support to `.delete()` and its variants in **View** 
### New Methods
- **DistinctModelAccess**
  - Added `.clearColumn(...)` and `.clearProperty(...)`
- **View**
  - Added `.deleteNotLinkedTo(...)` and `.forNotLinkedTo(...)` (protected)

## v1.16 - 01.05.2023
While this update focuses on adding new utility functions, 
please note that you will need to perform some refactoring in order to migrate.
### Breaking Changes
- **FilterableView** now requires the implementation of `.self`
- Removed implicit conversions from access points to **Vector** and **Option**
  - While this change may require you to fix a couple of build errors, 
    it will also help you avoid accidental database queries
### Deprecations
- Deprecated **SqlExtensions**
### New Features
- Added **TimeDeprecatableView** and **NullDeprecatableView** in order to make accessing deprecatable items easier
### New Methods
- **ChronoRowFactoryView**
  - Added `.takeLatest(Int)`, `.takeEarliest(Int)` and `.createdDuring(HasInclusiveEnds[Instant])`
- **ConditionElement**
  - Added `.time`
  - Added `.appearsWithin(String)`
- **ManyModelAccess**
  - Added `.toMapBy(...)`, `.pullColumnMap(...)` and `.pullColumnMultiMap(...)`
- **OrderBy**
  - Added `.withDirection(OrderDirection)`, `.ascending` and `.descending`
- **RowFactoryView**
  - Added `.take(Int, OrderBy)`
- **SqlSegment**
  - Added `.mapSql(...)` and `.mergeWith(SqlSegment)(...)`
### Other Changes
- **Value** to **ConditionElement** conversion is now implicitly available, you don't need to 
  import `SqlExtensions._` anymore
- **Storable**`.insert()` now throws if index is not specified in a context where one is required. 
  Previously the insert was simply skipped.

## v1.15 - 02.02.2023
This update focuses mostly on bugfixes and better joining support.  
One neat update is the addition of combinable column length rules.
### Breaking Changes
- The `order: Option[OrderBy] = None` -parameter in **Access**`.findNotLinkedTo(...)` 
  is now the 3rd and not the 2nd parameter.
  - This is unlikely to cause major problems, but should be reviewed
### BugFixes
- **FromRowFactory**, when used with a table that doesn't have a primary column, 
  will no longer reduce the result into a single row.
### New Features
- Added support for conditional joins. See **Join**`.where(Condition)`.
### New Methods
- **Access**
  - Added `.findNotLinkedTo(Table)`
- **Reference**
  - Added `.reverse`
  - Added functions for converting references to joins
- **References**
  - Added functions that produce reference-based graphs
### Other Changes
- When loading column length rules from json, multiple rules may now be combined by separating them with "` or `" 
  (without quotation marks). This means that the second rule will kick in if the application of the first rule fails. 
    - For example, `"expand to 255 or crop"` will primarily attempt to make the column larger, but at will rather crop 
    input that's longer than 255 characters.
- **References** object now returns **CachingSeq** instead of **Set** in many functions
- `.segment` in **Condition** is now public instead of private

## v1.14 - 02.10.2022
This update adds a number of functions to access points that utilize ordering, i.e. different variants of 
min and max -accessing.
### Breaking Changes
- **SingleChronoRowModelAccess**`.latest` now returns an access-point, not the read item
### Deprecations
- In **SingleAccess**
  - Deprecated the `.first(...)` -variants in favor of `.firstUsing(...)` and `.findFirstUsing(...)`
  - Deprecated the `.top(...)` -variants in favor of `.topBy(...)`
  - Deprecated some `.maxBy(...)` and `.minBy(...)` variants
### New Features
- Added new **Log ErrorHandlingPrinciple**
- Added a constructor to **LatestModelAccess**
- **SingleAccess** max & min -functions now support joining
### New Methods
- **SingleModelAccess**
  - Added multiple new methods for reading max/min column values
### Other Changes
- **ConnectionPool** now terminates all unused connections when the connection closing thread is interrupted 

## v1.13 - 18.08.2022
This update reflects changes in **Flow** v1.16
### Breaking Changes
- `ClearOldData.daily(...)` and `ClearUnreferencedData.loopDailyFrom(...)` now require an implicit **Logger** parameter
### New Methods
- **ColumnLengthRule**
  - Added `.recoverWith(ColumnLengthRule)`
### Bugfixes
- Expanded `ColumnLengthRules.loadFrom(...)` logic to support current (v1.5) **Vault Coder** syntax

## v1.12.1 - 06.06.2022
This update contains major bugfixes that fix problems introduced in v1.12 update 
and also some older bugs appearing in edge-cases.

In addition, new utility features were added. Most importantly column maximum length customization support, 
better support for timestamped item access and support for joins when pulling column data.
### Deprecations
- Deprecated **NullDeprecatable**`.idColumn` in favor of `.index`
### New Features
- Added **ChronoRowFactoryView** and **SingleChronoRowModelAccess** traits which wrap a **FromRowFactoryWithTimestamp** 
  factory and provide utility functions accordingly
- You may now customize column maximum length handling logic via `ColumnLengthRules.loadFrom(...)`
  - (Added a new variation of the `.loadFrom(...)` method)
### Bugfixes
- Fixed an issue where column length limits would throw every time an empty value was being inserted
- Fixed an issue where `globalCondition` was not applied to `readColumn(...)` in **ModelAccess**
### Other Changes
- You can now apply joins to `.pullColumn(Column)` in **DistinctReadModelAccess**
- **NullDeprecatable** now extends **Indexed**
- Updated how database selection is managed in **Connection**

## v1.12 - 27.01.2022
This update contains a major refactoring of the factory and access traits, as well as some important fixes. 
New important features include database events (triggers).
### Scala
This module now uses Scala v2.13.7
### Breaking Changes
- **DistinctModelAccess** no longer requires property `defaultOrdering`. 
  - This property is now read from the associated **Factory** (new property).
- **Access**`.read(...)` now accepts two more parameters (`joins` and `joinType`)
  - This also applies to `readColumn` in **ModelAccess**
- Changed constructor parameter ordering in **Column**. Also added new optional parameter **LengthLimit**. 
- Added a new constructor parameter to **SqlSegment**
- SqlTarget trait now requires properties `.databaseName` and `.tables`
- Changed **ClearUnreferencedData** constructor
### Deprecations
- Deprecated multiple **FromResultFactory** and **FromRowFactory** methods in favor of renamed, and sometimes extended, 
  versions.
### New Features
- Added database update events (on data insertions, updates and deletions)
  - See **Triggers** for more information
- Added column length limit management to **Insert** and **Update** (See **ColumnLengthRules**)
- **ClearUnreferencedData** now accepts a set of tables to ignore when checking for references, which is useful for 
  more customized use-cases
### New Methods
- **ConditionElement**
  - Added `.notIn(Iterable)`
### Bugfixes
- `.nonDeprecatedCondition` in **Expiring** was the complete opposite of what it should have been - now fixed
- `USE <databaseName>;` -statement was missing from database connection opening - added
### Other Changes
- `.createDatabase(...)` in **Connection** now accepts optional default character set and default collate -parameters
- ConnectionSettings scaladoc was misleading in terms of proposed character set. 
  Now correctly proposes `utf8` and not `UTF-8`

## v1.11.1 - 04.11.2021
Supports changes in Flow v1.14

## v1.11 - 18.10.2021
This update changes how **SingleIdModelAccess** works, making it a trait and easier to extend, although unfortunately 
breaking the existing sub-classes in the process. Besides this update, there are some internal logic optimizations to 
**Table** and **FromRowFactory** classes.
### Breaking Changes
- **SingleIdModelAccess** is now a trait and not a class
  - This will break the existing `extends` -statements and require a new idValue -property from the extending classes
### New Features
- Added **SingleIntIdModelAccess** trait, which is a version of **SingleIdModelAccess** that is less generic and 
  easier to implement (if you use integers as row ids)
### New Methods
- **NullDeprecatable**
  - Added `.deprecatedCondition`
### Other Changes
- **FromRowFactory** now filters out duplicate rows (based on row primary keys) before parsing them
- **Table** now uses a Map internally for matching property names to columns. The matching is also now case-insensitive.

## v1.10 - 3.10.2021
This update contains some package-related refactoring, as well as refactoring based on the latest **Flow** changes. 
Unfortunately this means that you will most likely also have to do some refactoring on your end. Besides this, 
new deprecation support was added based on traits that previously existed on the **Utopia Citadel** module. These 
are now more widely available.
### Breaking Changes
- Moved the **DataInserter** trait from `utopia.vault.model.template` to `utopia.vault.nosql.storable`
- **References**`.columnsBetween(Table, Table)` and `.connectionBetween(Table, Table)` now return 
  **Pair**s instead of tuples.
  - Also includes additional method variants
  - This change is quite unlikely to require major refactoring
### New Features
- Added **TimeDeprecatable**, **NullDeprecatable**, **Expiring** and **DeprecatableAfter** from the **Citadel** module
  - These allow for easier deprecation implementation in storable factories
### New Methods
- **Table**
  - `.validate(Model)` - shorter version for writing `.requirementsDeclaration.validate(...).toTry`
### Other Changes
- **Reference** now uses **Pair** internally instead of two separate **ReferencePoint**s. 
  The interface remains largely the same, however.

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
