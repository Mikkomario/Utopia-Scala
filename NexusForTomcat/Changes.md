# Utopia Nexus for Tomcat - List of changes
## v1.2
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Added implicit JsonParser parameter to HttpExtensions request conversion. 
The parser is used when parsing request parameters.