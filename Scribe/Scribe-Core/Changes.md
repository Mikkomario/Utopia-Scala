# Utopia Scribe Core - list of changes

## v1.0.2 (in development)
Supports **Flow v2.4**
### Bugfixes
- **ClientIssue**`.toModel` didn't previously include the issue occurrence details, 
  meaning that they wouldn't get delivered to the server (fixed)
### New features
- Added **NoOpScribe** for situations where you don't want to set up any logging
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v1.0.1 - 22.01.2024
This update focuses on enabling you to print error data to console as well as to the default target (server database).
### New features
- Added **ConsoleScribe** class that writes detailed issue data to the console, 
  and possibly to an external log file as well
- **Synagogue** now supports "copy-logging"
  - Simply call `.copyTo(...)` in order to introduce logging end-points that also need to be notified of any records
### Other changes
- Scala version updated to 2.13.12

## v1.0 - 27.09.2023
Initial release
