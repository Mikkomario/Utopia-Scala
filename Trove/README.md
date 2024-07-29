# Utopia Trove
Trove allows you to host a local MariaDB database from within the application itself.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault)

## Dependencies
This project uses the [MariaDB4j library](https://github.com/MariaDB4j/MariaDB4j), 
which is licensed under the Apache 2.0 license.  

You will need to add a Maven dependency: `ch.vorburger.mariaDB4j:mariaDB4j:3.0.1`

## Main Features
Local embedded database
- Easy startup and shutdown commands

Database version control
- Automatic database version updates based on .sql files

## Implementation Hints
Starting and shutting down the database takes a while. A loading screen or some other user feedback 
should be shown during the setup.

Include [createDatabaseVersionTable.sql](https://github.com/Mikkomario/Utopia-Scala/tree/master/Trove/sql) 
file contents in your database structure documents.

Your database structure documents should contain the following header comments:
- `Type:` `Full` or `Changes`
    - Where default is `Full`
- `Version:` or `to:`, followed by a version number (E.g. `Version: v1.2.3`)
- `from:` or `origin:`, followed by a version number
    - This field is present only in change / update documents

See [ScanSourceFiles](https://github.com/Mikkomario/Utopia-Scala/blob/master/Trove/src/utopia/trove/controller/ScanSourceFiles.scala) 
for details

### Classes you should be aware of
- [LocalDatabase](https://github.com/Mikkomario/Utopia-Scala/blob/master/Trove/src/utopia/trove/controller/LocalDatabase.scala) - 
  For starting and stopping a local database