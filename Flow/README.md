# Utopia Flow
*The foundational Utopia module for any application*

## Main Features
What you get when you use Utopia Flow

### Typeless data handling
- **Value** and **Model** classes offer a way to handle data without type information, 
  conveniently converting between various supported types like Double, Int, String and Vector
- This allows one to represent and handle typeless data like SQL or JSON values

### JSON, XML and CSV support & Integration with models and typeless values
- Flow offers full support for JSON and XML processing that fully utilizes the power of the typeless values
- Conversion between JSON and Model / Value is seamless and converts between supported types under the hood
- Please note that the current implementation of JSON parser prioritizes accessibility over performance and is not
most efficient at this time. You may wish to use another parser for very large json files where performance
becomes an issue.
    - There is a much more efficient implementation of json reading in **BunnyMunch** module (**JsonBunny**). 
    It offers the same benefits as **JSONReader**, just more efficiently. The difference is that 
    **JsonBunny** leverages *Jawn* json parser while **JSONReader** is completely independent.
- There is a very neat CSV file parser (**CsvReader**) available, also

### Event-based pointers
- Mutable and read-only pointer interfaces tha support mapping and merging
  - A critical functionality when creating reactive systems (like GUIs)
- Full support for value change events

### Various data structures
- Tree and Graph supports
- Support for weakly referenced lists
- Mutable concurrent collections (**Volatile**, **VolatileFlag**, **VolatileList** and **VolatileOption**) 
  may be used in multi-threaded environments where some data is shared between multiple threads.

### Tools for asynchronous programs
- **ThreadPool** implementation for generating a scaling ExecutionContext
- **Loop** and **Breakable** traits for looping, asynchronous operations, as well as support for shutdown hooks
- `.waitFor` -simplification of **Future** waiting available through extension (**AsyncExtensions**)

### Data caching & Local data containment
- utopia.flow.caching package contains various tools for caching individual or multiple pieces of data either
temporarily or permanently, asynchronously or synchronously
- utopia.flow.container package contains numerous classes and traits for storing data locally in json files, 
which is useful when you need to store small amounts of data and don't want to use a local database 
(**Vault** and/or **Trove**)
  
### Pleasant interface for time classes
- A scala- and user-friendly interface to Java's time classes (Instant, LocalDate, LocalTime etc.) through 
extension (**TimeExtensions**)
- **WaitUtils** for synchronous waiting

### Easy-to-use interface for file interactions
- Additional easy-to-use functions for files (java.nio.Path) through extension (**FileExtensions**)
- Advanced multi-threading file search algorithm (**Guild**)

### Lots of quality-of-life collection updates
- Access to a number of new collection methods via extension (**CollectionExtensions**)

## Implementation Hints
Hints and advice to help you get started and to get most out of Utopia Flow

### What you should know before using Flow
When you use **Flow** or its sub-modules, you want to call `utopia.flow.generic.DataType.setup()` at the
beginning or your App.

When you wish to instantiate typeless values, please enable implicit value conversions by adding 
`import utopia.flow.generic.ValueConversions._`

### Extensions you should be aware of
- utopia.flow.util.**CollectionExtensions**
    - Collection utility updates, like support for multimaps and optional return values instead of throwing
      indexOutOfBounds
- utopia.flow.util.**TimeExtensions**
    - Functional additions to java.time classes
    - Conversion between java.time.Duration and scala.concurrent.duration.Duration
    - Easy creation of scala.concurrent.duration.FiniteDuration by using numeric extension 
      (E.g. by writing `2.minutes`)
- utopia.flow.async.**AsyncExtensions**
    - Utility updates to Future
- utopia.flow.generic.**ValueConversions**
    - Implicit conversions from value supported classes (Int, Double, String, Vector[Value], etc.) to **Value**
- utopia.flow.generic.**ValueUnwraps**
  - The opposite of **ValueConversions**; Allows you to implicitly convert Value to supported classes like 
    String, Double, Int etc.
  - Useful in JSON parsing and other such situations where you read a number of values from typeless data.
- utopia.flow.util.**AutoClose**
    - Provides `.consume` and `.tryConsume` methods for autocloseable instances (like streams, etc.).
      This does java's try-with style resource handling functionally.
- utopia.flow.util.**StringExtensions**
    - Utility methods for String
- utopia.flow.util.**FileExtensions**
    - A number of new scala-friendly methods for java.nio.file.Path
- utopia.flow.util.**NullSafe**
    - When working with Java classes, you sometimes need to perform null-checks. In case you do, you can simply
      convert a possibly null value into an option by calling `.toOption`
    - You should only import this extension in contexts where nulls are being received from Java methods
    - Alternatively you can simply use `Option.apply(...)`

### You should get familiar with these classes
- **Value** - When you need to use attributes, but you can't define their exact typing below Any
- immutable.**Model**[Constant] - When you need to group a number of values together to form an object
- **ThreadPool** - When you need an implicit ExecutionContext (I.e. when you need to do anything asynchronous)
- **XmlElement**, **XmlReader** & **XmlWriter** - When you need to deal with XML
- **Loop** - When you need to loop a function or a process in background
- **WaitUtils** - When you need to block / wait for a period of time
- **PointerWithEvents** & ChangingLike - When you need a mutable value with property change events
- **CsvReader** - When you need to read .csv or other text-based tables
- **StringFrom** & **LinesFrom** - When you need to read file or stream data into strings
- **TryCache** - When you need to cache function results that may fail
- **ObjectFileContainer** & **ObjectsFileContainer** (and others) - When you need to store limited amount of 
  data locally between use sessions 