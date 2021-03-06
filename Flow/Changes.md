# Utopia Flow - List of Changes

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