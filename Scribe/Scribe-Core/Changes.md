# Utopia Scribe Core - list of changes

## v1.0.2 (in development)
Supports **Flow v2.4**

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
