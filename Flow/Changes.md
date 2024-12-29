# Utopia Flow - List of Changes

## v2.5.1 (in development)
### Deprecations
- Renamed **DateRange**'s `.period` to `.length`
### New features
- Added **TimeUnit** enumeration, which is an advanced version of Java's enumeration with the same name
- Added **LazilyUpdating** class, which performs lazy incremental updates regularly or when requested to do so
- Added **UntilExternalFailureIterator**
- Added **RepeatOneForeverIterator**
- Added **LazilyInitializedChanging** that provides lazy **Changing** wrapping
### New methods
- **IterableOnce**
  - Added `.splitMapInto(...)`, `.splitFlatMapInto(...)` and `.splitInto(...)`
- **ModelLike**
  - Added `.tryGet(...)`
- **Seq** (**CollectionExtensions**)
  - Added `.findNextIndexWhere(...)` and `.findPreviousIndexWhere(...)`
- **InputStream** (**StreamExtensions**)
  - Added `.buffered` and `.notEmpty`
- **String** (**StringExtensions**)
  - Added `.splitToLinesIterator(Int)`
- **Try** (**TryExtensions**)
  - Added `.mapFailure(...)`
### Other changes
- Internal refactoring in **TimeExtensions**
  - Modified conversions between different duration types
  - Added better handling for Inf & MinusInf duration values
  - Rewrote number to duration conversions
- `.allChildrenIterator` in Path (via **FileExtensions**) now returns the paths from top to bottom
- **Days** now extends **HasSign**
- Minor refactoring in **Duration**`.finite`
- Minor refactoring in `ModelLike.apply(IterableOnce)`

## v2.5 - 04.10.2024
A larger update focusing on pointers. The main changes are:
- Refactored **Flag** class hierarchy, adding non-eventful versions (**Settable** & **Switch**)
- **Volatile** classes are no longer automatically eventful
  - Also refactored the **Volatile** interfaces quite a bit 
- Added more robust logging to **Pointers**
  - This adds required implicit **Logger** access in most pointer constructors
- Refactored **CollectionExtensions** by dividing it into 3 different files
  - Unfortunately this change requires a lot of import changes in the dependent projects

This update also adds some important bugfixes, most notably to XML parsing (see bugfixes section for more details).

As usual, there's also a large number of utility updates and new smaller features and functions.
### Breaking changes
- Moved parts of **CollectionExtensions** to different files:
  - Moved **Try**-related features to **TryExtensions**
  - Moved **Either**-related features to **EitherExtensions**
  - Moved **Range**-related features to **RangeExtensions**
  - This is because the **CollectionExtensions** file was getting bloated, which negatively affected IDE performance
- A large number of pointer-related changes:
  - All non-wrapping pointers now require an implicit logger as a construction parameter
    - Classes implementing **Changing** are now required to implement `listenerLogger: Logger`
    - This requirement was added because previously errors would not always get properly logged or even handled, 
      and could break the change event distribution system
    - This same requirement is extended to instances of **ListenableLazy**, because of their `stateView` property
    - Note: For **Flags**, if you didn't utilize the change events, 
      you may want to use the new **Settable** and **Switch** instead
  - `Pointer.empty`, `EventfulPointer.empty` and `LockablePointer.empty` are now all computed properties 
    instead of methods (i.e. adding empty parentheses at the end is no longer required nor supported)
  - `readOnly: Changing` in **Changing** is now abstract, 
    meaning that if you extend Changing in one of your classes, you may have to specify this value manually
  - Previously the available implicit collection functions for **Pointer** required **Vector** content type.
    The current version requires **Seq** instead of **Vector**. This affects some of the function return types.
- A number of changes to **Volatile** and its subclasses
  - **Volatile** no longer supports change events by default
    - In order to utilize change events, use **EventfulVolatile** (also available as `Volatile.eventful`) instead
  - **Volatile** is now a trait instead of a class
    - If you have extensions of this class, you may need to implement some required properties
- A number of changes to flag classes
  - Renamed **FlagLike** to **Flag** and **Flag** to **SettableFlag**
  - **ResettableFlag** now requires the implementation of `value_=(...)` instead of `set()` and `reset()`
- **Resettable** is now required to implement `isSet: Boolean`
- `StdIn.selectFrom(...)` doesn't print "Found X items" anymore
- `!` in **Regex** now doesn't use `[^...]` when the content contains special characters, such as parentheses, `|` or brackets.
- Removed `extends Any` from **View**, **Switch**, **Changing** and **Pointer**
### Bugfixes
- **PairOps**`.minMax` was bugged in the previous version, returning the items in the wrong order
- **XmlReader** now doesn't include text content which was outside the read XML elements (such as tabs etc.)
- `StdIn.selectFrom(...)` and `.selectFromOrAdd(...)` now correctly apply filtering
- Bugfix to **AsyncProcessMirror**, which would previously get stuck at queued mappings
- **TripleMergeMirror** now correctly declares changing stopped if its mirroring condition gets fixed to false
- Removed accidental test print from `bestMatch(...)` (**CollectionExtensions**)
### Deprecations
- Deprecated **VolatileList** and **VolatileOption** in favor of `Volatile.seq` and `Volatile.optional` 
  which now provide almost the same interface using implicit functions.
- Deprecated a lot of logging functions in **Try** and **TryCatch**, 
  in favor of a new more simple `.log` & `.logWithMessage(String)` functions
- Renamed `Regex.withinParenthesis` to `.withinParentheses ` and `.withBraces` to `.withinBrackets`
### New features
- **ThreadPool** now supports **ExcEvents** and **ExcListeners**
- **Changing** items now have a more robust **Logger**-based handling of errors thrown by **ChangeEventListeners**
- Added **IntSet** class, which stores integers, utilizing their consecutive nature by treating them as ranges
- Added **LazyTripleMergeMirror** for more complex lazy pointer-merging
- Added **Settable** and **Switch** classes, which provide the functionality of 
  **SettableFlag** (previously **Flag**) and **ResettableFlag** without change events
  - Related to this update, added **MaybeSet** trait
- Added **VolatileSwitch** class, which implements the features of **VolatileFlag**, but without change events
- Added **WeakCache** (new version), and **WeakKeysCache**
- **Loggers** are now implicitly convertible to **ScopeUsable**, allowing for scoped logging definitions
- Added `.spanTo(...)` and `.spanFrom(...)` to numeric classes via **RangeExtensions**
### New methods
- **Changing**
  - Added a number of new merge function variants:
    - Merging with n pointers
    - Separate strong merging (which matches previous mergeWith)
    - A new variant of `.lazyMergeWith(...)`
- **ConsoleExtensions**
  - Added `.parseDate(String)`, which provides semi-flexible date-parsing 
    often useful when dealing with user (console) input
- **CopyOnDemand** (object)
  - Added a new function-based constructor
- **FromModelFactory**
  - Added `.fromPath(Path)`
- **IterableOnce** (**CollectionExtensions**)
  - Added `.toOptimizedSeq`
  - Added `.toTryCatch` for collections that contain instances of **TryCatch**
  - Added `.toIntSet` for collections of **Ints**
- **Iterator** (**CollectionExtensions**)
  - Added `.pairedTo(...)`
- **LoopingProcess**
  - Added `.toTimedTask` and `.runAsTimedTask()`
- **OnceFlatteningPointer**
  - Added `.tryComplete(Changing)`
- **PairingIterator** (object)
  - Added `.to(...)`
- **PairOps**
  - Added `.flatMapFirst(...)`, `.flatMapSecond(...)` and `.flatMapSide(...)`
- **Pointer** and its subclasses
  - Added a number of new constructors and refactored certain existing constructors
  - A large number of new methods are now available of **Pointers** of type **Option** and of type **Seq**
- **Seq** (**CollectionExtensions**)
  - Added `.pairedTo(...)`
- **ThreadPool**
  - Added multiple new methods for tracking the current thread-usage
- **Try** (**TryExtensions**)
  - Added `.log` and `.logWithMessage(String)`
- **TryCatch**
  - Added `.log` and `.logWithMessage(=> String)`
### Other changes
- Built with Scala v2.13.14
- Merge functions in **Changing** now apply optimizations to source-pointer listening
  - The previous implementations are available as `strongMergeWith(...)` variants
- Multiple changes to **SingleThreadExecutionContext**:
  - This execution context now supports automatic disposal of the managed thread in case it remains idle for 
    long enough (optional feature)
  - This interface now exposes `stop()`, with which the current executing thread may be killed
    - This also discards all the queued tasks, but does not interrupt the currently running task
- `.distinctWith(...)` in **IterableOnce** (via **CollectionExtensions**) now accepts an **EqualsFunction** 
  instead of a regular function
  - Use-cases should get resolved implicitly, however
- String to number conversions in **Value** now trim the string and remove any control characters, such as newlines
- In **WeakCache**, both the keys and the values are now weakly referenced
  - The previous implementation, where only values are weakly referenced, is now **WeakValuesCache**
- Modified **HasEnds** `toString` implementation
- **ListenableResettableLazy** is now covariant
- The result type parameter in **LazyMergeMirror** is now covariant
- "Create new" option in `StdIn.selectFromOrAdd(...)` is now indented
- Added `Always(Boolean)` for creating **AlwaysTrue** or **AlwaysFalse**
- Internal refactoring within **ThreadPool**
- Added new `toSeq` & `toIndexedSeq` implementations to **EmptyView**, **SingleView** & **PairView**
- Minor refactoring in pairing functions in **CollectionExtensions**
- Refactored `.onceAllSourcesStop(...)` in **MayStopChanging**

## v2.4 - 28.07.2024
This is a pretty large update, mostly due to its delayed release.
This update focuses on the following areas: 
- Short **Seq** collections, namely: **Empty**, **Single** & **Pair**
  - Added simple collection classes to handle cases where there are 0-1 items
  - Made a lot of improvements / optimizations on the functionality of **Pair**
  - Added **LazyPair**, as well as some new **scala.collection.View** implementations
- **Graph** search algorithms
  - This update introduces a completely new interface for performing searches within **Graph**s. 
    - These allow for a much wider range of use-cases and optimizations
  - The search algorithm itself was also improved somewhat
- **Model** validation
  - Rewrote large parts of the **Model** validation logic & interface. 
  - The current version supports lazy initialization.
- **Pointer**s / the **Changing** interface
  - **Pointer** + **Changing** -interface can now be referenced directly with **EventfulPointer**, 
    rather than having to use the `with` keyword
  - New pointer types: **CopyOnDemand**

Besides these, there are smaller changes concerning:
- Collection utilities, namely: **ZipBuilder**, **ConsecutivelyDistinctIterator** and a number of new collection methods
- **StdIn** / console: More interaction templates via **ConsoleExtensions** + **StringUtils** for producing ASCII tables
- **EqualsBy** functionality
- **ActionQueue**: Slightly modified interface and logic
- Logging: New logging implementation: **LogQueue**

### Breaking changes
- In most places where **Vector** was used required, **Seq** is now used
- Removed `.notEmpty` from **StringExtensions** because of ambiguities when combined with **CollectionExtensions**
  - Now present in **StringExtensions** as `.ifNotEmpty`
- **ModelDeclaration**`.validate(AnyModel)` now returns a **Try** instead of **ModelValidationResult**
- **ModelLike** implementations are now required to provide `.generatesNonEmptyFor(String)`
- **PropertyFactory** implementations are now required to provide `.generatesNonEmpty(String)`
  - Consequently, modified some of the existing **PropertyFactory** constructors to request this information 
- **EventfulPointer** is now a trait instead of a class
  - The new trait is intended to be used for all interfaces that provide both **Pointer** and **Changing** interfaces
- **EqualsBy**`.equalsProperties` now returns **Seq** instead of **Iterable**
- The write functions in **FileExtensions**, which accept a function, now return the return value of that 
  function, and no longer the path itself.
- **Graph** search functions now assume that the edge cost function always returns a value greater than zero
  - While this may result in incomplete results if ignored, that shouldn't cause much harm in most use-cases
### Deprecations
- Deprecated **ModelValidationResult** (now replaced with **Try**)
- Deprecated all existing graph search functions in favor of new versions with different syntax
- Deprecated `DataTypeException.apply(...)` in favor of the more typical syntax `new DataTypeException(...)`
- Renamed `.filterWithPaths(...)` in **TreeLike** to `.pathsToRootsWhere(...)` because the term "filter" is misleading
### Bugfixes
- `.tryVector` and `.tryString` in **Value** now return a success for empty values (used to fail before)
- **EqualsBy** comparison didn't work in all cases, since it was dependent on **Iterable** `==`, which could vary. 
  The new implementation works the same way, regardless of compared collection type 
  (although it now requires **Seq** to be used)
- Fixed a bug in **CachingSeq**`.apply(Int)`
- **ActionQueue**`.pushAsync(...)` now properly blocks the start of the next action
### New features
- Added a more advanced (and optimized) interface for **Graph**-searches
- Added new interactive utility functions to **StdIn** via **ConsoleExtensions** 
- Added **CopyOnDemand** pointer type that copies a **View** value whenever requested
- Added **LogQueue** for capturing log entries
- Added the (rewritten) **Filter** class from the **Inception** module
- Added **Empty** object, an optimized empty **IndexedSeq** implementation
- Added **Single** class, an optimized **IndexedSeq** implementation for individual items
- Added **OptimizedIndexedSeq** for building instances of **Empty**, **Single**, **Pair** or **Vector**
- Added **ConsecutivelyDistinctIterator** class
- Added **FromCollectionFactory** trait
- Added **ZipBuilder** class
- Added **LazyPair** class
- Added **OpenEnumeration** and **OpenEnumerationValue** in order to facilitate the creation of cross-module enumerations
- Multiple new features to **ActionQueue**:
  - Added `.asExecutionContext`
  - The push and prepend functions now return a **QueuedAction** instead of a **Future**. **QueuedActions** support states. 
    I.e. you may track when the wrapped action is actually started / running.
- Added **StringUtils** object for converting data into a console-displayable table
### New methods
- **ArgumentSchema**
  - Added `.isFlag`
- **Changing**
  - Added `.viewWhile(FlagLike)`
  - Added `.onceFixedAt(...)`
- **FileEditor**
  - Added `.nextLineIterator`
- **Fixed** (object)
  - Added `.never` which is an alias for `Fixed(None)`
- **FlagLike**
  - Added `.lightSwitch(...)` and `.switch(...)`, which are mapping functions specialized for boolean values
- **HasTwoSides**
  - Added `.iteratorWithSides` from **Pair**
  - Added `.zipWithSide`
  - Added `.sideWhere(...)`
- **Iterable** 
  - **AsyncCollectionExtensions**
    - Added `.mapParallel(Int)(...)`, 
      which utilizes multiple (although a limited number of) threads to map the collection contents
  - **CollectionExtensions**
    - Added `.notEmpty`
    - Added `.extremeByOption(Extreme)(...)`
    - Added `.takeMax(Int)`, `.takeMin(Int)`, `.takeMaxBy(Int)(...)`, `.takeMinBy(Int)(...)`, 
      `.takeExtreme(Int, Extreme)` and `.takeExtremeBy(Int, Extreme)(...)`, 
      which collect n smallest or largest items from the collection
    - Added `.findForAll(...)`
- **IterableOnce**
  - **AsyncCollectionExtensions**
    - Added `.foreachParallel(Int)(...)`
  - **CollectionExtensions**
    - Added `.nonEmptyIterator`
    - Added `.countAll` which returns the number of times each unique item appears in the collection
    - Added `.mapCaching(...)`
- **Iterator** (**CollectionExtensions**)
  - Added `.notEmpty`
  - Added `.consecutivelyDistinct`
- **Lockable** (object)
  - Added `.view(Changing)` which constructs a **LockableBridge**
- **PairOps**
  - Added `.findForBoth(...)` which functions like `.findForAll(...)` in **CollectionExtensions**, 
    but preserves **Pair** type.
- **PollingIterator** (object)
  - Added `.from(Iterator)`
- **PropertyFactory**
  - Added `.mapResult(...)`
- **Regex**
  - Added `.replaceOthers(String, String)` that works somewhat like `(!this).replaceAll(...)`, but does so more reliably
- **ResettableFlag**
  - Added `.switch()`
- **Seq** (**CollectionExtensions**)
  - Added `.appendAllIfDistinct(...)`
  - Added `.groupConsecutiveWith(...)`
- **String** (**StringExtensions**)
  - Added `.isMultiLine`
  - Added `.containsAllCharsFrom(IterableOnce)` and `.containsCharsInOrder(IterableOnce)`
  - Added `.replaceAllExcept(Regex, String)`
  - Added `.forNonEmpty(...)`
- **TreeLike**
  - Added `.rootsWhereIterator(...)`, `.rootsBelowWhereIterator(...)` and `.pathsToRootsWhereIterator(...)`
- **Try** (**CollectionExtensions**)
  - Added `.foreachOrLog(...)`
- **TryCatch**
  - Added a couple mapping functions and `.orElse(TryCatch)`
- **WeekDay** (object)
  - Added `.matching(String)`
### Other changes
- Rewrote the `.validate(AnyModel)` function in **ModelDeclaration**, so that it:
  - Uses `.apply(...)` instead of `.containsNonEmpty(...)` when testing for the required properties
    - This is in order to support lazily generated properties (i.e. non-default property factories)
  - Only applies non-required property declarations on-demand (when / if those properties are requested)
    - This also means that these properties won't appear in the resulting model's defined properties until 
      they're specifically requested using `.apply(...)`
  - `.apply(...)` in the resulting model also supports alternative property names
  - The resulting model still utilizes the original model's property factory
- **ModelLike**`.contains(String)` and `.containsNonEmpty(String)` now support lazily initialized properties 
  (e.g. those returned via the new model validation implementation)
- `~==` implementation in **Value** is now more flexible and takes into account approximate equality in inner values as well 
- Conversion from **String** to date, instant and date-time values is now much more flexible
- Conversion from **String** to **Float** and **Long** (via **Value**) is now slightly more flexible
- The generic type parameter in **PropertyFactory** is no longer restricted 
- `View.fixed(...).mapValue(...)` now produces a fixed view instead of a **Lazy**
- `View(...).mapValue(...)` now produces a call-by-name (mapping) view instead of a **Lazy**
  - I.e. mapping results are no longer cached in these cases
- The `mirrorCondition` in **OptimizedMirror** is now stricter
  - Previously the value would still reflect the updated value when called directly
  - Also, now `.destiny` correctly changes when mirror condition seals to `false`
- **XmlElement**`.toString` now yields XML instead of JSON.
- **Iterator**`.pollable` now tests if the iterator is already pollable
- **Regex** now extends **MayBeEmpty**
- Added **EmptyInput** interface to **TwoThreadBuffer**
- **GraphNode** and **GraphEdge** now extend **Extender**, providing implicit access to their wrapped values
- Minor optimization to **FlagLike** -wrapping
- Minor optimization to **AlwaysTrue** and **AlwaysFalse** merge functions
- Minor optimization to certain -while functions in **Changing**
- Minor optimization to **VolatileList** -constructing
- Optimized certain **Pair** functions
  - E.g. `++` now returns a **Pair** if no elements are added
- **Pair** now also introduces its own **BuildFrom**, **IsIterable** and **IsSeq**
- Size-related optimizations to **CachingSeq**

## v2.3 - 22.01.2024
This update contains a very large number of new methods, features and bugfixes. 
The most major updates concern:
- **Changing**: Further optimization, especially around cases where changing stops
- **TimedTask**: Support for cancellation & rescheduling
- **Matrices**: Major optimization & refactoring
- **TwoThreadBuffer** & **PrePollingIterator** (both new): Tools for asynchronous iteration
### Breaking changes
- Divided the `operator` package into multiple sub-packages
- Rewrote large portions of **TimedTask** and **TimedTasks** classes in order to add support for task rescheduling
- Multiple breaking changes concerning **Changing**
  - The abstract functions `.isChanging` and `.mayStopChanging` were replaced with `.destiny`
  - The abstract function `_addListenerOfPriority(...)` no longer needs to account for the case
  where the item has already stopped from changing, as it will only be called for still-changing items
- **Cache** is now a trait instead of a class, and replaces the **CacheLike** -trait
- Rewrote some parts of the **Matrix** classes
  - Implementing classes need to be adjusted a lot
  - `.iterator` and `.indexIterator` ordering may have changed, use the specifically ordered variants in case your 
    use-case is ordering-specific
  - `.map(...)` in **MatrixView** now doesn't cache map results
- Renamed **CanBeZero** to **MayBeZero** and **CanBeAboutZero** to **MayBeAboutZero**
- Moved **JsonSettingsAccess** to `parse.json`
- `.divideBy(...)` in **CollectionExtensions** now returns a **Pair** instead of a **Tuple**
  - You can work around this by appending `.toTuple` after these method calls
- Deleted classes and functions that were deprecated at v2.0 or earlier
### Deprecations
- `.view` in **Volatile** and **EventfulPointer** is now replaced with `.readOnly` in **Changing**
- Renamed `Regex.parenthesis` to `.parentheses`
### Bugfixes
- **JsonSettingsAccess** settings file search algorithm was bugged, now fixed
- Fixed a bug in **Path**`.parts` and `.partsIterator` (used to throw an **IllegalArgumentException**)
- `ResettableFlag(initialValue = true)` now correctly sets the initial value
- **StringFrom** now correctly preserves line-breaks
- **EqualsBy** now compares the equals-properties using `==` instead of using hashCodes
- **Volatile**`.mutate(...)` now properly locks the wrapped item (whoops!)
- **PostponingProcess** now properly reacts to wait time change events that occur during the function run
- `PairingIterator.apply(IterableOnce)` now pre-polls the first item in order to avoid wrong sequencing between 
  `hasNext` and `next()`
- The following **Changing** implementations now properly take into account 
  the listening condition for the purposes of stopping changing: **LightMergeMirror**, **MergeMirror**
### New Features
- **TimedTask** and **TimedTasks** now support pointer-based task-rescheduling
- Added file-related utility methods under **FileUtils**
- Added a new iterator class **PrePollingIterator** for asynchronous iterator buffering
- Added new **TwoThreadBuffer** class for buffered parallel push & pull operations
- Added **CollectSingleFailureLogger** for specialized logging use-cases 
  where errors need to be caught and handled later (typically for **Try**`.failWith(...)` / `.failIf(...)`)
- Added **TimedSysErrLogger** class
- Added **Use** object for providing scope-specific (implicit) access to certain values
- Added **LockableBridge** class
- Added **ChangingUntil** & **StatefulValueView** **Changing** mirror classes
- Added **ChangeResult** model, relating to other **Changing** changes
- Added conditional mirroring support to **OptimizedMirror**
- Added a **PairView** class, as well as a general **HasTwoEnds** -trait
- Added **CacheLatest** class
- Added **HasSign** and **HasBinarySign** traits based on the **Signed** and **BinarySigned** traits
- Added **HasExtremes** -trait
- Added **SomeBeforeNone** utility ordering class for altered **Option**-ordering
### New methods
- **ActionQueue**
  - Added `.queueSizePointer: Changing[Int]`
  - Added `.pushAsync(=> Future)`
  - Added `.prepend(...)` variations
- **CompoundingBuilder**
  - Added `.popAll()`
- **Changing**
  - Added multiple new until & withState methods
  - Added utility functions (`.isAlwaysEmpty`, `.mayBeNonEmpty`, etc.) to **Changing** items that contain 
    collections or **MayBeEmpty** instances
  - Added `.flatten` function for **Changing** items that contain **Changing** values
- **Delay**
  - Added `.future(...)`
- **FromModelFactory**
  - Added `.mapParseResult(...)` and `.flatMapParseResult(...)`
- **Future** (**AsyncExtensions**)
  - Added `.or(Future)`
- **IterableOnce** (**CollectionExtensions**)
  - Added `.zipAndMerge(...)`,  `.zipMap(...)` and `.zipFlatMap(...)`
  - Added `.foreachWhile(=> Boolean)(...)`
- **Iterator** (**CollectionExtensions**)
  - Added `.mapSuccesses(...)` and `.flatMapSuccesses(...)` to **Iterators** containing instances of **Try**
  - Added `.prePollingAsync(Int)` that utilizes the new **PrePollingIterator**
- **Lazy**
  - Added `.mapCurrent(...)`
- **LoopingProcess**
  - Added `.skipWait()` utility function
- **MatrixLike**
  - Added `.indicesAroundIterator(...)` for iterating over an area around a certain position
- **MappingCacheView**
  - Added `.toCache`
- **MayStopChanging**
  - Added a couple of "onceNotChanging" method variants
- **Model** (object)
  - Added `.from(AnyModel)`
- **OptimizedBridge**
  - Added `.detach()`
- **Pair**
  - Added `.mapAndMerge(...)(...)`
- **Path** (**FileExtensions**)
  - Added `.real`, which is a safe version of `.toRealPath()`
  - Added `.hasSameContentAs(Path)` for identical file -searching (not yet tested)
  - Added `.tryIterateChildrenCatching(...)`
- **Regex** (object)
  - Added a couple of new regular expression values
- **Seq** (**CollectionExtensions**)
  - Added `.appendIfDistinct(...)`
- **Sign**
  - Object
    - Added `.apply(End)`
  - Class
    - Added `.end` and `.toOrdering`
    - Added new `*` variant
- **SpanLike**
  - Added `.withEnd(A, End)`, as well as multiple map end variations
- **String** (**StringExtensions**)
  - Added `.prependIfNotEmpty(String)` and `.appendIfNotEmpty(String)`
- **Try** (**CollectionExtensions**)
  - Added `.failWith(Throwable)` and `.failIf(=> Option[Throwable])`
- **View**
  - Added `.flatten` to **Views** that contain **Views**
### Other changes
- **Path** now extends **ApproxEquals**, as well as **MayBeEmpty** (after importing **FileExtensions**)
  - `~==` checks whether the two paths target the same file
  - `.isEmpty` checks whether the targeted directory is empty
    - Returns false for existing regular files
- **Future**`.raceWith(Future)` now handles situations where both futures fail
- Added stop condition support to **LightMergeMirror**
- **Changing**`.mapWhile(...)` now uses **OptimizedMirror** instead of a **Mirror**
- Added some optimizations to **Flags** and other **Changing** items
- Added `.toString` implementations to **ComparisonOperator**
- Scala version updated to 2.13.12

## v2.2 - 27.09.2023
This update focuses on the pointer system (i.e. the **Changing** trait).  
There are also some new numerical features, as well as a large number of utility and quality improvements.
### Highlights
- Updated **Changing** interface (new event types), including new pointer types
- **Sign** & **Signed** refactoring
- New **UncertainNumber** class
### Breaking Changes
- Renamed **HasEnds** to **HasOrderedEnds** and **HasInclusiveEnds** to **HasInclusiveOrderedEnds**
  - Added new more generic traits to replace the old name versions
- Multiple changes relating to **Signed**, **SignedOrZero** and **BinarySigned**:
  - **Signed** now requires the implementation of `.sign: SignOrZero` 
    instead of separately defining `.isPositive` and `.isNegative`
    - In **BinarySigned**, this property is `.sign: Sign`
  - `.sign` now returns **SignOrZero** for **SignedOrZero** and **Signed** traits, 
    and will no longer return **Positive** in case of a zero value
- `Sign.of(...)` now returns **SignOrZero** instead of **Sign**
- Renamed **PointerWithEvents** to **EventfulPointer** and **IteratorWithEvents** to **EventfulIterator**
- Updated the abstract functions in **Changing**
  - Added four new required abstract functions:
    - `.hasListeners: Boolean` and `.numberOfListeners: Int`
    - `.mayStopChanging: Boolean` and `_addChangingStoppedListener(=> ChangingStoppedListener): Unit`
  - `.addListener(ChangeListener)` and `.addDependency(ChangeDependency)` are no longer abstract and is instead 
    replaced with `.addListenerOfPriority(End)(ChangeListener)`
  - `.addListenerAndSimulateEvent(...)` is no longer abstract and now contains an additional parameter
- **ChangeListener** now returns **ChangeResponse** instead of **DetachmentChoice**
  - **DetachmentChoice** is still considered a **ChangeResponse** for backwards-compatibility, but 
    a **Boolean** will no longer implicitly map to a **DetachmentChoice**
- Replaced **ChangeDependency** with new version of **ChangeListener**
- **IterableOnce**`.toTryCatch` now returns a **TryCatch** instead of a **Try**
- **String**`.splitAtFirst(String)` and `.splitAtLast(String)` in **StringExtensions** 
  now return a **Pair** instead of a **Tuple** 
- Renamed **UncertainBoolean.Uncertain** to **UncertainBoolean** and 
  **UncertainBoolean.Certain** to **UncertainBoolean.CertainBoolean**
- Renamed **Value**`.trySting` to `.tryString` (typo)
- **ListenableResettableLazy** is now a trait and not a class (i.e. the `new` keyword no longer works in this context)
- The default implementation of **Changing**`.map(...)` now uses an **OptimizedMirror**. 
  - It may be appropriate to review the uses of this method and to see whether 
    `.strongMap(...)` or `.mapWhile(...)` would be more appropriate options. 
### Deprecations
- Deprecated **HasEnds**`.toPair` in favor of `.ends`
- Deprecated **ChangeDependency** and **DetachmentChoice** (see breaking changes)
- Deprecated certain listener properties in **AbstractChanging** in favor of new listener style properties
- Deprecated `.fireChangeEvent(...)` in **AbstractChanging** in favor of new `.fireEvent(...)` variants
  - Please note the slightly different functionality 
    - I.e. that `.foreach { _() }` must be called for the result value in order to actuate the scheduled after-effects
- Deprecated `.toPair` in **ChangeEvent** because the name implied conversion
- Deprecated `Pointer.withEvents(A)` in favor of `Pointer.eventful(A)`
  - Similarly, renamed **Iterator**`.withEvents(...)` to `.eventful(...)`
- Deprecated `.isPositive` and `.isNegative` in **Signed**
- Renamed `.indexWhereOption(...)`, `.optionIndexOf(...)` and `.lastIndexWhereOption(...) `
  to `.findIndexWhere(...)`, `.findIndexOf(...)` and `.findLastIndexWhere(...)`
- Renamed various prebuilt **Regex** instances
- Renamed `.isInFuture` and `.isInPast` from **Instant** (**TimeExtensions**) to `.isFuture` and `.isPast`
- Renamed `.map(...)` to `.mapTo(...)` in **SpanLike** because of name conflicts
- In **UncertainBoolean**, deprecated `.value` in favor of `.exact`
### Bugfixes
- Fixed deadlock issues in **PostponingProcess**
- **Pair**`.equalsUsing(EqualsFunction)` didn't work previously
- Fixed a logic error in **PairingIterator** `:+`
### New Features
- Added new **SignOrZero** enumeration with three values: **Positive**, **Negative** and **Neutral**
  - The **Positive** and **Negative** options are still available as a binary pair under trait **Sign**
- Added **UncertainSign** enumeration and **UncertainNumber** class
- **ChangeListeners** can now cause after-effects to be triggered after the completion of a change event
- Added **ChangingStoppedListener** trait, which is supported by all **Changing** instances
  - For the **Changing** implementations, added **MayStopChanging** trait and **AbstractMayStopChanging** class
  - Also optimized change listener handling in many pointers
- Added **ConditionalChangeReaction** class/object for creating **ChangeListeners** that attach or detach themselves when 
  an external condition is met
  - These are utilized in **Changing**`.mapWhile(...)` and as optional features in other map-like functions, 
    as well as in `.addListenerWhile(...)`
- Added **OnceFlatteningPointer** class that resembles **SettableOnce**, 
  except that it will wrap another pointer (i.e. not just a value) once complete.
- Added **LockablePointer** class
- Added **ListenableMutableLazy** class and **ResetListenable** trait
- Added new **TryCatch** utility class for handling situations that included non-critical failures
- Added **JsonSettingsAccess** utility class for processing json settings files
- Added new **Steppable** trait for items that provide step-based iteration
- Added sorting support to immutable **Models**
- **Value**, **Constant** and **Model** now extend **ApproxEquals**
- Added **RoundingFunction** utility trait
### New Methods
- **Changing**
  - Object
    - Added `.completionOf(Future)`
  - Instance
    - Added `.nextChangeFuture: Future[ChangeEvent]`
    - Added `.addListenerAndPossiblySimulateEvent(...)`, a variant of `.addListenerAndSimulateEvent(...)`
    - Added new map and merge variants (e.g. `.strongMap(...)` and `.lightMergeWith(...)`)
- **ConditionalLazy** (object)
  - Added `.ifSuccessful(...)` that only caches successful attempts
- **Either** (**CollectionExtensions**)
  - Added `.eitherAndSide` for symmetric Eithers
  - Added `.mapSide(End)(...)` for symmetric Eithers
- **Extreme** (object)
  - Added `.apply(Sign)`
- **FlagLike**
  - Added `.onceNotSet(...)`
- **HasEnds** & **IterableHasEnds** (object)
  - Added constructors
- **Iterable** (**CollectionExtensions**)
  - Added `.ends` and `.endsOption`
  - Added `.mapTo(...)` for `.toMap` -conversion
- **IterableOnce** (**CollectionExtensions**)
  - Added `.pairedBetween(...)`
  - Added `.split` -property for collections that consist of **Tuples**
- **IterableSpan** (object)
  - Added new constructors
- **Iterator**
  - Added `.trySucceedOnce` for **Iterators** that contain **Tries**
- **Instant** (**TimeExtensions**)
  - Added `+` and `-` support for **Durations** (including `Duration.Inf`)
- **LocalDate** (**TimeExtensions**)
  - Added `.isFuture`, `.isPast` and `.isToday`
- **MapAccess** (object)
  - Added `.apply(...)` for converting functions into map accesses
- **PairedIterator** (object)
  - Added new constructors
- **Pointer**
  - Added `.mutate(...)`, which behaves exactly like `.pop(...)` in **Volatile**
- **PostponingProcess** (object)
  - Added a new constructor
- **Seq** (**CollectionExtensions**)
  - Added `.reverseSorted`, `.reverseSortBy(...)` and `.random`
  - Added `.mapEnd(End)(...)`, `.mapFirst(...)` and `.mapLast(...)`
  - Added `.pairedFrom(...)` and `.pairedBetween(Pair)`
- **Span** (object)
  - Added `.numeric(...)`
- **SpanLike**
  - Added `.map(...)`
- **String** (**StringExtensions**)
  - Added `.dropRightWhile(...)`
  - Added `.notStartingWith(String)` and `.notEndingWith(String)`
- **Throwable** (**ErrorExtensions**)
  - Added `.causesIterator`
- **TreeLike**
  - Added `.bottomToTopNodesIterator`
  - Added `.nodesBelowIteratorUpToDepth(Int)`
- **Try**
  - **AsyncExtensions**
    - Added `.toCompletedAttempt: Future[A]`
    - Added `.flattenToFuture `variant that supports **TryCatch**
  - **CollectionExtensions**
    - Added `.toTryCatch`
    - Added `.logToOption` and `.logToOptionWithMessage(String)`
    - Added `.flatMapCatching(...)`, a flatMap that supports **TryCatch**
- **Year** (**TimeExtensions**)
  - Added `.dates`
### Other
- Added a **Sided[A]** type alias for **Either[A, A]** to **CollectionExtensions**
- Added a **Mutate[A]** type alias for **A => A** functions
- Added a couple new constructors for **Span** classes
- **Pair**`.separateMatching` now uses `EqualsFunction.default` by default
- **Tree**`.map(...)` and `.flatMap(...)` now use `EqualsFunction.default` by default
- Added a low-priority conversion from **LocalTime** values to **LocalDateTime** values (assigns the current date as the date)
- `Sign.of(...)` now accepts a wider range of numeric classes
- `PollableOnce.apply(...)` now accepts the parameter as call-by-name
- **Console** now accepts its prompt as a call-by-name parameter

## v2.1 - 01.05.2023
This update mostly introduces new utility functions and some new utility classes.  
Most of the changes target **WeekDay** and **Pointer** classes. 
This update also introduces a number of new functions for collections.
### Breaking Changes
- Many **WeekDay** -related functions now require an implicit **WeekDays** -parameter
- `WeekDay.current()` is now `WeekDay.current`
- `YearMonth.week(WeekDay)` is now `YearMonth.week(implicit WeekDays)`
- Renamed `.mapAsync(...)` variations in **Changing** to `.mapToFuture(...)`. Added new `.mapAsync(...)` implementations.
- Replaced `allowReplace: Boolean` -parameter with `conflictResolve: FileConflictResolution` in move and copy -operations
### Bugfixes
- There was a major performance issue with `.view(...)` and `.size` in **Matrix**, which is now fixed
- Fixed **Path**`.relativeTo(Path)`
- Fixed a bug in **Iterable**`.filterBy(Extreme)(...)`
- Fixed a bug in **AbstractChanging** where change event listeners would not get detached properly
- Fixed a bug in **TripleMergeMirror** where the third input was not always mirrored properly
### Deprecations
- Deprecated `.equalsUsing(...)` and `.notEqualsUsing(...)` in favor of `.isSymmetricWith(...)` and `.isAsymmetricWith(...)`
- Deprecated `WeekDay.iterate(WeekDay)` and `.reverseIterate(WeekDay)` in favor of `.iterate` and 
  `.reverseIterate` in **WeekDay** (instance) 
### New Features
- Custom file conflict resolutions can now be used in standard file move and copy operations
- The first day of the week may be configured using **WeekDays**
- Added **NumberExtensions** -utilities (`utopia.flow.util.NumberExtensions`)
- Added **IndirectPointer** and `Flag.wrap(...)`
- Added **SingleThreadExecutionContext** class
- **FileLogger** now supports time-based log entry grouping
- Added **UsesOrdering** and **UsesSelfOrdering** traits for items that use multiple orderings
### New Methods
- **Changing**
  - Added `.onNextChangeWhere(...)(...)` and `.once(...)(...)` that area a synchronous alternative to 
    `futureWhere(...).foreach(...)`
- **Iterable** (**CollectionExtensions**)
  - Added `.minMaxBy(...)` and `.minMaxByOption(...)`
  - Added `.oneOrMany` and `.emptyOneOrMany`
  - Added `.toMapBy(...)`
  - Added `.replaceOrAppend(...)`, which is a variation of `.mergeOrAppend(...)`
  - Added alias `.hasEqualContentWith(Iterable)(EqualsFunction)` for `~==`
- **MatrixLike**
  - Added `.columnIndices` and `.rowIndices`
- **Pair**
  - Added `.isSymmetricWith(EqualsFunction)` and `.isAsymmetricWith(EqualsFunction)`
  - Added `.minMax` and `.minMaxBy`
- **Path** (**FileExtensions**)
  - Added `.isChildOf(Path)`
- **Pointer**
  - Added `.filterCurrent(...)`, `.filterNotCurrent(...)` and `.mapCurrent(...)` to **Pointers** that contains **Options**
- **SettableOnce**
  - Added `.onceSet(...)` that works like `.future.foreach(...)` but is synchronous
- **StdIn** (**ConsoleExtensions**)
  - Added `.readLineIteratorWithPrompt(String)`
- **String** (**StringExtensions**)'
  - Added `.isSimilarTo(String, Int)`
- **Try** (**CollectionExtensions**)
  - Added `.logFailure` and `.logFailureWithMessage(String)`
### Other
- In **TimeLogger**, `.print()` must now be called separately
- **Pair**`.isSymmetric` and `.isAsymmetric` now accept an implicit **EqualsFunction** parameter (where default is ==)
- **Path**`.write(...)` and its variants (in **FileExtensions**) now automatically create the 
  parent directories before writing
- `EqualsFunction.by(...)` now has a default **EqualsFunction** parameter
- EqualsExtensions now contains `~==` separately for **Options** 

## v2.0 - 02.02.2023
This update represents a major refactoring of the standard **Flow** features.  
While most updates concern naming and package structure, there are also a number of new features and data structures.

While this update may be tedious to apply (you'll need a lot of Shift + Ctrl + R), you are rewarded with a 
nice set of new tools and a more intuitive naming and package structure.
### Breaking Changes
- Reorganized the package structure
- Renamed `def repr` to `def self` in multiple places, 
  because `repr` conflicts with `final def repr: Iterable[A]` in **Iterable**
- Changes relating to variants of **Changing** (or **ChangingLike**)
  - **Changing** is now an abstract class **AbstractChanging** already containing `listeners` and `dependencies` -variables
  - **ChangingLike**, then was renamed to **Changing** instead
  - Removed the implicit class for **Changing** of **Boolean**, moved these methods to **FlagLike** and
    added an implicit conversion, which is available through `import utopia.flow.view.template.eventful.FlatLike.wrap`
  - Modified internal mapping and merging logic, allowing for more advanced (incremental) merge functions
    - This affects mirror instance creation using the keyword `new`. Cases where `.of(...)` is used are not modified.
  - **Changing**`.delayedBy(Duration)` now accepts the duration as call-by-name
- The following traits were renamed and replaced an existing class with the same name
  - **Viewable** and **Settable** to **View** and **Pointer**
  - **LazyLike** and **ListenableLazyLike** to **Lazy** and **ListenableLazy**
  - **ResettableLazyLike** and **MutableLazyLike** to **ResettableLazy** and **MutableLazy**
- Renamed **LazyWrapper** to **PreInitializedLazy**
- Rewrote **DataType** class as a trait and moved all data type objects under the **DataType** object 
  - Consequently, `DataType.setup()` needn't be called anymore
- Renamed all attribute -related functions and references in **ModelLike** to property -related counterparts
- Changes relating to variants of **Model**:
  - Renamed **template.Model** to **ModelLike** and **mutable.Model** to **MutableModel**
  - **MutableModel** now accepts different constructor parameters
  - Rewrote parts of the **Model** classes
  - **Variable** is now a trait and not a class
    - **Variable** class (private) now also allows for more customization and behaves differently at default
  - Rewrote **PropertyChangeEvent** and event management in **MutableModel**
    - **PropertyChangeListener**`.onPropertyChanged(...)` is now `.onPropertyChange(...)`
  - Changes to **PropertyGenerator**, which is now **PropertyFactory**:
    - The `value` parameter in `.apply(...)` is no longer an **Option**
  - Changed to **PropertyDeclaration**:
    - Renamed and modified constructors
    - Added a new abstract property: `isOptional: Boolean`
- Changes relating to variants of **Tree**:
  - `.content` is now `.nav`
  - Replaced the abstract `.containsDirect(A)` with `.navEquals: EqualsFunction[A]`
  - Rewrote and renamed a number of functions
    - Tree combining (`+`) and appending (`:+`) now work differently by default; 
      They merge the trees together, instead of simply adding new child nodes.
    - For example, branches now contain node references instead of just the nav or content references
    - Leaves now includes the root node if it is empty. See `.leavesBelow` for the previous implementation.
    - Similarly, branches now include this node by default. `.branchesBelow` matches the previous
      implementation (i.e. `.allBranches`)
  - The mutable **Tree** is now **MutableTree** and mutable **TreeLike** is **MutableTreeLike**
  - **TreeLike** no longer extends **Node**
- Renamed **JSONReader** to **JsonReader** and **JSONReadEvent** to **JsonReadEvent**
- Moved **JsonReadEvent** types under the **JsonReadEvent** object
- Rearranged type parameters in **Combinable** and **Scalable**
- Renamed **Equatable** to **EqualsBy**, and changed its `public def properties: IterableOnce` to
  `protected def equalsProperties: Iterable`
- Renamed **ApproximatelyEquatable** to **ApproxEquals**
- Renamed **Zeroable** to **CanBeZero** and **ApproximatelyZeroable** to **CanBeAboutZero**
  - **CanBeZero** now also requires an implementation for a new abstract property `zero: Repr`
- Renamed **LinearMeasurable** to **HasLength**
- **CachingIterable** is now **CachingSeq** and **LazyIterable** is now **LazySeq**
  - Both also contain new functions
- Renamed **MapLike** to **MapAccess**
- **GraphNode** and **GraphEdge** no longer extend **Node**
- Renamed **ConversionReliability** values to PascalCase (e.g. from **NO_CONVERSION** to **NoConversion**)
- In **UncertainBoolean**, renamed a number of functions. Also renamed **Undefined** to **Uncertain**.
### Deprecations
- Deprecated all previous **PropertyGenerator** sub-classes in favor of the new **PropertyFactory** object functions
- Deprecated **Node** in favor of **View**
- Deprecated **NullSafe** in favor of `Option.apply(...)`
- Deprecated **NoSuchAttributeException** in favor of **NoSuchElementException**
- In **SignedOrZero**, deprecated `.positiveOrZero `and `.negativeOrZero` in favor of `.minZero` and `.maxZero`
- Deprecated a bunch of method in **Model** classes in favor of their renamed counterparts
- Deprecated **Generator** and **Counter**
- Deprecated `.iterator` in **Lazy** in favor of `.valueIterator`
- Deprecated `.compareWith(...)` in **Pair** in favor of `.merge(...)`
- Deprecated `.isNotSymmetric` in **Pair** in favor of `.isAsymmetric`
- Deprecated `.maxGroupBy(...)` and `.minGroupBy(...)` in **CollectionExtensions** in favor of 
  `.filterBy(Extreme)(...)` and its variants. 
### Bugfixes
- Fixed **Regex**`.times(Range)` that previously yielded invalid regular expressions
### New Features
- Added both immutable and mutable **Matrix** traits and implementations
- Added **CachingMap**, **LazyTree** and **LazyInitIterator**
- Added **ViewGraphNode** as a lazily initialized graph
- Added generic Range traits: **Span**, **IterableSpan**, **NumericSpan**, **HasEnds**, and so on
- Added **TimedTask** trait, which is now supported in **TimedTasks**, also
- Added new ways to write **LocalTime** values (after importing **TimeExtensions**)
- Added **SettableOnce** and **MutableOnce**, which are like **Promise**s with **ChangeEvent**s
- Added **ReleasingPointer** class
- Added **MaybeEmpty** trait and **NotEmpty** object for dealing with items that have the `.isEmpty` -property
- Added **End** and **Extreme** -enumerations (first, last, min, max)
  - Added new related methods via **CollectionExtensions**, also
- Added **Identity** object which functions as an identity function (i.e. `a => a`)
- Added **NoOpLogger** object
- Added **ApproxSelfEquals** trait
- Added **TryFuture** object that makes it easier to construct completed **Futures** with **Try** values.
- **ModelDeclarations** now support optional properties
### New Methods
- **CanBeAboutZero**
  - Added `.notCloseZero`
- **CanBeZero**
  - Added `.nonZeroOrElse(...)` and `.mapIfNotZero(...)`
- **Changing**
  - Added `.incrementalMap(...)`, `.incrementalMergeWith(...)` and `.incrementalFlatMap(...)` -methods
- **Either** (**CollectionExtensions**)
  - Added `.divergeMapLeft(...)` and `.divergeMapRight(...)`
- **Iterable** (**CollectionExtensions**)
  - Added `.hasSize` for more effective size comparisons (see **HasSize**)
  - Added `.only`, which returns **Some** when the collection contains exactly one item
  - Added `.minMax` and `.minMaxOption`
  - Added `.popWhile(...)`, which resembles `.takeWhile(...)`, 
    except that it also collects the remaining items as a separate collection
  - Added `.mapOrAppend(...)` and `.mergeOrAppend(...)` that either maps/merges an item into the collection, 
    or appends it.
  - Added `.end(Sign)` and `.endOption(Sign)` for head/last -access
- **IterableOnce**
  - Added `.existsExactCount(Int)(...)`
- **Iterator** (**CollectionExtensions**)
  - Added `.minMax` and `.minMaxOption`
- **Lazy**
  - Added `.map(...)` and `.flatMap(...)`
- **Path**
  - Added `.toTree`
- **Pair**
  - Added `.existsWith(...)` and `.forallWith(...)`
  - Added `.flatMergeWith(Pair)(...)`, `.findMergeWith(Pair)(...)` and `.toMapWith(Pair)`
  - Added `.oppositeOf(...)` and `.oppositeToWhere(...)`
  - Added `.isSymmerticBy(...)`, `.isAsymmetricBy(...)`, 
    `.equalsUsing(EqualsFunction)`, `.notEqualsUsing(EqualsFunction)`, `~==` and `!~==`
  - Added `.iteratorWithSides` and `.mapWithSides(...)`
  - Added a number of new methods for pairs that contain collections (accessed implicitly)
- **PointerWithEvents** (type)
  - Added `.empty`
- **Seq** (**CollectionExtensions**)
  - Added `.findAndPop(...)`, which finds an item and removes it from the collection
  - Added new `.slice(...)` variations
- **Signed**
  - Added a number of new utility functions
- **Tree** (object)
  - Added a new recursive constructor: `.iterate(...)`
- **View**
  - Added `.valueIterator`
### Other Changes
- **Changing(Like)** `.map(...)`, `.flatMap(...)`, `.lazyMap(...)`, `.mergeWith(...)`, `.lazyMergeWith(...)`
  and `.delayedBy(...)` are no longer abstract
- **Path** `.parentOption` (via **FileExtensions**) now converts the path to a root path, if necessary
- **Pair**`.sorted` now returns a **Pair**
- Optimized `~==` implementation for **Strings**

## v1.17 - 02.10.2022
This version contains a few larger changes and a large number of little updates and additions here and there.  

The more major changes include:
- Handling of **InterruptedException** in **Wait** and classes that utilize **Wait** (namely various **Process** classes)
- Changes to **ChangingLike** and **ChangeListener**, which enable temporary listeners
- Changes relating to synchronization, i.e. to **Volatile** classes, in order to make the software more resistant 
  to accidental deadlocks

These, and other changes are listed in more detail below.

### Breaking Changes
- Removed `~==` and `!~==` from **StringExtensions**, as they are now made available through **EqualsExtensions**
- **ChangeListener**`.onChangeEvent(ChangeEvent)` is now expected to return a **DetachmentChoice** instance, 
  which determines whether the listener will be removed from the applicable change event source
  - This may cause build errors in certain cases. 
    There are, however, implicit conversions from **Unit** (old use-case) and **Boolean** to **DetachmentChoice**, 
    so the earlier implementations should work in most cases, also.
  - **ChangingLike** implementations should also be altered in a manner which handles this return value. 
    **Changing** already does this, covering most of the use-cases.
- Altered **ChangingLike**`.futureWhere(...)` in following ways:
  - **ChangingLike** trait now implements this function by itself, meaning that subclasses are no longer required to 
    implement this function 
  - This function no longer accepts an implicit execution context, since the default implementation doesn't require one
- **ChangingLike** implementations are now required to implement `.removeDependency(Any)`
- Altered `.current` implementations in **AsyncExtensions** in following ways:
  - **Future**`.currentResult` now behaves as `.current` used to behave
  - **Future**`.current` now behaves as `.currentSuccess` used to behave
  - Removed **Future**`.currentSuccess` altogether
- Rewrote the **AsyncMirror** class (matching **ChangingLike**`.mapAsync(...)` -variants)
  - The mapping function must now return a **Future**
  - The resulting pointer now also shows the mapping process state 
    (e.g. whether an asynchronous mapping is occurring in another thread)
  - `AsyncMirror.apply(...)` no longer catches errors but expects merging logic to be provided
  - Renamed and modified **ChangingLike**`.mapAsync(...)` -variants
- **ResettableLazyLike**`.reset()` is now required to return a **Boolean**
- `new PollingIterator(...)` is now hidden, please use `PollingIterator.apply(...)` instead
### Deprecations
- Deprecated `.runAndSet(...)`, `.doIfNotSet(...)` and `.mapAndSet()` in **VolatileFlag**
- Deprecated `.get` in **LazyFuture** in favor of `.value`, reflecting similar changes in past releases
### New Features
- It is now possible to write change listeners that detach from the change event source automatically. 
  Simply return `DetachmentChoice.detach`, `DetachmentChoice(shouldContinue = false)` or `false` in an 
  `.onChange(ChangeEvent) ` function implementation to detach from the event source.
- Added **PostponingProcess** class that behaves somewhat like **DelayedProcess**, but accepts a variable wait target
- Added **ValueConvertibleFileContainer** and **ValueConvertibleOptionFileContainer** -classes
  - These are best utilized when combined with **ValueConversions** and **ValueUnwraps**
- Added **Flag** and **ResettableFlag** -traits and implementations
- Added **OptionsIterator** and **ZipPadIterator** classes
- Added change event support for iterators
  - See **IteratorWithEvents** and `.withEvents(...)` in **CollectionExtensions**
### New Methods
- **ChangeEvent**
  - Added `.toPair`
  - Added `.toStringWith(A => String)`
  - Added a new constructor variant that accepts a **Pair**
- **ChangingLike**
  - Added `.flatMap(...)` that is a map function that supports functions that yield changing items
  - Added `.nextFutureWhere(...)` which works like `.futureWhere(...)`, except that it can't be triggered  
    by the current value
- **Either** (**CollectionExtensions**)
  - Added new functions for eithers that contain items of the same type on both sides
- **Future** (**AsyncExtensions**)
  - Added `.waitWith(AnyRef, Duration)` that works like an interruptible `.waitFor()`
  - Added `.currentSuccess` and `.currentFailure` to **Futures** which contain instances of **Try**
- **Iterable** (**CollectionExtensions**)
  - Added `.zipPad(...)` functions, which behave like `.zip(...)` but pad the shorter collection where necessary
- **Iterator** (**CollectionExtensions**)
  - Added `.zipPad(...)` functions (see **Iterable**)
- **Option** (**CollectionExtensions**)
  - Added `.mergeWith(Option)`
- **Pair**
  - Object
    - Added `.fill(...)` that calls a call-by-name parameter twice
  - Instance
    - Added `.isSymmetric`, `.isNotSymmetric` and `.compareWith(...)`
    - Added a `.zip(Pair)` that returns a **Pair**
- **Path** (**FileExtensions**)
  - Added `.parts` and `.partsIterator` and `.length`
  - Added `.allDirectoriesIterator` and `.allSubDirectoriesIterator`
  - Added `.relativeTo(Path)`
  - Added `.take(Int)`, `.takeRight(Int)`, `.drop(Int)` and `.dropRight(Int)`
- **Process**
  - Added a protected `.markAsInterrupted()` -function that acts as a `.stop()`, but only alters the process' state
- **Signed**
  - Added `.ifPositive` and `.ifNegative`
- **Value**
  - Added a new variant of `.apply(...)`
- **Wait** (object)
  - Added `.untilNotifiedWith(AnyRef)`, a variant of `.apply(...)`
- **XmlReader** (object)
  - Added `.parseString(String)`
### Other Changes
- Added proper handling for **InterruptedExceptions** in **Wait**
  - **Wait** now properly breaks (stops) when it encounters an **InterruptedException**
  - `Wait.apply(...)` now returns a boolean that indicates whether the wait was forcibly interrupted or not
  - Modified the following classes to support interruptions, also:
    - **DelayedProcess**
    - **LoopingProcess**
    - **TimedTasks**
    - **ExpiringCache**
    - **ExpiringLazy**
    - **DelayedView**
    - **KeptOpenWriter**
  - **Future**`.raceWith(Future)` may now yield a failing future in case the process is interrupted with an 
    **InterruptedException**
- **XmlElement**`.toXml` now wraps element content in CDATA if there exist any unaccepted characters within the 
  element's contents
- Rewrote **Value**`.castTo(DataType, DataType)` so that it will cast to the closer data type
- **Volatile**`.value` is no longer synchronized. For synchronized access, use `.synchronizedValue`
- **VolatileFlag**.`set()` and `.reset()` now return booleans that indicate whether the flag state was actually modified
- **PollingIterator** is now type covariant
- **Process**`.registerToStopOnceJVMCloses()` ignores the call if it has uses **ShutdownReaction** of **Cancel**
  - As a consequence, this function is no longer deprecated in **LoopingProcess**

## v1.16 - 18.08.2022
This update adds a number of new collection functions, and even new collection types. 
Xml handling is also updated to support namespaces. Various bugfixes, optimizations and quality-of-life updates 
are also included. This update also adds a new logging system.

Due to the changes in Xml and trees, and because of the new logging system, this update requires a number of changes 
from the implementing clients, especially those utilizing the xml-related features.
### Breaking Changes
- **String** to **Value** conversion now returns an empty value for empty strings
- The following classes now use **Logger**:
  - **AsyncMirror** and **ChangingLike**`.mapAsync` -variants
  - All **FileContainer** variants
  - **Iterator**`.mapCatching(...)` (**CollectionExtensions**)
  - **Process**, **DelayedProcess**, **LoopingProcess**, **Delay** and **Loop** functions
  - **ThreadPool** and **NewThreadExecutionContext**
  - **TimedTasks**
- **XmlElement** now uses namespaces, which causes a number of breaking errors and changes
- **XmlElement**.text now returns a **String**, not an **Option**
- Altered the **Model** class in following breaking ways:
  - The `new` constructor is now private
  - Removed constructors that took a key and a value parameter
  - Altered the tuple constructor variant and renamed it to `.from(...)`
  - `--` now accepts a collection of keys, not constants
    - The previous implementation is available as `.withoutAttributes(...)`
- **Tree** implementations now require an implicit **EqualsFunction** parameter
- **TreeLike** now requires a new function `.containsDirect(...)` in order to support custom equality testing
- Moved **Equatable** to `utopia.flow.operator`
- **Iterable**`.bestMatch(...)` may cause a build error, due to new function variants
- Moved **Equatable** to the `operator` -package
- **JsonConvertible** now requires implementation of `appendToJson(StringBuilder): Unit`
  - Please notice that all existing implementations have been modified to include this function, so this should be 
    a problem only in custom implementations, and even then a minor one
- **ModelConvertible** is now a sub-trait of **ValueConvertible**
  - May cause some minor build errors in cases where your classes inherited both
### Deprecations
- Deprecated some `...andGet(...)` -functions from **Volatile** classes, because of changes made to the base function 
  versions
### New Features
- Added **Logger** trait and 2 basic implementations (**SysErrLogger** and **FileLogger**)
- Added **FromValueFactory** -trait that also provides implicit **Value** unwraps
- Added **LazyVector**, **LazyIterable** and **CachingIterable** classes for lazy iteration
- Added **Pair**, **FiniteDuration** and **Days** as new data types to the generic value system
- PropertyDeclarations and model validating now support alternative property names
- Added the **FoldingIterator** class
- Added **CompoundingVectorBuilder** class that allows one to check the current vector state while building
- Added **MappingCacheView** and **KeyMappingCache** classes, corresponding with new `.mapValuesView(...)` and 
  `.mapKeys(...)` -functions in **CacheLike**
- Added **EqualsFunction**, **ApproximatelyEquatable**, **EqualsExtensions** and **ApproximatelyZeroable** for custom 
  equality testing
- Added **ScopeUsable** trait (from **Reflection**)
- Added to- & from- model conversion to **DateRange**
- Added **ObjectMapFileContainer** class
- Added **FromModelFactoryWithDefault** -trait
### New Methods
- **Constant**
  - Added `.mapValue(...)`
- **Duration** (**TimeExtensions**)
  - Added `.isInfinite`
- **GraphNode**
  - Added a number of new cheapest route -functions
- **Iterable** (**CollectionExtensions**)
  - Added `.areAllEqual: Boolean`
  - Added `.maxGroupBy(...)`
  - Added `.containsEqual(...)`
  - Added new `.bestMatch(...)` variants
- **IterableOnce** (**CollectionExtensions**)
  - Added `.lazyMap(...)` and .`lazyFlatMap(...)` which yield a **LazyIterable**, 
    as well as `.caching` which yields a **CachingIterable**
  - Added `.foldLeftIterator(...)` and `.reduceLeftIterator(...)` functions
  - Added `.forNone(...)`, which is same as `.forAll(...)` with inverted check function
  - Added a number of utility methods for iterables of tries
- **LocalDate** (**TimeExtensions**)
  - Added `.monthOfYear: Int`
- **Map** (**CollectionExtensions**)
  - Added `.mapKeys(...)`
- **Model**
  - Added `.map(...)`, `.mapKeys(...)` and `.mapValues(...)`
- **Path** (**FileExtensions**)
  - Added `.commonParentWith(...)`
  - Added `.deleteContents()` -which deletes all directory contents
- **Property**
  - Added `.isEmpty` and `.nonEmpty`
- **Regex**
  - Added `.replaceAll(String, String)`
- **String** (**StringExtensions**)
  - Added `.nonEmptyOrElse(=> String)` and `.mapIfNotEmpty(...)` to help to work around possibly empty strings
  - Added `.filterWith(Regex)` and `.filterNotWith(Regex)`
  - Added `.replaceAll(Regex, => String)`
- **TreeLike**
  - Added `.apply(...)` which acts like / but supports multiple parameters (i.e. a path)
- **Value**
  - Added `.tryVectorWith(...)`, `.tryPairWith(...)` and `.tryTupleWith(...)(...)`
### Bugfixes
- **TreeLike**`.nodesBelowIterator` now works as expected. The previous version (in v1.15) yielded an empty iterator, 
  causing problems in multiple dependent functions
- **Volatile** classes now fire change events only **after** unlocking themselves. 
  The previous implementation led to deadlocks in reactive systems (such as **Reflection**)
- **Version** finding didn't work for version numbers that started with a number larger than 9. Fixed.
- **Path**`.parent` (**FileExtensions**) now works for a relative empty path, also
### Other Changes
- Optimized json conversion (see breaking changes)
- `.divideBy(...)` in **IterableOnce** through **CollectionExtensions** now returns collections based on the implicit 
  builder, not always **Vector** types
- **Pair** now extends **IndexedSeq** and **IndexedSeqOps**
- **Regex**`.replaceAll` now accepts the replacement parameter as call-by-name
- **FileContainer** saving now utilizes shutdown hooks to complete the save even on jvm exit
- **Iterable**`.containsAll(...)` (via **CollectionExtensions**) now accepts an implicit **EqualsFunction** parameter
- Optimized `GraphNode.cheapestRoutesTo(...)`
- Optimized **XmlElement**`.toXml`
- Changed `.toString` implementation in **DateRange**
- **SimpleConstantGenerator** may now be passed as an object to apply the default version

## v1.15 - 06.06.2022
This major update introduces a number of utility changes, including many breaking changes. 
The biggest changes target the waiting and looping classes, which were completely rewritten in order to support 
close hooks / jvm close events.

The added SHA256 utility hasher object is also worth mentioning.
### Breaking Changes
- **Loop** is now an abstract class instead of a trait. This is because the trait contains attributes.
- Renamed `makeNode` in **TreeLike** (template) to `newNode`
- Added an abstract `createCopy(...)` function to **TreeLike** (immutable) in order to separate between copy and create 
  use-cases
- **CsvReader** now accepts a separator of type **Regex** instead of **String**. 
  Quotation ignoring is removed for non-default separators, as it should be present within the separator itself.
- `.divide(String)` in **Regex** now returns `Vector[Either[String, String]]` instead of `Vector[String]`
  - The previous implementation is now named `.separate(String)`
- **PollableOnce** construction parameter is now call-by-name
### Deprecations
- Deprecated most **WaitUtils** functions in favor of **Wait** and **Delay**
- Deprecated **Loop**, **TryLoop**, **DailyTask** and **WeeklyTask** in favor of new 
  **LoopingProcess** class and new **Loop** functions
- Deprecated **SynchronizedLoops** in favor of **TimedTasks**
- Deprecated **SingleWait** in favor of **Wait**
- Deprecated the **Async** object in favor of the new process classes
- Deprecated **VolatileFlag**`.notSet` in favor of `.isNotSet`
- Deprecated (immutable) **TreeLike**`.replace(...)` and `.findAndReplace(...)`
### New Features
- Added **Sha256Hasher** utility tool
- Created a new wait and loop system
  - See: **Process**, **LoopingProcess**, **DelayedProcess**, **Wait**, **Delay** and **Loop**
- Added two new wait target types: **DailyTime** and **WeeklyTime**
- Created **TimedTasks**, which is an improved implementation of **SynchronizedLoops**
- Added **DeprecatingLazy** class, which is a **Lazy** implementation that may renew the value based on a condition
- Added **SureFromModelFactory** trait
- Added state tracking to **CloseHook**
- Added **PairingIterator** class
### New Methods
- **CloseHook**
  - Added `-=` for removing hooks
- **Iterator** (**CollectionExtensions**)
  - Added `.paired` and `.pairedFrom(...)`
  - Added `.foreachCatching(...)`
- **Lazy** (object)
  - Added `.wrap(...)`, which calls `LazyWrapper.apply(...)`
- **LazyLike**
  - Added `.iterator`, which always returns a lazy single-item iterator
- **Loop**
  - Added `.startAsyncAfter(FiniteDuration)`, a delayed version of `.startAsync()`
  - Added `.isRunning`
- **Month** (**TimeExtensions**)
  - Added `.value`, which is an alias for `.getValue`
- **Regex**
  - Added `.splitIteratorIn(String)` for lazy splitting
- **ResettableLazy**
  - Added `.filter(...)` and `.filterNot(...)`, which are kind of conditional resets
- **Value**
  - Added `.nonEmpty`, which has the same function as `.isDefined`
- **VolatileFlag** (object)
  - Added an `.apply()` function, like in other **Volatile** classes
- **Year** (**TimeExtensions**)
  - Added `.value`, which is an alias for `.getValue`
- Added a number of new methods to **TreeLike** and **XmlElement**
### Other Changes
- **WaitTarget**s can now be constructed implicitly from **Instant**, **Duration**, and **LocalTime**
- Changed **Map**`.mergedWith(Map, ...)` to `.mergeWith(Map)(...)` (in **CollectionExtensions**)
  - Previous version was deprecated
- Type parameter NodeType in **TreeLike** (template) is now covariant
- `tryForeach` (in **CollectionExtensions**) now accepts a wider range of function result types

## v1.14.1 - 27.01.2022
This update introduces a number of quality-of-life improvements, and also some important fixes to json writing and 
regular expressions.
### Scala
This module now uses Scala v2.13.7
### Deprecations
- Replaced `.dividedWith(...)` in **CollectionExtensions** by `.divideWith(...)` 
  - The former is still available, but deprecated
### New Features
- Added **DeepMap** data structure, which resembles a map containing other maps and uses paths as keys
- **ModelDeclaration** now supports child model declarations
- Added **ConditionalLazy**, which is a variant of **Lazy** that only caches the value if it fulfills a condition
- Added **Quarter** (1/4th of a year) enumeration, which is also supported by **TimeExtensions**
### New Methods
- **Instant** (**TimeExtensions**)
  - Added `.toLocalDate` and `.toLocalTime`
- **IterableOnce** (**CollectionExtensions**)
  - Added `.flatDivideWith(...)` & `splitFlatMap(...)`
- **Iterator** (**CollectionExtensions**)
  - Added `+:` (prepend one)
- **String** (**StringExtensions**)
  - Added `!~==` - case-insensitive inequality operator
- **UncertainBoolean**
  - Added `.mayBeFalse` and `.mayBeTrue`
### Bugfixes
- `||` -method in **Regex** combined some regular expressions incorrectly. The current version is more careful.
### Other Changes
- **Value**`.toJson` now escapes newline characters and uses `\"` instead of `'` as quotation replacement
- **Instant** now gains **SelfComparable** instead of **RichComparable** when importing **TimeExtensions**

## v1.14 - 04.11.2021
Beside the obviously breaking change of removing generic type parameters from the immutable **Model**, 
this update mainly adds utility functions and features.
### Breaking Changes
- Immutable model no longer accepts type parameters
- `Regex.word` now matches whole words as described in the documentation. 
  Added `Regex.wordCharacter` to replace the previous (undesired) effect.
### New Features
- Added **RefreshingLazy** that works a little like **ExpiringLazy**, expect that resets are synchronous and 
  performed at value reads - A more useful implementation if the item is intended to be cached for most of the time
- Added **MultiMapBuilder** which makes it easier to build maps which contain vectors as values
- Added **MutatingOnce** which provides one-time access to a value, mutating it after the first access
### New Methods
- **CombinedOrdering**.type
  - Added a new utility constructor
- **Model**
  - Added a couple variants of `.without(...)` which removes multiple attributes based on their names
- **Range** (**CollectionExtensions**)
  - Added `.exclusiveEnd`
- **Regex**
  - type
    - Added `.noneOf(String)` which is the inverse of `.anyOf(String)`
  - instance
    - Added `.firstRangeFrom(String)`
    - Added `.ignoringWithin(Char, Char)`
- **StdIn** (**ConsoleExtensions**)
  - Added `.readValidOrEmpty(...)`, which interacts with the user in order to find a valid value
### Other Changes
- Added a double-checking option to `StdIn.readNonEmptyLine(...)` (**ConsoleExtensions**)
- `Regex.newLine` is now `\R` and not `\n`, capturing a wider range of newline characters
- Refactored some **Regex** functions
- Optimized **PollingIterator** mapping

## v1.13 - 18.10.2021
Updated built-in value conversions (concerning Vector and LocalDate types). Added interactive console support with 
the new **Console** class and **ConsoleExtensions** object, as well as by working on the command arguments -system.
### Breaking Changes
- Instant to Long conversion now converts to milliseconds instead of seconds. The same is true the other way around.
- String to Vector conversion now first attempts to find either arrays `[a, b, c]` or tuples `(d, e, f)` 
  or parts separated by `,` or `;`, and if neither of those applies, wraps the value in an array like before.
- The third parameter in `ArgumentSchema.flag(...)` is now `hint: String` and not `defaultValue: Boolean`.
  - This is quite unlikely to require major refactoring, as the usual use case for the default value is
    to specify it as a named parameter
### Deprecations
- Deprecated **AutoCloseWrapper**`.get` in favor of `.wrapped`
### New Features
- Added multiple console interaction features (**Console** and **Command** classes)
### New Methods
- **Regex**
  - Added `.ignoringQuotations`
- **VolatileFlag**
  - Added `.getAndReset()`
### Other Changes
- Value casting from String to LocalDate now accepts format dd.MM.yyyy also
- **AutoCloseWrapper** now extends **Extender**, allowing implicit access to its contents

## v1.12.1 - 04.10.2021
This small update adds file editing through **CollectionExtensions**.
Many collection-related utility additions are also included.
### Deprecations
- Deprecated **PollingIterator**'s `.takeNextWhile(...)` in favor of `.collectWhile(...)`
### New Features
- Added **Version** class for version number handling
### New Methods
- **Either** (**CollectionExtensions**)
  - Added `.rightOrMap(...)` and `.leftOrMap(...)` utility functions
- **Path** (**FileExtensions**)
  - Added `.edit(...)` and `.editToCopy(...)(...)` -methods for text/line -based file editing
- **PollingIterator**
  - Added `.foreachWhile(...)(...)` and `.foreachUntil(...)(...)`
  - Added `.collectWhile(...)` and `.collectUntil(...)`
### Other Changes
- Added a custom `.toString` implementation to **Today**

## v1.12 - 3.10.2021
This major update adds a number of new traits (based on previous **Genesis** versions) for classes which support 
certain operators (*, +). New **Pair** data structure is also added and taken into use where applicable. 
There are also a large number of new utility methods added.
### Breaking Changes
- **Iterator**`.takeNextTo(...)` and `.takeNext(Int)` (**CollectionExtensions**) now return an iterator and not a vector, 
  and don't advance the iterator immediately. The previous implementations are available as `.collectTo(...)` and 
  `.collectNext(Int)`
- **Seq**`.paired` now returns **Pair**s instead of tuples. This may require refactoring when used with pattern matching.
### Deprecations
- Deprecated **Iterable**`.tryForEach(...)` in favor of **IterableOnce**`.tryForeach(...)` (**CollectionExtensions**)
- Deprecated **Path**`.forChildren(...)` and `.foldChildren(...)` in favor of the new iteration functions 
  (**FileExtensions**)
### New Features
- Added **Pair**, a data structure that holds exactly two values of the same type
- Added **PollableOnce**, a container that returns its value once before becoming invalid
- Added **RecursiveDirectoryIterator**, which is also used by some refactored **FileExtensions** functions
- Added **Sign** class to represent binary direction (+ or -). This closely resembles 
  **Direction1D** from **Utopia Genesis**
- Added following new operator -related traits that enable easier creation and handling of scalable and combinable 
  data types
  - **LinearMeasurable** - Provides `.length` -property, which is used in **Utopia Genesis**
  - **Reversible** - Supports the unary - operator
  - **Zeroable** - Recognizes between zero and non-zero instances
  - **Signed** - Recognizes between positive and negative and can swap between them
  - **SignedOrZero** - Adds zero handling to Signed
  - **BinarySigned** - Special case for signed items that can never be zero
  - **Scalable** - Allows multiplication
  - **LinearScalable** - Allows linear multiplication
  - **Combinable** - Supports the + operator (and possibly -)
  - **Multiplicable** - Supports * operator for integers but not doubles
  - **DoubleLike** - Performs like a double number
  - **IntLike** - Performs like an integer number (doesn't support /)
### New Methods
- **Graph**
  - `.edgesTo(N)`
  - `.withoutEdge(E)`
- **GraphNode**
  - `.cheapestRoutesTo(...)(...)` and `.shortestRoutesTo(...)`, which return multiple routes in case of equal costs
- **IterableOnce** (**CollectionExtensions**)
  - `.takeTo(...)`, which works like `.takeWhile(...)`, except that it includes the terminating item
  - `.tryForeach(...)`, based on` .TryForEach(...)` in **Iterable**
  - `.tryFlatMap(...)`, which combines the ideas of `.tryMap(...)` and `.flatMap(...)`
- **Iterator** (**CollectionExtensions**)
  - Added `.existsCount(...)`
  - Added `.skip(Int)`
- **Model**
  - Added `.apply(String, String, String*)` that works like standard `.apply(String)`, except that it uses 
    alternative options if primary search fails.
- **Path** (**FileExtensions**)
  - Added `.iterateChildren(...)`, `.tryIterateChildren(...)`, `.allChildrenIterator` and `.iterateSiblings(...)` 
    for easier and more memory-friendly child path iterations
  - Added `.unique`, which makes sure a non-existing path is used
- **PollingIterator**
  - Added `.pollToNextWhere(...)`, which enables one to make sure the next item will fulfill a specific requirement
- **Regex**
  - Added multiple new iterator-based methods
- **ResettableLazyLike**
  - Added `.popCurrent()`, which is a variation of `.pop()` that doesn't generate a value if one didn't exist already.
- **String** (**StringExtensions**)
  - Added `.containsMany(String, Int)`, which checks whether the string contains multiple occurrences of a sub-string
### Other Changes
- **Path**`.withFileName(String)` now applies the existing file extension if no new extension is provided in the 
  file name parameter (**FileExtensions**)
- `TryLoop.attempt(...)` now has a special handling for cases where the maximum number of allowed attempts is 1 or lower

## v1.11.1 - 9.9.2021
This small patch resolves a bug in **TimeExtensions** where two dates couldn't be subtracted from each other 
(since v1.10).
### Bugfixes
- **LocalDate** (**TimeExtensions**)
  - `-(other: LocalDate)` no longer throws (was dependent on `Duration.between(...)`, 
  which can't handle LocalDates, apparently)

## v1.11 - 4.9.2021
This update fixes some issues that you may have faced when dealing with v1.10 changes 
(cache classes & the **Period** class). 
This update also contains a range of small utility additions.
### Breaking Changes
- Added an abstract `.cachedValues` property to **CacheLike** trait to support certain use cases 
  which were available with the previous cache versions but not with v1.10.
### New Features
- Added **MapLike** trait that only specifies an `.apply(K)` method. 
  This is to support wider range of input options in some functions (currently only used in the **Ambassador** module).
  - **Model** and all **CacheLike** instances extend this trait.
  - Instances of **MapLike** can also be created simply by passing a single-input function.
### New Methods
- **Path** (**FileExtensions**)
  - Added `.writeUsing(PrintWriter => U)`, a modified version of `.writeWith(BufferedOutputStream => U)`
- **Period** (**TimeExtensions**)
  - Added `.toApproximateDuration` for converting a **Period** to a **FiniteDuration** (not exact)
- **Regex**
  - Companion Object
    - Added `.lowerCaseLetter` and `.upperCaseLetter` properties
  - Class
    - Added `.rangesFrom(String)`
- **String** (**StringExtensions**)
  - `.quoted`, which returns the string within double quotes
  - `.uncapitalize`, which returns the string with the first character in lower case
### Other Changes
- **CombinedOrdering** now accepts more generic **Ordering** parameters

## v1.10 - 13.7.2021
This release provides you with a completely new set of **Lazy** containers to be used for caching.  
There's also an update on time related classes, making durations more reliable.  
This update also includes a number of new collection and **Promise** extensions.
### Breaking Changes
- Major refactoring in **Cache** classes
  - Rewrote **ExpiringCache** and **ReleasingCache**
  - Modified `TryCache.apply(...)` and `TryCache.releasing(...)` and removed `TryCache.expiring(...)`
  - Removed multiple methods from **AsyncCache**.type
- Multiple functions in **TimeExtensions** now require a **FiniteDuration** instead of just **Duration**, 
  because they would break with infinite duration values
- Removed implicit conversion from **Period** to **Duration**, 
  added a new **Days** class to cover for that use case
- Multiple **TimeExtensions** methods that would return or accept a **Period** now use **Days**
- `ChangingLike.lazyMap(...)` and `ChangingLike.lazyMergeWith(...)`
  now return **ListenableLazyLike** instead of just **LazyLike**
  - This allows one to continue chaining map functions after these method calls also
### Deprecations
- Deprecated **SingleCacheLike** trait and implementations in favor of various **LazyLike** implementations
  - **ClearableSingleCacheLike** is replaced with **ResettableLazyLike**
  - **ExpiringSingleCacheLike** is replaced with **ExpiringLazy**
- Deprecated **MultiCacheLike** trait and its sub-traits and implementations in favor of new 
  **MultiLazyLike** trait and its subclasses 
### New Features
- Added a new **Days** class to represent time on date level. This works as a smooth bridge between 
  **FiniteDuration**, which is more precise, and **Period**, which is unreliable in most contexts 
  because of the months and years -parameters.
- Added new **LazyLike** implementations, **ListenableLazy** and **ListenableResettableLazy** 
  which provide access to value generation and value change events
- Added **ExpiringLazy** class that automatically resets its contents after a specific time period
  - This class replaces **ExpiringSingleCache** class, which is now deprecated
- Added **WeakLazy** class that only holds a weak reference to the generated items
- Added **MultiLazyLike** and **ExpiringMultiLazyLike** traits and implementations that allow one to create 
  custom caches using various instances of **LazyLike**
- Added utility classes (**CommandArgumentSchema**, **CommandArgumentsSchema** and **CommandArguments**) 
  for application and console command argument processing.
### New Methods
- Added multiple new methods to **Instant** and **Duration**/**FiniteDuration** through **TimeExtensions**
- **Future** (**AsyncExtensions**)
  - Added `.foreachResult(...)` to **Future**s that contain a **Try**
- immutable.**Model**
  - `+:(Constant)` that adds a new property to the beginning of this model
- **Settable**
  - Multiple new utility methods
  - New utility methods for **Settable**s containing **Option**s and **Vector**s
- **Throwable** (**ErrorExtensions**)
  - `.stackTraceString`
- **Try[Future[Try[...]]]** (**AsyncExtensions**)
  - Added .flattenToFuture that converts this **Try** to a **Future[Try[A]]**
- **Value**
  - Added `.tryString`, `tryInt`, `tryDouble` etc. methods that return a **Try**
### Other Changes
- Added direct implicit conversion from **Today** to **ExtendedLocalDate**, allowing access to **TimeExtensions** 
  methods directly from **Today** without going through **LocalDate** first
- `WaitUtils.waitUntil(Instant, AnyRef)` now has a default value for the second parameter (lock)

## v1.9.1 - 12.5.2021
This adds new collection utility methods, especially on **IterableOnce** and **TreeLike** traits.
### New Methods
- **IterableOnce** (**CollectionExtensions**)
  - `.existsCount(Int)(...)` which is a type of combination of `.exists(...)` and `.count(...)`
  - `.takeTo(...)` - Works somewhat like `.takeWhile(...)`, but will include the terminating item
  - `.takeNextTo(...)` which works much like the previously mentioned method, except that this one 
    advances the iterator instead of forming a new iterator
- **TreeLike**
  - Template (all)
    - Multiple new iterator methods
    - `.findBranches(...)`
  - Immutable
    - `.withoutChildren`

## v1.9 - 17.4.2021
In this update Flow got a number of new utility updates, including a range of new classes. 
Various extensions also were expanded to contain a wider range of utility methods.
### Breaking Changes
- Moved time-related classes / objects from utopia.flow.util to utopia.flow.time 
- Changed `.setIfEmpty` method variations in **VolatileOption** to accept a *call by name* parameter 
  instead of a function
- **XmlReader** and **XmlWriter** objects now support **Path** instead of **File**
- Deprecated **PointerLike** trait and replaced it with **Settable** trait in all implementations
- Renamed **Lazy** to **MutableLazy** and **VolatileLazy** to **MutableVolatileLazy**
    - Also, Lazy class parameter is now a private call by name parameter and no longer a public function with parameters
- Replaced **Changing** with **ChangingLike** in most places
  - Also, replaced `Changing.wrap(...)` with `Fixed(...)`
- Added dependency support to **ChangingLike**, which requires some code changes in 
  implementations but allows more reliable value mirroring
- `GraphNode.cheapestRouteTo(...)` now accepts the cost function in a separate parameter list
### Deprecations
- Deprecated `LazyLike.get`. Lazy instances now behave more like pointers, extending the new **Viewable** trait.
- Deprecated `Volatile.get` in favor of `.value`
### New Features
- Added a delaying / buffering pointer view class (**DelayedView**)
- Added asynchronous mapping support for pointers via **AsyncMirror** class
- Added **LazyMirror** class and `.lazyMergeWith(...)` to **Changing**
- Added **Viewable** and **Settable** traits that allow more generic pointer handling
    - Also, added **View** for **Viewable** wrapping
- Added **Lazy**, **ResettableLazy**, **VolatileLazy** and **ResettableVolatileLazy** classes to support 
  situations where mutability in a lazy container is not required nor desirable
- Added **WeakCache**
- `XmlWriter.writeFile(...)` and `.writeElementToFile(...)` now create the target directory if 
  it didn't exist yet
- Added date range support (**DateRange**, **YearlyDateRange**) which includes a number of new 
  methods in **TimeExtensions**
- Added **Now** and **Today** objects to be used instead of `Instant.now()` and `LocalDate.now()`
- Added **Regex** class (from **Utopia Reflection** module)
- Added **UncertainBoolean**, which is a utility class representing a boolean option (true, false or unknown)
- Added polling support for **Iterator** (use **CollectionExtensions** and `.pollable`)
- Added **ActionBuffer** class which performs a group action whenever a buffer is filled
### New Methods
- **AsyncExtensions**
  - **Future**
    - `.notCompletedBefore(Future)`
    - `.currentSuccess`
- **CollectionExtensions**
  - **Iterator**
    - `.pollable`
    - `.last` & `.lastOption`
    - `.nextWhere(...)`
    - `.foreachGroup(Int)(...)`
    - `.groupBy(...)`
  - **Range.Inclusive**
    - `.subRangeIterator(Int)`
  - **Seq**
    - `.takeRightWhile(...)`
    - `.maxIndexBy(...)`, `minIndexBy(...)`, `maxOptionIndexBy(...)` and `minOptionIndexBy(...)`
- **FileExtensions**
  - **Path**
    - Multiple new file write and append methods
- **StringExtensions**
  - **String**
    - `.slice(Range)` & `.cut(Range)`
    - `.stripControlCharacters`
- **TimeExtensions**
  - **Duration**
    - `.toPreciseMinutes`, `.toPreciseHours`, `.toPreciseDays` and `.toPreciseWeeks`
- **ChangeEvent**
  - `.compareBy(...)`, `.differentBy(...)` and `.compareWith(...)`
  - `.merge(...)` and `.mergeBy(...)`
- **ChangeListener**.type
  - `.onAnyChange(...)`
- **Changing**
  - `.delayedBy(Duration)`
  - `.mapAsync(...)`, `.tryMapAsync(...)`, `.mapAsyncCatching(...)` and `.mapAsyncMerging(...)`
- **TryCache**.type
  - `.releasing(...)`
- **Volatile**
  - `.getAndUpdate(...)`
- **VolatileList**
  - `.popAll()`
- **WeekDay**.type
  - `.current()`
- **XmlElement**
  - Object
    - `.build(...)`
  - Class
    - `.toSimpleModel`, which allows somewhat heuristic xml to json conversion
    - Other utility methods
### Fixes
- Fixed a bug in `LocalDate.next(WeekDay)` and `LocalDate.previous(WeekDay)` (returned same day even when 
includeSelf-parameter was set to false)
- Fixed a bug in **Period** comparison (didn't compare day value)
- Fixed a bug in `WeekDay.forIndex(Int)` (didn't accept 0 or negative values)
- Fixed a bug in **XmlWriter** where attributes were written in lowercase letters
### Other Changes
- Exceptions thrown by `Loop.runOnce()` implementations are now caught and printed, not propagated further.
- **CsvReader** now removes '-characters from the beginnings of columns, if present
    - Also, quotation marks are removed from around the values
- Added `ignoreEmptyStringValues: Boolean` -parameter to `CsvReader.iterateLinesIn(...)` and `.foreachLine(...)` 
- Changed `Duration.description` behavior when dealing with over 72 hour durations
- Added implicit conversion from **Period** to **Duration** (activated with `import utopia.flow.time.TimeExtensions._`)

## v1.8
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- CollectionExtensions: 
    - toMultiMap(f1, f2) and toMultiMap() were replaced with toMultiMap(f1)(f2), toMultiMap(...) and asMultiMap
        - Use of asMultiMap is discouraged since it currently only supports tuple vectors
        - New toMultiMap methods support all IterableOnce instances
- Generator
    - .apply method in Generator object now takes two parameter lists instead of one
- Sub-classes of trait Changing now need to specify their listener lists separately    
### Deprecations
- FromModelFactory.fromJSON was deprecated in favor of new .fromJson method that takes an implicit 
JsonParser as a parameter.
### New Features
- ValueUnwraps -extensions added to utopia.flow.generic -package. By importing these extensions, you 
can automatically unwrap values to most basic types (saves you from writing value.string or value.getInt etc.)
- Mirror and MergeMirror classes added for supporting mapping change listening
    - Added map and merge methods to Changing respectively
- Added .view -property to PointerWithEvents, which allows one to provide a read-only interface to such pointers
- Added a WeekDay enumeration
    - LocalDate.weekDay now returns an instance of this enumeration when TimeExtensions has been imported
- Added WeeklyTask trait & wrapper for creating loops that run a task once a week
    - Best use together with SynchronizedLoops
- Added TimeLogger class for performance testing
### Fixes
- tryMap(...) in CollectionExtensions now uses buildFrom and not Factory. This should result in better result 
collection types when used.
- LocalDate is now properly converted to JSON (previously converted to Instant first)
- Fixed TreeLike.filterWithPaths
- Fixed a logic error in ModelDeclaration.validate(Model)
- Fixed .append(...) method in FileExtensions (now properly creates the target file if it doesn't exist)
### New Methods
- CollectionExtensions
    - Added .slice(Range) to SeqOps
    - Added .getOrMap(...) to Try
    - Added a couple of new .divided methods
    - Added .containsAll to Iterable
- FileExtensions: Added .toJson, .contains(String), .containsRecursive(String), writeFromReader(...) and 
writeStream(...)
- TimeExtensions
    - Added .next(WeekDay, Boolean) to LocalDate
    - Added multiple new methods for LocalTime as well
- Added CsvReader class
### Other Changes
- JsonParser trait added, which JSONReader now implements. The purpose of this change is to 
offer a way to use alternative parsers when necessary (see BunnyMunch module for one 
alternative implementation)
- ModelDeclaration.validate now makes sure the required values are non-empty. This applies for 
String, Vector and Model type value requirements.
- Instant now has <= and >= methods through TimeExtensions
- Moved .findMap(...) from Iterable to IterableOnce in CollectionExtensions

## v1.7

### Major Changes
- Module is now based on **Scala 2.13.1** and not 2.12.18
### Breaking Changes
- Loop.runOnce and Loop.nextWaitTarget are now public and not protected. This will cause compile errors in sub-classes.
### New Features
- Added **Graph** for creating and using immutable graphs (previously there was only a mutable node 
implementation)
- **SynchronizedLoops** - Added for running multiple loops in a single background thread
- **DailyTask** - A new implementation of **Loop** that is run daily at a specific time
- Period comparison (inexact) (and some other new methods) is now available by importing **TimeExtensions**
### New Methods
- template.TreeLike
    - findWithPath(...)
    - filterWithPaths(...)
- CollectionExtensions
    - Seq
        - splitToSegments(Int)
    - Iterator
        - forNext(Int)(...)
