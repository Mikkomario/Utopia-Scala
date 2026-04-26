# Utopia Nexus for Tomcat - List of changes

## v1.3.5 (in development)
Built with Scala v2.13.18

## v1.3.4 - 15.03.2026
In this update, I rewrote every class offered, focusing on **Nexus v2** support and asynchronous request -handling.
### Breaking changes
- Moved **HttpExtensions** to `controller.servlet` package
- **LogicWrappingServlet** now requires `logger: Logger`, `jsonParser: JsonParser` `exc: ExecutionContext` and 
  `expectedParameterEncoding: ParameterEncoding`
### Deprecations
- Replaced **ServletLogic** with a new version in the `controller.servlet` package
  - The new version accepts **ParameterEncoding** instead of **ServerSettings**, and requires `logger: Logger`
- Replaced **LogicWrappingServlet** with a new version in the `controller.servlet` package
- Replaced **ApiLogic** with a new version in the `controller.servlet` package
  - The new version no longer implements request or response -interception; These are now handled in **ApiRoot**.
- Replaced `.toRequest` in **HttpExtensions** with `.toNexusRequest`
### New features
- Asynchronously completing requests are now supported
- Added **ParameterEncoding** class; Expected parameter encoding is now communicated with it, 
  instead of with **ServerSettings** 
### New methods
- Added `.toNexusRequest` to **HttpServletRequest** (via **HttpExtensions**)

## v1.3.3 - 01.11.2025
A new build supporting **Flow v2.7**.

## v1.3.2 - 26.05.2025
This update adds support for **Access v1.6**

## v1.3.1 - 23.01.2025
Support for latest Flow

## v1.3 - 04.10.2024
Made request & response -interception a little easier to use and modified `Date` header generation logic.
### Breaking changes
- **ApiLogic** now accepts **RequestInterceptor** and **ResponseInterceptor** traits instead of simple functions
  - However, existing function definitions should implicitly convert to request interceptors, 
    so no build errors should follow on that side
### Other changes
- Built with Scala v2.13.14
- Modified response `Date` -header generation in **ApiLogic** to overwrite existing response header, 
  unless the existing value is earlier than the request time
- Removed previously deprecated **EchoServlet**

## v1.2.7 - 28.07.2024
A very minor update
### Deprecations
- Deprecated **EchoServlet**. A more viable example is available under the **Nexus-Test-Server** module.
### New methods
- **LogicWrappingServlet** (object)
  - Added a new constructor

## v1.2.6 - 22.01.2024
Supports **Flow v2.3**
### Other changes
- Scala version updated to 2.13.12

## v1.2.5 - 02.02.2023
This update supports the changes introduced in **Flow** v2.0.

## v1.2.4 - 18.08.2022
This update adds a pre-built **HttpServlet** base, making new servlet creation much easier.
### New Features
- Added **LogicWrappingServlet** to simplify **HttpServlet** creation
  - Added **ApiLogic** class to further simplify this, providing a standard servlet implementation logic that utilizes 
    a **RequestHandler** 
    - The new **ApiLogic** instance supports request and response interception
    - Internal server errors are automatically logged and a date header is added where not present

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
