# Utopia Nexus for Tomcat - List of changes

## v1.2.4 (in development)
New Build / Supports changes in **Flow** v1.16
### New Features
- Added **LogicWrappingServlet** to simplify **HttpServlet** creation
  - Added **ApiLogic** class to further simplify this, providing a standard servlet implementation logic that utilizes 
    a **RequestHandler** 

## v1.2.3 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v1.2.2 - 04.11.2021
New build / supports changes in **Flow** v1.14

## v1.2.1 - 4.9.2021
This update didn't change the code, but required a rebuild in order to support changes in the higher modules.

## v1.2 - 30.8.2020
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Added implicit JsonParser parameter to HttpExtensions request conversion. 
The parser is used when parsing request parameters.
