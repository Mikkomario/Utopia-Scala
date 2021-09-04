# Utopia Nexus for Tomcat - List of changes

## v1.2.1 - 4.9.2021
This update didn't change the code, but required a rebuild in order to support changes in the higher modules.

## v1.2 - 30.8.2020
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Added implicit JsonParser parameter to HttpExtensions request conversion. 
The parser is used when parsing request parameters.