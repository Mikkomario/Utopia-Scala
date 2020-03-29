UTOPIA VAULT --------------------------------

Required Libraries
------------------
    - Utopia Flow
    - Maria DB or MySQL client (used mariadb-java-client-1.5.9.jar in development)


Purpose
-------

    Utopia Vault is a framework for interacting with MariaDB / MySQL databases without having to write any SQL.
    Vault supports operations on high object oriented level, sql statement level and raw sql connection level.


Main Features
-------------

    Database connection and pooled connection handling
        - Using database connections is much more streamlined in Vault, which handles many repetitive and necessary
        background tasks like closing of result sets and changing database.
        - Query results are wrapped in immutable Result and Row classes, from which you can read all the data you need
        - Value insertion and statement preparation will be handled automatically for you

    SQL Statements with full support for typeless values and models
        - Vault uses Flow's Value and Model classes which means that all data types will be handled automatically
        under the hood.

    Template Statements that make database interactions much simpler and less prone to errors
        - Insert, Update, Delete, Select, SelectAll, SelectDistinct, Limit, OrderBy, MaxBy, MinBy and Exists statements
        - Easy to write conditions with Where and Extensions
        - You don't need to know specific syntax for these statements. All you need to know is what they do and
        in which order to chain them.

    Automatic table structure and table reference reading
        - Use Tables object to read table and reference data directly from the database
        - This means that you only need to update your database and all models will automatically reflect those changes.
        - Column names that use underscores '_' are converted to camel case syntax more appropriate for scala / java
        environments (eg. "row_id" is converted to "rowId")

    Advanced joining between tables using Join and SqlTarget
        - Once reference data has been set up (which is done automatically in Tables object), you can join tables
        together without specifying any columns or conditions. Vault will fill in all the blanks for you.
        - If you wish to manually specify joined columns, that is also possible

    Storable, Readable, Factory and Access traits for object-oriented database interactions
        - Storable trait allows you to push (update or insert) model data to database with minimum syntax
        - Readable trait allows you to pull (read) up to date data from database to your model
        - Mutable DBModel class implements both of these traits
        - Factory traits can be used for transforming database row data into your object models
            - You will be able to include data from multiple tables, if you wish. Simply use
            FromResultFactory or FromRowFactory.
        - Access traits allow you to create simple interfaces into database contents and to hide the actual sql-based
        implementation
        - These traits allow you to use a MariaDB / MySQL server in a noSQL, object-oriented manner


Usage Notes
-----------

    Please call utopia.flow.generic.DataType.setup() before making any queries. Also, when adding values to queries
    and models, you can import utopia.flow.generic.ValueConversions._ for implicit value conversions.

    Unless you're using a local test database with root user and no password, please specify connection settings
    with Connection.settings = ConnectionSettings(...) or Connection.modifySettings(...)

    The default driver option (None) in connection settings 'should' work if you've added mariadb-java-client-....jar
    to your build path / classpath. If not, you need to specify the name of the class you wish to use and make
    sure that class is included in the classpath.

    If you want to log errors or make all parsing errors fatal, please change ErrorHandling.defaultPrinciple.


Available Extensions
--------------------

    utopia.vault.sql.Extensions
        - Allows you to use values (or value convertible items) as condition elements
        - Usually works in combination with utopia.flow.generic.ValueConversions


v1.4  ----------------------------------

    New Features
    ------------

        LinkedStorableFactory and MultiLinkedStorableFactory added as utility options for converting DB data from
        two tables into a single model. Cases where you need to link more than 2 tables should still be handled using
        FromRowFactory or FromResultFactory.

        Result now contains split(Table) method to be used when working with joined tables and FromResultFactories.

        Result also contains parse and parseSingle methods for easier access with factories

        Result also contains rowValues and rowIntValues methods for easier data access for single value selections

        New max and min methods in SingleIdAccess

        ConditionalAccess, ConditionalSingleAccess and ConditionalManyAccess traits/classes allow access to database
        rows based on a search conditions and can be used to provide sub groups under accesses

        Added support for offset in sql statements (used for skipping n rows from the beginning of results)

        Added foreach, fold, mapReduce and flatMapReduce functions to command to support very large queries. These
        queries will be split in parts when used. The size of each split may be specified in ConnectionSettings.
        Default split size is 10000 rows.

        foreach, fold and mapReduce also added to FromRowFactory

        Column name conversion method can now be changed in Tables -object.

        Select.tables added

        DatabaseCache added. Allows one to temporarily or permanently cache results of simple queries.

        Storable instances can now be converted to a wider range of conditions using toConditionWithOperator(...)

        Added RowFactoryWithTimestamps trait for all row factories that have a creation time column


    Updates & Changes
    -----------------

        Package structure updated

        A new set of access classes was created and the existing set of classes was deprecated for removal

        Update no longer returns an option

        ConnectionPool now properly closes connections on jvm exit

        ConnectionPool now has default parameters. Also, java.time.Duration was changed to
        scala.concurrent.duration.Duration in ConnectionPool.

        Tables object was converted to a class. Uses a ConnectionPool for reading class data instead of using an
        individual DB connection.

        MultiLinkedStorableFactory now accepts any FromRowFactory as the child factory

        FromRowFactory now returns Try instead of Option. Added parseIfPresent method in cases where one needs to first
        check whether table data exists in specified row.


    Fixes
    -----

        Result.grouped(...) didn't work correctly when secondary table had empty rows. Now empty rows are no longer
        included in the resulting map.

        Insert couldn't previously insert empty rows to a database. This is now possible.

        DatabaseTableReader will now work with all table names, including those like "order"
        (Added `` around requested table name)


    Required Libraries
        ------------------
            - Utopia Flow 1.6.1+
            - MariaDB or MySQL client


v1.3  ---------------------------------------

    New Features
    ------------

        Number of updated rows can now be retrieved from a Result instance (on update).

        getMin & getMax methods added to FromRowFactory

        new Tables object reads both tables and references directly from the database. There's no need to use
        DatabaseTableReader, DatabaseReferenceReader or some other kind of setup anymore.
            - Please note that column names are converted to property names using "underscore to camel case" -rule.
            For example, "test_column" is converted to "testColumn"

        Connection object now contains modifySettings(...) method for cases when you simply wish to modify a couple
        of settings and not the whole settings instance.

        SelectDistinct object was added to create queries where you only wish to receive distinct values.

        MaxBy and MinBy objects were added for easier orderBy ... Limit 1 -syntax.

        StorableWithFactory now contains searchMin(String) & searchMax(String) -methods

        StorableFactory parsing failures may now be handled through ErrorHandling object. By default the handler ignores
        all errors.

        StorableFactoryWithValidation provides an option for easy model creation from database data. The data
        is automatically validated before model conversion so you don't need to performs any validation yourself.

        Result now allows one to group rows based on two or more tables (grouped(...) methods).

        Access, SingleAccess, ManyAccess, SingleAccessWithIds and ManyAccessWithIds -traits were added for model and
        index access interfaces


    Updates & Changes
    -----------------

        Connection now throws DBExceptions instead of SQLExceptions on failure. Storable also throws more detailed
        DBExceptions on failure.

        Insert & push methods in storable now work on tables without auto-increment indexing when the model has a
        specified index.

        References.setup(...) now preserves existing data.

        Insert.apply(...) now executes the insert statement instead of just creating one. Return type changed to Result.

        FromResultFactory.target changed from protected to public

        A failed join will now throw an exception

        .in(...) (when creating conditions) will now accept any traversable group and not just seqs

        FromResultFactory, FromRowFactory and StorableFactory were moved to utopia.vault.nosql.factory


    Fixes
    -----

        References.columnsBetween(Table, Table) had a programming error in it and didn't work. This caused all reference
        joins to fail as well. Fixed the error.

        Added value parsing from DOUBLE columns, which was missing for some reason. Fixed other parsings as well.


    Required Libraries
    ------------------
        - Utopia Flow 1.6+
        - MariaDB or MySQL client


v1.2.1  ----------------------------------

    Updates & Changes
    -----------------

        New utility methods (firstValue) in Result & Row

        StorableWithFactory now accepts FromRowFactories instead of just StorableFactories


    Required Libraries
    ------------------
        - Utopia Flow 1.5+
        - MariaDB or MySQL client


v1.2    ----------------------------------

    New Features
    ------------

        FromRowFactory trait for converting (joined) database rows to object data.

        Exists object for checking whether any results can be found for a query. Also added two exists methods to
        FromResultFactory to see whether there would be any object data for specified query / index.

        search(...) and searchMany(...) methods added to Storable. Also addded StorableWithFactory trait that provides
        search() and searchMany() without parameters.


    Updates & Changes
    -----------------

        FromResultFactory now only has getMany(...) and getAll() methods because Limit(1) couldn't be used with all
        join styles. FromRowFactory now handles cases where data can be represented using a single row, which also
        supports Limit(1).

        Result.isEmpty now only checks whether there are any rows or generated keys available (empty rows now count).
        Also added nonEmpty that behaves exactly like !isEmpty.

        Storable(...) now produces a StorableWithFactory instead of just Storable


    Required Libraries
    ------------------
        - Utopia Flow 1.5+
        - MariaDB or MySQL client


v1.1.1  ----------------------------------

    New Features
    ------------

        FromResultFactory trait to support StorableFactory-like features when a model is constructed from data between
        multiple joined tables.

        Added apply(Storable) to Where for easier conversion from storable to where clause


    Required Libraries
        ------------------
            - Utopia Flow 1.5+
            - MariaDB or MySQL client


v1.1  ------------------------------------

    New Features
    ------------

        ConnectionPool class for reusing connections and for connection sharing

        index(...) & indices(...) methods in Table allow to easily search for specific row indices

        Utility index methods added to Result and Row classes

        ascending and descending methods added to OrderBy


    Changes & Updates
    -----------------

        Storable and StorableFactory traits now offer simple immutable model -based implementations when calling apply

        Made Result, Row Column and Table case classes

        Updated package structure
            - model divided into model.immutable and model.mutable
            - test package moved under a separate source

        Added `backticks` around column and table names within sql statements to avoid errors concerning reserved
        workds in MySQL

        Result and row index methods updated. Row index doesn't take parameters anymore and returns first index. Also,
        instead of returning Option[Value], the index methods now return Value (which may be empty)


    Fixes
    -----

        Fixed an error in DatabaseTableReader where table description syntax had changed


    Required Libraries
    ------------------
        - Utopia Flow 1.5+
        - MariaDB or MySQL client