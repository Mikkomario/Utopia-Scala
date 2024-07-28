# Utopia Flow
*The foundational Utopia module for any application*

## Main Features
These are the features you get when you use Utopia Flow

### Typeless data handling
**Value** and **Model** classes offer a way to handle data without type information, a little like in Python, 
for example, conveniently converting between various supported types like Double, Int, String and Vector.

This is very useful in interfaces to typeless and soft-typed systems and languages, such as MySQL, JSON and XML. 
If you have previous experience with Java in dealing with these interfaces, you will know the pain of 
having to convert all *Any* / *Object* types to appropriate data types, dealing with possible 
exceptions at every corner. With **Flow**, you can simply call `.toInt` etc., and the type conversion algorithm will 
work it's hardest to give you the desired result.

What's more, convertible types and conversion logic may be introduced in lower modules as well, 
meaning that you're not tied to just the basic implementation here in **Flow**. For an example on how to do this, 
please check the **Paradigm** module's `utopia.paradigm.generic` package.

### JSON, XML and CSV support & Integration with models and typeless values
- **Flow** offers full support for JSON and XML processing that fully utilizes the power of the typeless values
- Conversion between JSON and **Model** / **Value** is seamless and converts between supported types under the hood
- Please note that the current implementation of JSON parser prioritizes accessibility over performance and is not
very efficient at this time. You may wish to use another parser for very large json files where performance
becomes an issue.
  - There is a much more efficient implementation of json reading in **BunnyMunch** module (**JsonBunny**). 
  It offers the same benefits as **JsonReader**, just much more efficiently. The difference is that 
  **JsonBunny** leverages [Jawn](https://github.com/typelevel/jawn) json parser while **JSONReader** is completely 
  independent.  
    - If you plan to use json parsing in any real (i.e. commercial) application, I'd highly suggest to use **JsonBunny** 
    instead of **JsonReader**.
- There is a very neat CSV file parser (**CsvReader**) available, also

### Event-based pointers
- Both mutable and read-only pointer interfaces that support mapping, merging, etc.
  - A critical tool when creating reactive systems (such as GUIs)
- These include full support for value change events

### Various data structures
- Both mutable and immutable **Tree** and **Graph** data structures
- Support for various collection and **Iterator** classes, such as **Pair** and **PollingIterator**
- A number of lazily initialized collection classes (**CachingSeq**, **LazySeq**, **LazyPair**, **CachingMap**, etc.)
- Mutable concurrent collections (**Volatile**, **VolatileFlag**, **VolatileList** and **VolatileOption**) 
  may be used in multithreaded environments where data is being modified and accessed from multiple threads.
- Support for weakly referenced lists
- A large number of additional functions to standard Scala collections via **CollectionExtensions**

### Tools for asynchronous programs
- **ThreadPool** implementation for generating a scaling **ExecutionContext**
- **Process** and **LoopingProcess** traits for looping, asynchronous operations, as well as support for shutdown hooks
- Blocking `.waitFor()` -simplification of **Future** waiting available through extension (**AsyncExtensions**)
- Various waiting -related tools
  - See **Wait** for more details

### Data caching & Local data containment
- `utopia.flow.view.immutable.caching` package contains various tools for caching individual or multiple pieces of 
  data either temporarily or permanently, asynchronously or synchronously
- `utopia.flow.parse.file.container` package contains numerous classes and traits for storing data locally in json files, 
which is useful when you need to store small amounts of data and don't want to use a local database 
(i.e. **Vault** and/or **Trove**)
  
### Pleasant interface for time classes
- A Scala- and user-friendly interface to Java's time classes (**Instant**, **LocalDate**, **LocalTime** etc.) through 
extension (**TimeExtensions**)
- A new set of time-related models such as **WeekDay**, **Days** and **DateRange**

### Easy-to-use interface for file interactions
- Additional easy-to-use functions for files (i.e. java.nio.file.Path instances) through extension (**FileExtensions**)

### String and regular expression -utilities
- The **Regex** class offers a more beginner-friendly (and readable) way to build regular expressions, 
  which is useful for people like me who didn't grow up using them every day
- **StringExtensions** also offers a set of neat methods for easier String-handling

### Command line interface template
- `utopia.flow.util.console` package contains various tools for command line argument handling
- Full console-based interface is also supported (see **Console** class)

## Implementation Hints
Hints and advice to help you get started and to get most out of Utopia Flow

### What you should know before using Flow
When you wish to instantiate typeless values, please enable implicit value conversions by adding 
`import utopia.flow.generic.casting.ValueConversions._`

### Extensions you should be aware of
- utopia.flow.generic.casting.**ValueConversions**
  - Implicit conversions from value-supported classes (Int, Double, String, Vector, etc.) to **Value**
  - Also, through importing utopia.flow.generic.casting.**ValueUnwraps**, you gain implicit conversions from **Value** 
    back to these data types, which is useful in **FromModelFactory** -implementations, for example.
- utopia.flow.collection.**CollectionExtensions**
    - Contains a large number of method additions for standard collection classes, such as 
    Iterator, Iterable, Seq, Option, Either and Map
- utopia.flow.time.**TimeExtensions**
    - Functional additions to java.time classes
    - Conversion between java.time.Duration and scala.concurrent.duration.Duration
    - Easy creation of scala.concurrent.duration.FiniteDuration by using numeric extension 
      (E.g. by writing `2.minutes`)
- utopia.flow.parse.file.**FileExtensions**
  - A number of new scala-friendly methods for java.nio.file.Path
- utopia.flow.async.**AsyncExtensions**
    - Utility updates to Future
- utopia.flow.util.**StringExtensions**
  - Utility methods for String
- utopia.flow.parse.**AutoClose**
    - Provides `.consume` and `.tryConsume` methods for autocloseable instances (like streams, etc.).
      This does java's try-with style resource handling functionally.
    - For certain (older) Java classes that implement the method close() but don't extend AutoCloseable, 
    you may utilize the AutoCloseWrapper class
- utopia.flow.util.console.**ConsoleExtensions**
  - Specifies a number of utility methods for StdIn

### You should get familiar with these classes
- **Value** - When you need to use attributes, but you can't define their exact typing below Any
- immutable.**Model** - When you need to group a number of values together to form an object
- **ThreadPool** - When you need an implicit ExecutionContext (I.e. when you need to do anything asynchronous)
- **XmlElement**, **XmlReader** & **XmlWriter** - When you need to deal with XML
- **Loop**, **Wait** and **Delay** - When you need to loop a function or a background process
- **EventfulPointer**, **Changing** and **Flag** - When you need a mutable value with property change events
  - Note: Within `utopia.flow.view` package, there's a wide variety of implementations of the **Changing** interface 
    suitable for different (optimized) use-cases 
- **Lazy** and **CachingSeq** - When you want to initialize something lazily
- **CsvReader** - When you need to read .csv or other text-based tables
- **StringFrom** and **LinesFrom** - When you need to read file or stream data into strings
- **Cache** and **TryCache** - When you need a simple lazily initialized map
- **ObjectFileContainer** & **ObjectsFileContainer** (and others) - When you need to store limited amount of 
  data locally between use sessions 