# Utopia Nexus - List of Changes

## v2.0.1 (in development)
Built with Scala v2.13.18

## v2.0 - 15.03.2026
This update rewrote every class in this project. The main ideas are the same, but brought up-to-date. 
See the below list of changes for more information.
### Breaking changes
- `.path` in the (now deprecated) **http.Request** is now named `.pathOption`
- `.cookies` in **Request** is no longer a **Map**; the **Map** version is available as `.cookieMap`
- `.name` in **Body** is now **String** instead of an **Option**
- Some **Request** functions receive different implicit parameters
### Deprecations
- Deprecated and replaced the following request-related classes:
  - Replaced **http.Request** with a new version: **model.request.Request**
  - Replaced **Body**, **BufferedBody**, **StreamedBody** with the new **Request** version, 
    as well as the **StreamOrReader** class.
- Deprecated and replaced the following response-related classes:
  - Replaced **http.Response** with a new version: **model.response.Response**
  - Replaced **Result** with **RequestResult**
- Deprecated and replaced the following API node classes:
  - Replaced **Resource** with **ApiNode**
  - Replaced **ResourceWithChildren** with **NodeWithChildren**
  - Replaced **LeafResource** with **LeafNode**
  - Replaced **NotImplementedResource** with **NotImplemented**
  - Replaced **ItemsByIdResource** with **FindById**
  - Replaced **ModularResource** with **ModularApiNode**
  - Replaced **ExtendableResource** with **ExtendableApiNode**
  - Replaced **ExtendableResourceFactory** with **ExtendableApiNodeFactory**
  - Replaced **rest.scalable.UseCaseImplementation** with a new version: 
    **controller.api.node.extendable.UseCaseImplementation**
    - Note that the new version uses different parameter ordering and has different constructors
  - Replaced **rest.scalable.FollowImplementation** with a new version: 
    **controller.api.node.extendable.FollowImplementation**
- Deprecated and replaced the following request-handling classes:
  - Replaced **RequestHandler** with **ApiRoot**
  - Replaced **ResultParser** and all its subclasses with **ContentWriter**, **WriteResponseBody** and their subclasses
    - However, notice the different defaults in the generated property names, in the built-in content writers
  - Replaced **ResourceSearchResult** with **PathFollowResult**
    - Note the different user-interface, however
  - Replaced **Context** with **RequestContext**
  - Replaced **rest.PostContext** with a new version: **controller.api.context.PostContext**
  - Replaced **interceptor.RequestInterceptor** and **ResponseInterceptor** with **InterceptRequest** and 
    **controller.api.interceptor.RequestInterceptor**
- Instead of **Path**, a simple **Seq** is used now.
- Deprecated **ServerSettings** for removal
- Deprecated **FilesResource** for removal
### New features
- Added **ApiVersion** class, which is now yielded by **ApiRoot** for request context -creation
- **ApiRoot** instances can now be built incrementally, making API setup easier
- **RequestResult** now supports custom response-body writing, not just **Value**-based response bodies.
- Request body is now accessible as both **InputStream** and **BufferedReader** (via **StreamOrReader**), 
  allowing for wider streaming options
- **Response**s may now be streamed (asynchronously)
- **ContentWriter** and **InterceptRequest** are now specified when constructing **ApiRoot**
  - Previously **ResultParser** was defined in **Context**
  - Previously the interception logic was not setup in this module
- **PathFollowResult** is now simpler and easier to construct than its predecessor: **ResourceSearchResult**:
  - **Ready** is now an object; The responding **ApiNode** will always be considered the method execution target.
  - **Follow** no longer receives the remaining path; Only one step is taken with each call to **ApiNode**'s `.follow(...)`.
  - **Error** was replaced with **NotFound**, and no longer specifies the (failure) status; 
    **NotFound** always yields the 404 Not Found -status.
- The new request interception logic allows for a greater control at different points of request execution
- **Request** class can now represent a buffered state
- Added **ApiRequestCommand** class for performing/testing API requests locally

## v1.9.6 - 01.11.2025
A new build supporting **Flow v2.7**.

## v1.9.5 - 26.05.2025
This update adds support for **Access v1.6**

## v1.9.4 - 04.10.2024
A small update adding **RequestInterceptor** & **ResponseInterceptor** traits
### New features
- Added **RequestInterceptor** & **ResponseInterceptor** traits
  - These are supported in **Nexus-for-Tomcat**'s **ApiLogic** interface
### Other changes
- Built with Scala v2.13.14

## v1.9.3 - 28.07.2024
This update only contains some smaller changes to **Response**, as well as support for **Flow v2.4** updates.
### Deprecations
- Deprecated `.withModifiedHeaders(...)` in **Response** in favor of the new `.mapHeaders(...)`
### New methods
- **Response**
  - Added `.withStatus(Status)` and `.mapStatus(...)`
### Other changes
- In some instances where **Vector** was used, **Seq** is now used

## v1.9.2 - 22.01.2024
Supports **Flow v2.3**

## v1.9.1 - 27.09.2023
Rebuild due to parent module changes.
### Breaking changes
- Removed deprecated functions
### Other changes
- Scala version updated to 2.13.12

## v1.9 - 02.02.2023
This update mostly reflects changes made in **Flow** v2.0, including similar naming and type updates.  
In addition to this, more convenience is added with the importing of **PostContext** and with the addition of 
**ItemsByIdResource**, two very practical features.
### Breaking Changes
- The message / description property / parameter in **ResourceSearchResult.Error** and **Result** is now of 
  type **String** instead of **Option**. An empty string, not **None**, represents an empty message / description.
- Renamed the xml and json -based **ResultParser** implementations from ...XML and ...JSON to ...Xml and ...Json
### New Features
- Added **PostContext**, an abstract **Context** class that provides utility functions for post body processing
  - These functions were separated and moved to **Nexus** from the **Exodus** module
- Added **ItemsByIdResource** trait, which simplifies resource by id -accessing

## v1.8.1 - 02.10.2022
This update is mostly there to support changes introduced in **Flow** v1.17.
### New Methods
- **Request**
  - Added `.pathString`

## v1.8 - 18.08.2022
This update reflects changes in **Flow** v1.16, namely utilizing the new logging system.
### Breaking Changes
- **RequestHandler** now requires an implicit logger parameter within its constructor
### Other Changes
- **RequestHandler** now logs encountered errors (in addition to returning 505, like before)

## v1.7 - 06.06.2022
This update mostly concerns modular rest resources, refactoring the associated base traits. 
In addition, this update also includes some smaller quality-of-life improvements.
### Breaking Changes
- Modular resources were updated so that use case implementations no longer specify methods. Instead, use cases within 
  the modular resources are now stored in Maps.
### New Features
- Added **NotImplementedResource** trait (from **Exodus**)
### Other Changes
- Added default value `Value.empty` to `Result.Success(...)` constructor

## v1.6.2 - 27.01.2022
This update introduces an important bugfix concerning modular resources
### Scala
This module now uses Scala v2.13.7
### New Methods
- **ExtendableResource**
  - Added `.addChild(=> Resource)`, which is a utility variation of `.extendWith(FollowImplementation)`
### Bugfixes
- **Important**: There was a logic error in **ModularResource** which applied the first implementation 
  regardless of its method. Current version properly filters by method used.

## v1.6.1 - 04.11.2021
Supports changes in Flow v1.14

## v1.6 - 13.7.2021
This is a major upgrade on the **Nexus** module. Most importantly, the **RequestHandler** now supports 
versioning. Secondly, this update adds support for extendable resources, which is utilized in the 
**Utopia Exodus** project, for example.
### Breaking Changes
- **RequestHandler** was rewritten to support versioning
- **ResourceSearchResult.Ready** now takes the ready resource as the first parameter
  - Also, all **ResourceSearchResult** types now use type parameter C for context.
### New Features
- **RequestHandler** now supports versioning (using different resources on different API versions)
- Added **ModularResource** trait and abstract **ExtendableResource** class that support 
  custom implementations with **UseCaseImplementation** and **FollowImplementation**
  - Also added **ExtendableResourceFactory** class that allows extensions on resource classes that take parameters
- Added new **Result** type: **Redirect**
### Other Changes
- **Request** now contains public constructor parameter `.created` 
  which holds the creation time of that **Request**
- **RequestHandler**'s type parameter **C** is now contravariant (**-C**)

## v1.5.1 - 17.4.2021
This small update adds utility traits to make Rest resource implementation easier.
### New Features
- Added **ResourceWithChildren** and **LeafResource** traits to make Rest resource 
  implementation easier

## v1.5
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Renamed NoOperation result to NotModified
### Deprecations
- StreamedBody.bufferedJSON, .bufferedJSONModel and .bufferedJSONArray were deprecated in favor of new 
.bufferedJson, .bufferedJsonObject and .bufferedJsonArray. The new implementations take an implicit 
JsonParser, so they are no longer locked to JSONReader only.
