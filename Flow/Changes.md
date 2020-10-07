# Utopia Flow - List of Changes
## v1.9 (beta)
### Breaking Changes
- Changed setIfEmpty method variations in VolatileOption to accept a call by name parameter instead of a function
### New Features
- Added a delaying / buffering pointer view class (DelayedView)
- Added asynchronous mapping support for pointers via AsyncMirror class
### New Methods
- Added .notCompletingBefore(Future) to Future in AsyncExtensions
- Added .subRangeIterator(Int) to Range.Inclusive in CollectionExtensions
- Added .getAndUpdate(...) to Volatile
- Added .delayedBy(Duration) to Changing
- Added .mapAsync(...), .tryMapAsync(...), .mapAsyncCatching(...) and .mapAsyncMerging(...) to Changing
- Added .toPreciseMinutes, .toPreciseHours, .toPreciseDays and .toPreciseWeeks to Duration in TimeExtensions
- Added .current() to WeekDay
- Added .popAll() to VolatileList
### Fixes
- Fixed a bug in LocalDate.next(WeekDay) and LocalDate.previous(WeekDay) (returned same day even when 
includeSelf-parameter was set to false)
- Fixed a bug in period comparison (didn't compare day value)
- Fixed a bug in WeekDay.forIndex(Int) (didn't accept 0 or negative values)
### Other Changes
- Exceptions thrown by Loop.runOnce() implementations are now caught and printed, not propagated further.
- CsvReader now removes '-characters from the beginnings of columns, if present
- Changed Duration.description behavior when dealing with over 72 hour durations
- Added implicit conversion from Period to Duration (activated by importing TimeExtensions._)

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