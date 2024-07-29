# Utopia Flow
Flow provides the standard set-of-tools that are used in all the other Utopia modules.

## Main Features
These are the features you get when you use Utopia Flow

### Typeless data handling
[Value](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/generic/model/immutable/Value.scala) 
and [Model](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/generic/model/immutable/Model.scala) 
classes offer a way to handle data without type information, a little like in Python, 
for example, conveniently converting between various supported types like Double, Int, String and Vector.

This is very useful in interfaces to typeless and soft-typed systems and languages, such as MySQL, JSON and XML. 
If you have previous experience with Java in dealing with these interfaces, you will know the pain of 
having to convert all *Any* / *Object* types to appropriate data types, dealing with possible 
exceptions at every corner. With **Flow**, you can simply call `.toInt` etc., and the type conversion algorithm will 
work it's hardest to give you the desired result.

What's more, convertible types and conversion logic may be introduced in lower modules as well, 
meaning that you're not tied to just the basic implementation here in **Flow**. For an example on how to do this, 
please check the **Paradigm** module's 
[utopia.paradigm.generic package](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm/src/utopia/paradigm/generic).

### JSON, XML and CSV support & Integration with models and typeless values
- **Flow** offers full support for JSON and XML processing that fully utilizes the power of the typeless values
- Conversion between JSON and **Model** / **Value** is seamless and converts between supported types under the hood
- Please note that the current implementation of JSON parser prioritizes accessibility over performance and is not
  very efficient at this time. You may wish to use another parser for very large json files where performance
  becomes an issue.
  - There is a much more efficient implementation of json reading in 
  [BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch) module (**JsonBunny**). 
  It offers the same benefits as **JsonReader**, just much more efficiently. The difference is that 
  **JsonBunny** leverages [Jawn](https://github.com/typelevel/jawn) json parser while **JsonReader** is completely 
  independent.  
    - If you plan to use json parsing in any real (i.e. commercial) application, I'd highly suggest to use **JsonBunny** 
    instead of **JsonReader**.
- There is a very neat CSV file parser (
  [CsvReader](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/parse/file/CsvReader.scala)) 
  available, also

### Event-based pointers
- Both mutable and read-only pointer interfaces that support mapping, merging, etc.
  - A critical tool when creating reactive systems (such as GUIs)
- These include full support for value change events

### Various data structures
- Both mutable and immutable 
  [Tree](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/immutable/Tree.scala) 
  and [Graph](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/immutable/Graph.scala) data structures
- Support for various collection and **Iterator** classes, such as 
  [Pair](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/immutable/Pair.scala) and 
  [PollingIterator](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/mutable/iterator/PollingIterator.scala)
- A number of lazily initialized collection classes (**CachingSeq**, **LazySeq**, **LazyPair**, **CachingMap**, etc.)
- Mutable concurrent collections (**Volatile**, **VolatileFlag**, **VolatileList** and **VolatileOption**) 
  may be used in multithreaded environments where data is being modified and accessed from multiple threads.
- Support for weakly referenced lists
- A large number of additional functions to standard Scala collections via **CollectionExtensions**

### Tools for asynchronous programs
- [ThreadPool](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/async/context/ThreadPool.scala) 
  implementation for generating a scaling **ExecutionContext**
- [Process](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/async/process/Process.scala) 
  and [LoopingProcess](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/async/process/LoopingProcess.scala) 
  traits for looping, asynchronous operations, as well as support for shutdown hooks
- Blocking `.waitFor()` -simplification of **Future** waiting available through extension (**AsyncExtensions**)
- Various waiting -related tools
  - See [Wait](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/async/process/Wait.scala) 
    for more details

### Data caching & Local data containment
- [utopia.flow.view.immutable.caching package](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow/src/utopia/flow/view/immutable/caching) 
  contains various tools for caching individual or multiple pieces of 
  data either temporarily or permanently, asynchronously or synchronously
- [utopia.flow.parse.file.container package](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow/src/utopia/flow/parse/file/container) 
  contains numerous classes and traits for storing data locally in json files, 
  which is useful when you need to store small amounts of data and don't want to use a local database 
  (i.e. [Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault) and/or 
  [Trove](https://github.com/Mikkomario/Utopia-Scala/tree/master/Trove))
  
### Pleasant interface for time classes
- A Scala- and user-friendly interface to Java's time classes (**Instant**, **LocalDate**, **LocalTime** etc.) through 
  extension (**TimeExtensions**)
- A new set of time-related models such as **WeekDay**, **Days** and **DateRange**

### Easy-to-use interface for file interactions
- Additional easy-to-use functions for files (i.e. java.nio.file.Path instances) through extension (**FileExtensions**)

### String and regular expression -utilities
- The [Regex](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/parse/string/Regex.scala) 
  class offers a more beginner-friendly (and readable) way to build regular expressions, 
  which is useful for people like me who didn't grow up using them every day
- **StringExtensions** also offers a set of neat methods for easier String-handling

### Command line interface template
- [utopia.flow.util.console package](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow/src/utopia/flow/util/console) 
  contains various tools for command line argument handling
- Full console-based interface is also supported 
  (see [Console](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/util/console/Console.scala) class)

## Implementation Hints
Hints and advice to help you get started and to get most out of Utopia Flow

### What you should know before using Flow
When you wish to instantiate typeless values, please enable implicit value conversions by adding 
`import utopia.flow.generic.casting.ValueConversions._`

### Extensions you should be aware of
- [utopia.flow.generic.casting.ValueConversions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/generic/casting/ValueConversions.scala)
  - Implicit conversions from value-supported classes (Int, Double, String, Vector, etc.) to **Value**
  - Also, through importing 
    [utopia.flow.generic.casting.ValueUnwraps](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/generic/casting/ValueUnwraps.scala), 
    you gain implicit conversions from **Value** 
    back to these data types, which is useful in 
    [FromModelFactory](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/generic/factory/FromModelFactory.scala) -implementations, 
    for example.
- [utopia.flow.collection.CollectionExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/CollectionExtensions.scala)
    - Contains a large number of method additions for standard collection classes, such as 
    Iterator, Iterable, Seq, Option, Either and Map
- [utopia.flow.time.TimeExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/time/TimeExtensions.scala)
    - Functional additions to java.time classes
    - Conversion between java.time.Duration and scala.concurrent.duration.Duration
    - Easy creation of scala.concurrent.duration.FiniteDuration by using numeric extension 
      (E.g. by writing `2.minutes`)
- [utopia.flow.parse.file.FileExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/parse/file/FileExtensions.scala)
  - A number of new scala-friendly methods for java.nio.file.Path
- [utopia.flow.async.AsyncExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/async/AsyncExtensions.scala)
    - Utility updates to Future
- [utopia.flow.util.StringExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/util/StringExtensions.scala)
  - Utility methods for String
- [utopia.flow.operator.equality.EqualsExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/operator/equality/EqualsExtensions.scala)
  - Provides access to `~==` methods, which can be used for case-insensitive string comparisons, 
    approximate double number comparisons, etc.
- [utopia.flow.parse.AutoClose](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/parse/AutoClose.scala)
    - Provides `.consume` and `.tryConsume` methods for autocloseable instances (like streams, etc.).
      This does java's try-with style resource handling functionally.
    - For certain (older) Java classes that implement the method close() but don't extend AutoCloseable, 
    you may utilize the AutoCloseWrapper class
- [utopia.flow.util.console.ConsoleExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/util/console/ConsoleExtensions.scala)
  - Specifies a number of utility methods for StdIn

### You should get familiar with these classes
- **Value** - When you need to use attributes, but you can't define their exact typing below Any
- immutable.**Model** - When you need to group a number of values together to form an object
- **ThreadPool** - When you need an implicit ExecutionContext (I.e. when you need to do anything asynchronous)
- **XmlElement**, **XmlReader** & **XmlWriter** - When you need to deal with XML
- **Loop**, **Wait** and **Delay** - When you need to loop a function or a background process
- [EventfulPointer](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/view/mutable/eventful/EventfulPointer.scala), 
  [Changing](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/view/template/eventful/Changing.scala) and 
  [Flag](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/view/mutable/eventful/Flag.scala) - 
  When you need a mutable value with property change events
  - Note: Within [utopia.flow.view package](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow/src/utopia/flow/view), 
    there's a wide variety of implementations of the **Changing** interface 
    suitable for different (optimized) use-cases 
- [Lazy](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/view/immutable/caching/Lazy.scala) 
  and [CachingSeq](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/immutable/caching/iterable/CachingSeq.scala) - 
  When you want to initialize something lazily
- **CsvReader** - When you need to read .csv or other text-based tables
- [StringFrom](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/parse/string/StringFrom.scala) 
  and [LinesFrom](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/parse/string/LinesFrom.scala) - 
  When you need to read file or stream data into strings
- [Cache](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/immutable/caching/cache/Cache.scala) 
  and [TryCache](https://github.com/Mikkomario/Utopia-Scala/blob/master/Flow/src/utopia/flow/collection/immutable/caching/cache/TryCache.scala) - 
  When you need a simple lazily initialized map
- **ObjectFileContainer** & **ObjectsFileContainer** (and others) - When you need to store limited amount of 
  data locally between use sessions 