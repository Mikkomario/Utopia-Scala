UTOPIA FLOW --------------------------------

Purpose
-------

    Utopia Flow is the base building block for other Utopia Libraries. Flow offers various tools for advanced data
    handling, as well as many generally required features.


Main Features
-------------

    Typeless data handling
        - Value and Model classes offer a way to handle data without type information, conveniently converting between
        various supported types like Double, Int, String and Vector
        - This allows one to represent and handle typeless data like SQL or JSON values

    JSON and XML support & Integration with models and typeless values
        - Flow offers full support for JSON and XML parsing + writing that fully utilizes the power of the typeless values
        - Conversion between JSON and Model / Value is seamless and converts between supported types under the hood
        - Please note that the current implementation of JSON parser prioritizes accessibility over performance and is not
        most efficient at this time. You may wish to use another parser for very large json files where performance
        becomes an issue.

    Various data structures
        - Tree and Graph supports
        - Support for weakly referenced lists and pointer-like data structures
        - Mutable concurrent collections (Volatile, VolatileFlag, VolatileList and VolatileOption) may be used in
        multi-threaded environments where some data is shared between multiple threads.

    Tools for asynchronous programs
        - ThreadPool implementation for generating a scaling ExecutionContext
        - WaitUtils for synchronous waiting
        - Loop and Breakable traits for looping, asynchronous operations, as well as support for shutdown hooks
        - WaitFor -simplification of Future waiting available through extension

    Data Caching
        - utopia.flow.caching package contains various tools for caching individual single or multiple pieces of data either
        temporarily or permanently, asynchronously or synchronously


Usage Notes
-----------

    When using typeless values, please call utopia.flow.generic.DataType.setup() on program startup. Please also
    import utopia.flow.generic.ValueConversions._ when creating new typeless values.


Available Extensions
--------------------

    utopia.flow.util.CollectionExtensions
        - Collection utility updates, like support for multimaps and optional return values instead of throwing
        indexOutOfBounds

    utopia.flow.util.TimeExtensions
        - Functional additions to java.time classes
        - Conversion between java.time.Duration and scala.concurrent.duration.Duration
        - Easy creation of scala.concurrent.duration.FiniteDuration by using numeric extension

    utopia.flow.async.AsyncExtensions
        - Utility updates to Future

    utopia.flow.generic.ValueConversions
        - Implicit conversions from value supported classes (Int, Double, String, Vector[Value], etc.) to Value

    utopia.flow.util.AutoClose
        - Provides consume and tryConsume methods for autocloseable instances (like streams, etc.).
        This does java's try-with style resource handling functionally.

    utopia.flow.util.StringExtensions
        - Utility methods for String

    utopia.flow.util.FileExtensions
        - A number of new scala-friendly methods for java.nio.file.Path


v1.6.1  -----------------------------------

    New Features
    ------------

        Instant + and - now also support scala duration

        Instant can now be converted to string using more wide range of DateTimeFormatters by calling method
        toStringWith(...)

        Added some new String extensions, which are available by importing utopia.flow.util.StringExtensions._

        StringFrom and LinesFrom objects for easier file- and stream reading

        TimeExtensions now also allow easier creation of Period instances. Other time extensions added as well.

        maxByOption, minByOption, tryMap, dropRightWhile and compareWith added to CollectionExtensions

        toTry method added to Option through collectionExtensions

        CollectionExtensions now allows one to sort a seqLike with multiple hierarchical orderings (sortedWith(...)).
        Seqs also now have mapFirstWhere(...)(...) method that maps the first item that matches a provided predicate.

        raceWith method added to Future through AsyncExtensions. This allows one to retrieve the first completed value
        from two futures.

        Added FileExtensions that add a number of new methods to java.nio.file.Path. Available by importing
        utopia.flow.util.FileExtensions._

        Added a VectorCollector class for collecting java streams to scala vectors

        WaitUtils now contains delayed -method which performs an operation after a delay

        Future now contains isEmpty method (which is an inverted isCompleted method) through AsyncExtensions

        AsyncExtensions now also provides futureCompletion -method for a combination of futures that works exactly
        like .future but simply returns Future[Unit]

        NewThreadExecutionContext added for cases where only one or two threads are required. It is still recommended
        to use a ThreadPool instead.
            - This feature was added for ConnectionPool closing in Vault where thread pool is not readily available
            and a single thread needs to be created at the very end of program lifespan

        Changing instances now have futureWhere(...) method that allows one to get a conditional future. Volatile now
        extends Changing and will support change events.

        Model now contains .hasOnlyEmptyValues and .hasNonEmptyValues methods in addition to .isEmpty and .nonEmpty,
        which included empty attributes.

    Updates & Changes
    -----------------

        Instant + and - won't throw anymore when adding a time period (Eg. 3.months) but will try to work around
        the problematic units by converting to local date time first. This may cause some invalid times to pass
        through but most of the time works more intuitively than the original.


v1.6  -------------------------------------

    New Features
    ------------

        Immutable model now contains renamed-function which allows one to easily change property names.

        JSONReader.parseFile(File) can now be used to parse the contents of a single json file

        Duration can now be described using .description (usable after importing utopia.flow.util.TimeExtensions._)

        New utility constructors added to ModelDeclaration.

        ModelDeclaration now contains validate(Model) method which makes sure specified model matches the declaration.

        CollectionExtensions now contains .bestMatch(...) function for Iterable items. This allows advanced searching
        based on hierarchical conditions.

        withTimeout(FiniteDuration) and resultWithTimeout(FiniteDuration) were added through AsyncExtensions to Future.

        CollectionExtensions now contains mapLeft(...) and mapRight(...) for Either.

        FromModelFactoryWithSchema handles model parsing more easily using a schema to validate the model before using it.

        New extensions added for java.time.Year, java.time.LocalDate and java.time.YearMonth

        SingleTryCache object now has expiring(...) method


    Updates & Changes
    -----------------

        Instant JSON representation is now in string format and not in long format. This might cause problems in
        external systems, although Utopia Flow should be able to parse the values just the same.

        Constant is now a case class

        ModelDeclaration syntax updated. Class constructor is now hidden. Please use object constructors instead.

        XmlReader can now use a reader. Also, XmlReader read method parameter syntax changed to use 2 parameter lists.

        FromModelFactory now returns Try[...] instead of Option[...]

        Simple loop construction syntax was change from Loop(Duration, () => Unit) to Loop(Duration, => Unit)

        Value.empty(DataType) replaced with Value.emptyWithType(DataType). Value.empty added as a property

        JSONReader completely rewritten. Methods parse... are either deprecated or removed. Please use apply method
        variations instead
            - JSONReader also now returns a Try[Value] and not a Option[Model[Constant]]

        Models now preserve attribute ordering. This mostly affects JSON-generation where previously models would
        list their attributes in varying order.


    Fixes
    -----

        JSONReader now works even when string portions contain markers like ',' '{' or ']'. Also, string json conversion
        now removes replaces double quotes " with single quotes ' to avoid parsing errors

        Since Instant.parse(String) wouldn't work on all ISO-8601 instants, added backup parsing styles to value conversion


v1.5  ------------------------------------

    New Features
    ------------

        CollectionExtensions updated
            - Map merge feature added
            - divideBy added
            - foreachWith added for simultaneous iterations
            - Repeating iterations added
            - Pairing and map folding added to seqs

        Trees updated
            - replace and findAndReplace added to immutable TreeLike
            - allContent added to TreeLike

        AsyncExtensions updated
            - Iterable items containing futures can now be treated like a future themselves with
            waitFor, waitForSuccesses, future and futureSuccesses
            - Additional support for Future[Try[...]]

        ActionQueue added
            - Allows sequential completion of multiple operations
            - Variable width allows control over how many actions are performed simultaneously

        Pointers with events added + property change events added to mutable models
            - Now you can listen to changes within a model or a pointer by adding a listener to it

        Easier duration creation added through importing TimeExtensions._
            - You can now write 3.seconds, 5.millis etc.

        Added classes for caching data
            - Single caches for caching single items
                - ClearableSingleCache for flushable caching
                - ExpiringSingleCache for temporary caching
                - SingleTryCache for caching with failures, where failed attempts will be cached only temporarily
                - SingleAsyncCache for requesting data asynchronously
            - Other caches for caching multiple items in key-value pairs
                - Cache for very simple caching
                - ExpiringCache for temporary caching
                - TryCache for caching with error handling (including temporary option)
                - AsyncCache for asynchronous caching (including temporary option)
                - ReleasingCache for weak caching

    Changes & Updates
    -----------------

        java.time.Duration usage changed to scala.concurrent.duration.Duration

        Lazy set function deprecated

    Bugfixes
    -----------

        Major bugfix in ThreadPool, which didn't queue actions correctly


v1.4  --------------------------------------

    New Features
    ------------

        ThreadPool class for creating an asynchronous ExecutionContext

        Breakable tasks and loops for asynchronous operations

        Extensions to Future

        Volatile classes as safe multithreaded collections

        CloseHook for ShutDownHook implementation


    Updates & Changes
    -----------------

        Package structure changed

        Tree refactored
        template.Tree renamed to TreeLike. Added immutable and mutable TreeLikes as well. XmlElement extends immutable.TreeLike

        Deprecated functions removed from Value. Value is now a case class

        getX methods added to Value

        Template version of GraphNode and GraphEdge added. Graph classes refactored.

        BugFix to Counter class

        Model constructors updated