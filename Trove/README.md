# Utopia Trove

## Parent Modules
- Utopia Flow
- Utopia Vault

## Main Features
Local embedded database
- Easy startup and shutdown commands

Database version control
- Automatic database version updates based on .sql files

## Implementation Hints

### Required External Libraries
This project has a Maven dependency: ch.vorburger.mariaDB4j:mariaDB4j:2.4.0

### Things you should know when using Trove
Starting and shutting down the database takes a while. A loading screen or some other user feedback 
should be shown during the setup.

Include createDatabaseVersionTable.sql file contents in your database structure documents.

Your database structure documents should contain the following header comments:
- "Type: " "Full" or "Changes"
    - Where default is "Full"
- "Version:" or "to:", followed by a version number (E.g. "Version: v1.2.3")
- "from:" or "origin:", followed by a version number
    - This field is present only in change / update documents
See **ScanSourceFiles** for details

### Classes you should be aware of
- **LocalDatabase** - For starting and stopping a local database