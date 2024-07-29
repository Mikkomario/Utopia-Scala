# Utopia Scribe Api
This module provides the tools necessary for setting up a Scribe logging system on your **server**.  
For details about the Scribe logging system in general, please refer to the **Scribe Core** README file.

## Parent modules
These Utopia modules must be present in your class path in order to use the Scribe Api module.
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch)
- [Scribe Core](https://github.com/Mikkomario/Utopia-Scala/tree/master/Scribe/Scribe-Core)
- [Utopia Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Nexus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Nexus)

## Main Features

Interface for storing log entries to your database
- This module provides the required database structure, as well as classes for interacting with the database
- Issues recorded using server-side **Scribe** instances are automatically recorded to the database

API-node for receiving and storing log entries from clients
- This node may be attached directly to your **RequestHandler** instance, 
  or under any of your **Resource** implementations
- This interface allows you to apply custom authorization measures to make sure only authorized requests are processed

Command line application for reviewing log entries
- Deployed as a separate runnable jar file

Automatic process that merges and cleans old log entries to conserve space in your database
- Fully configurable
- Must be triggered or set up separately as a recurring task

Maximum logging limit that safeguards your database against recursive logging
- You may customize the limit yourself
- If the limit is reached, logging is terminated

## Setting up the server-side Scribe system
In this section we will cover the steps you need to take in order to set up the Scribe logging system on your server.

First you need to **set up the database.** 
Simply run the SQL statements described in the latest scribe-db-structure document from 
[Scribe-Core/data/sql](https://github.com/Mikkomario/Utopia-Scala/tree/development/Scribe/Scribe-Core/data/sql).  
For this you will need a functioning MySQL or MariaDB database. 
We will assume you have one and not cover setting one up in this document.

In your **ApiLogic** implementation, call `ScribeContext.setup(...)` before you process any requests 
(typically in the constructor code / class body). 
Here you specify:
- Used **ExecutionContext** and **ConnectionPool** instances
- Your root **Tables** instance
- **Name of the database** you're using
  - If you're using a separate database for log entries, specify the name of that database, 
    as well as the Tables instance that corresponds with that database
- Your software **version**
- **Backup logging implementation**, which is used for recording errors within the Scribe logging system itself
- **Maximum log limit** that is used for preventing recursive logging

Next, construct a new **LoggingNode** instance and register it to your **RequestHandler**, 
or under one of your **Resource** implementations. Use the constructor that best suites your use-case.

Optionally, construct a new **Scribe** instance that you use as the root instance for all logging.
Optionally, create a new **Synagogue** instance and register your root Scribe instance to it, 
along with the backup **Logger** implementations that you wish to use.

Optionally, set up automated log cleaning processes by calling `LogCleaner.apply(LogStoreDurations) `
within a **TimedTask** or **Loop**. 
For this, you need to construct a new **LogStoreDurations** instance where you define the deletion and merging intervals 
for each Issue **Severity** level.

Here's an example code for setting up the log cleaning process.
```
// Defines merge and storage durations for Scribe logging entries
lazy val logStoreDurations = LogStoreDurations(Map(
    Debug -> LogStoreDuration(Map(
        // Merges debug logs after 1 week. Deletes them after 3 weeks.
        Merge -> Vector(Pair(2.weeks, 1.days), Pair(1.weeks, 1.hours)),
        Delete -> Vector(Pair(1.weeks, 1.weeks), Pair(3.days, 2.weeks), Pair(Duration.Zero, 3.weeks))
    )),
    Info -> LogStoreDuration(Map(
        // Starts merging info entries after one day, all the way up to 12 weeks, at which point they're deleted
        // The merging is intensified over time
        // Inactive entries are deleted at 6 weeks
        Merge -> Vector(
            Pair(7.weeks, 1.weeks), Pair(4.weeks, 24.hours),
            Pair(3.weeks, 12.hours), Pair(2.weeks, 6.hours), Pair(1.weeks, 3.hours),
            Pair(3.days, 1.hours), Pair(1.days, 15.minutes)),
        Delete -> Vector(Pair(2.weeks, 6.weeks), Pair(Duration.Zero, 12.weeks))
    )),
    // Applies to warnings and recoverable issues
    Warning -> LogStoreDuration(Map(
        // Merges older entries intensively, and more recent entries only a little
        Merge -> Vector(Pair(12.weeks, 4.weeks), Pair(8.weeks, 1.weeks),
            Pair(4.weeks, 24.hours), Pair(2.weeks, 3.hours), Pair(1.weeks, 30.minutes),
            Pair(3.days, 15.minutes), Pair(1.days, 5.minutes)),
        // Deletes all cases that are 6 months old or older
        // Inactive cases are deleted after 3 months, and resolved cases after 1.5 months
        Delete -> Vector(Pair(4.weeks, 6.weeks), Pair(2.weeks, 12.weeks), Pair(Duration.Zero, 24.weeks))
    )),
    // Applies to unrecoverable and critical issues
    Unrecoverable -> LogStoreDuration(Map(
        // Merges the entries quite slowly, but eventually (at 1 year) to 1 month groups
        Merge -> Vector(Pair(48.weeks, 4.weeks), Pair(20.weeks, 2.weeks), Pair(12.weeks, 1.weeks),
            Pair(8.weeks, 24.hours), Pair(6.weeks, 12.hours), Pair(4.weeks, 6.hours), Pair(3.weeks, 3.hours),
            Pair(2.weeks, 1.hours), Pair(1.weeks, 15.minutes)),
        // Deletes active entries after 7 years,
        // inactive entries after 1 year and resolved entries after half a year
        Delete -> Vector(Pair(4.weeks, 24.weeks), Pair(2.weeks, 48.weeks), Pair(Duration.Zero, 336.weeks))
    ))
))
// Cleans log entries once a day
tasks.addDaily(1.oClock) {
    connectionPool.tryWith { implicit c =>
        LogCleaner(logStoreDurations)
    }.failure.foreach { e => scribe.in("daily.cleanLogs").recoverable(e) }
}
```

You're now ready to construct new **Scribe** instances and to use the new logging system.  
Depending on your choice, construct new **Scribe** instances either:
1. By calling `.in(subContext: String)` from your **Synagogue** instance (recommended)
2. By calling `.in(subContext: String)` from your **root Scribe** instance
3. By constructing a new **Scribe** instance directly

## Using the Scribe console application
In this section we will cover the installation and the use of the Scribe console application. 
This is the main application for reading issue data from the database.

### Installing the console
Install the console simply by moving the runnable **Scribe-Console.jar** and the required dependencies 
to a directory on the server and by writing the necessary configuration file.

The console application requires these files in order to function correctly:
- Scribe-Console.jar
- scala-library-2.13.12.jar (or some other 2.13 version)
- scala-reflect-2.13.12.jar (or some other 2.13 version)
- [mariadb-java-client-1.5.9](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault/lib) (or other version)
- A settings file, which you may freely name and place as long as you fulfill the following requirements:
  - The file is located in the same directory, or in one of the directory's subdirectories
  - The file name includes the word "settings"
  - The file type is .json

### Configuring the console
Within the settings file, specify a json object with the following properties:
- `log_directory: Path (optional)` - Determines the directory where encountered errors are logged
  - Errors are always written on the console itself as well
- `database: Object` - Contains the following properties:
  - `address: String` - Address used when connecting to the database. The default value is `"jdbc:mysql://localhost:3306/"`
  - `user: String` - User used in authentication. The default value is `"root"`
  - `password: String` - Password used in authentication. Required if the database is password-protected.
  - `name: String` - Name of the used database

You may use an 
[example file](https://github.com/Mikkomario/Utopia-Scala/blob/development/Scribe/Scribe-Api/console-app/example-settings.json) 
as a template when writing your settings file.

### Starting the console
Go to the directory where Scribe-Console jar resides. Then run `java -jar Scribe-Console.jar`.

### Console commands
Use the `help` command to list the commands within the application itself. 
For now, we will not cover these commands within this document in detail.