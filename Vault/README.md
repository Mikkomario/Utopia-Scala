# Utopia Vault
Vault is the main Utopia library for interacting with MySQL-based databases.  
However, what's great with Vault is that you don't need much SQL know-how to use it.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)

## Required external libraries
You will need an external JDBC driver implementation in your classpath for **Vault** to work. 
I usually use **MariaDB Java Client** myself, a copy of which you can find from the 
[lib](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault/lib) directory.

## Main Features
Database connection and pooled connection handling
- Using database connections is very streamlined in Vault which handles many repetitive and necessary 
  background tasks like closing of result sets and changing database.
- Query results are wrapped in immutable 
  [Result](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/model/immutable/Result.scala) 
  and [Row](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/model/immutable/Row.scala) 
  classes, from which you can read all the data you need
- Statement preparation will be handled automatically for you

SQL Statements with full support for typeless values and models
- **Vault** uses **Flow**'s **Value** and **Model** classes which means that all data types will be handled 
  automatically.

Template Statements that make database interactions much simpler and less prone to errors
- **Insert**, **Update**, **Delete**, **Select**, **SelectAll**, **SelectDistinct**, 
  **Limit**, **OrderBy**, **MaxBy**, **MinBy** and **Exists** statements
- Easy to write conditions with **Where** and **Condition**
- You don't need to know the underlying sql syntax for these statements. All you need to know is what they do and 
  in which order to chain them.

Automatic table structure and table reference reading
- Use [Tables](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/database/Tables.scala) 
  object to read table and reference data directly from the database
- This means that you only need to update your database and all models will automatically reflect those changes.
- Column names that use underscores '_' are converted to camel case syntax more appropriate for Scala / Java 
  environments (E.g. "row_id" is converted to "rowId") (optional feature)

Advanced joining between tables using [Join](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/sql/Join.scala) 
and [SqlTarget](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/sql/SqlTarget.scala)
- Once reference data has been set up (which is done automatically in a **Tables** object), you can join tables 
  together without specifying any columns or conditions. **Vault** will fill in all the blanks for you.
    - If you wish to manually specify joined columns, that is also possible

**Storable**, **Readable**, Factory and **Access** traits for object-oriented database interactions
- [Storable](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/model/immutable/Storable.scala) 
  trait allows you to push (update or insert) model data to database with minimum sql syntax
- **Readable** trait allows you to pull (read) up-to-date data from database to your model
- Mutable **DBModel** class implements both of these traits
- Factory traits can be used for transforming database row data into your object models
    - You can include data from multiple tables if you want. Simply use 
    [FromResultFactory](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/nosql/factory/FromResultFactory.scala) or 
      [FromRowFactory](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/nosql/factory/row/FromRowFactory.scala).
- **Access** traits allow you to create simple interfaces into database contents and to hide the actual sql-based 
  implementation
- These traits allow you to use a MariaDB / MySQL server in a no-SQL, object-oriented manner

## Implementation Hints
Unless you're using a local test database with root user and no password, please specify connection settings
with `Connection.settings = ConnectionSettings(...)` or `Connection.modifySettings(...)`

The default driver option (None) in connection settings should work if you've added mariadb-java-client-....jar
to your build path / classpath. If not, you need to specify the name of the class you wish to use and make
sure that class is included in the classpath.

If you want to log errors or make all parsing errors fatal, please change `ErrorHandling.defaultPrinciple`.

If you want to manage column length limits, I'd recommend calling `ColumnLengthRules.loadFrom(Path)`, which reads 
column length management settings from a .json file.

When using **Vault**, it is highly recommended to also utilize the 
[Vault-Coder](https://github.com/Mikkomario/Utopia-Coder/tree/master/Vault-Coder) utility application, 
which can generate both database structure and Scala code for you, based on your model structure 
(a custom json document).

### You should get familiar with these classes
- [ConnectionPool](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/database/ConnectionPool.scala) - 
  When you need to open new database connections
- [Connection](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/database/Connection.scala) - 
  When you need to use database connections (usually passed as an implicit parameter)
- **Tables** - When you need to refer to database tables
- **Result** & **Row** - When you read database query results
- [Table](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/model/immutable/Table.scala) & 
  [Column](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/model/immutable/Column.scala) - 
  When you need to deal with database tables and columns
- **Storable** - When you're creating models for noSQL database interaction
- [FromValidatedRowFactory](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/nosql/factory/row/model/FromValidatedRowModelFactory.scala), 
  **FromRowFactory**, **FromResultFactory**, etc. -
  When you need to read model data from the database
- [SingleModelAccess](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/nosql/access/single/model/SingleModelAccess.scala) & 
  [ManyModelAccess](https://github.com/Mikkomario/Utopia-Scala/blob/master/Vault/src/utopia/vault/nosql/access/many/model/ManyModelAccess.scala) - 
  When you need to create interfaces for reading model data from the database
- **Select**, **SelectAll**, **Update**, **Insert**, **Delete** & **Exists** - When you need to create SQL queries
- **Where**, **Limit** and **OrderBY** - When you need to limit SQL queries