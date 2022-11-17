# Utopia Flow - List of Changes

## v2.0 (in development)
### Breaking Changes
- Reorganized the package structure
- **Changing** is now an abstract class **AbstractChanging** already containing `listeners` and `dependencies` -variables
  - **ChangingLike**, then was renamed to **Changing** instead
- Removed the implicit class for **Changing** of **Boolean**, moved these methods to **FlagLike** and
  added an implicit conversion, which is available through `import utopia.flow.view.template.eventful.FlatLike.wrap`
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
### Bugfixes
- Fixed **Regex**`.times(Range)` that previously yielded invalid regular expressions
### New Features
- **ModelDeclarations** now support optional properties
- Added **CachingMap**, **LazyTree** and **LazyInitIterator**
- Added **ViewGraphNode** as a lazily initialized graph
- Added **TimedTask** trait, which is now supported in **TimedTasks**, also
- Added new ways to write **LocalTime** values (after importing **TimeExtensions**)
- Added **SettableOnce**, which is like a **Promise** with **ChangeEvent**s
- Added **ReleasingPointer** class
- Added **Identity** object which functions as an identity function (i.e. `a => a`)
- Added **NoOpLogger** object
- Added **ApproxSelfEquals** trait
### New Methods
- **CanBeAboutZero**
  - Added `.notCloseZero`
- **CanBeZero**
  - Added `.nonZeroOrElse(...)` and `.mapIfNotZero(...)`
- **Iterable** (**CollectionExtensions**)
  - Added `.mapOrAppend(...)` and `.mergeOrAppend(...)` that either maps/merges an item into the collection, 
    or appends it.
- **Lazy**
  - Added `.map(...)` and `.flatMap(...)`
- **Path**
  - Added `.toTree`
- **Pair**
  - Added `.oppositeOf(...)` and `.oppositeToWhere(...)`
  - Added a number of new methods for pairs that contain collections (accessed implicitly)
- **PointerWithEvents** (type)
  - Added `.empty`
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
