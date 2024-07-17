# Utopia Disciple - List of Changes

## v1.7 (in development)
Refactoring response parsing
### Breaking changes
- Completely rewrote & reimagined the **ResponseParser** interface
- Multiple changes to **Gateway**
  - The constructors no longer accept json parsers or default encoding
    - Instead, many of the json-related helper functions now require an implicit **JsonParser** parameter
  - Changed the function parameter in the `.makeRequest(...)` `.makeBlockingRequest(...)` methods 
    to not cover situations where a response can't be acquired (in these cases the method is not called anymore)
- **StreamedResponse** now requires an additional construction parameter for closing related (parent) resources
- **Body** trait no longer requires the implementation of `charset` property. 
  Character set should now be applied directly to the `contentType` property.
### Bugfixes
- If the character set is already defined in the outgoing content type header,
  **Gateway** no longer adds a second copy of that parameter
### New features
- **ResponseParser**, **StreamedResponse** and **Gateway** now support asynchronous response parsing
### New methods
- **Gateway**
  - Added `.apply(Request)(ResponseParser)`
### Other changes
- In some instances where **Vector** was used, **Seq** is now used
- Refactored `.writeTo(OutputStream)` in **Body** to check whether the streams are buffered 
  and to apply buffering if necessary

## v1.6.3 - 22.01.2024
Supports **Flow v2.3**
### Breaking changes
- Removed functions that were deprecated at v1.3
### Other changes
- Scala version updated to 2.13.12

## v1.6.2 - 27.09.2023
Rebuild due to parent module changes

## v1.6.1 - 01.05.2023
This minor update adds a utility option for SSL trust store verification skipping.
### New Features
- Added `disableTrustStoreVerification: Boolean = false` parameter to **Gateway** constructors, allowing the user to 
  disable SSL trust store verification process entirely, when needed.
  - This is a workaround to a pesky "trustAnchors parameter must be non-empty" -error

## v1.6 - 02.02.2023
This update introduces request and response -interception, which enables more customization options. 
In addition, the **ResponseParser** class is updated with better error handling capabilities.
### Breaking Changes
- Many **ResponseParser** constructors now require an implicit **Logger** parameter in order to record parsing failures
### New Features
- Added support for request and response interceptors in **Gateway**
- Added the **AccessLogger** class, which may be placed as a request/response interceptor in order to log all 
  outgoing requests and their results
### New Methods
- Added a number of new constructors to **ResponseParser**, 
  and a number of new utility response-reading functions to **Gateway**

## v1.5.3 - 02.10.2022
This update mostly supports the changes introduced in **Flow** v1.17, 
but also contains a rather important bugfix.
### Bugfixes
- **Patch** method was previously converted to **Put** in some cases, fixed
### Other Changes
- **RequestRateLimiter** now handles thread interruptions, as well as JVM shutdowns, better

## v1.5.2 - 18.08.2022
New Build / Supports changes in **Flow** v1.16

## v1.5.1 - 06.06.2022
This update only introduces a single quality-of-life upgrade for specifying timeouts.
### New Methods
- **Timeout**.type
  - Added `.uniform(FiniteDuration)` -method (utility constructor)

## v1.5 - 27.01.2022
Updated Apache HttpClient dependency and added request rate limiting support
### Scala
This module now uses Scala v2.13.7
### Dependency Changes
- This project now uses Apache HttpClient v5.1.2 instead of v4.x.
  - This should only affect library dependencies, the interface remains the same
### Breaking Changes
- Introduced three new constructor parameters to **Gateway**
  - These should cause only minor errors, if any
### New Features
- Added **RequestRateLimiter** class that limits the number of simultaneous or consecutive actions
- **Gateway** now supports http client customization

## v1.4.3 - 04.11.2021
Supports changes in Flow v1.14

## v1.4.2 - 4.9.2021
This very minor update simply adds a new exception class for utility.
### New Features
- Added a **RequestFailedException** class

## v1.4.1 - 13.7.2021
No new features, only supports the latest (breaking) changes in the **Utopia Access** module.

## v1.4 - 17.4.2021
This update makes the **Gateway** interface more reliable and easier to use when you're dealing with 
multiple different interfaces. It will, however, require you to make small changes to your code.
### Breaking Changes
- **Gateway** is now multi-instance (class) instead of single instance (object).
    - **Gateway** settings (number of connections, time out, json parsing etc.) are now specified at instance 
      creation and can't be changed after that
    - This allows you to use different interfaces (E.g. those requiring parameter encoding and those forbidding it)
      without undesired side effects
- Parameter encoding and whether request body parameters are supported -flag are now defined in **Gateway** 
  and no longer in **Request**
    - This way you will need to define these settings only once
- Removed **Gateway** methods that were deprecated in v1.3 and v1.3.1
### New Features
- You can now specify in **Gateway**, whether json format should be allowed to be used in query (uri) parameters
  - The default option is *true*, I.e. to convert the values to json before adding them to the uri
  - By setting this option to *false*, you can remove quotation marks from around the string values

## v1.3.1
### Deprecations
- `Gateway.introduceStatus` and `.introduceStatuses` are now deprecated. 
  `Status.introduce` should be used instead.
### New Methods
- **StringBody**.type
  - Added `.urlEncodedForm(...)` method which can be used for creating url encoded forms as request bodies

## v1.3
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Divided http package into two sub-packages: request and response
- Created a new set of interaction methods to Gateway.
    - Removed success/failure request method. 
    - Deprecated other old methods.
    - New interface methods don't wrap all results in Try. Especially the value-based 
    buffered result methods are now more error-resilient.
- StreamedResponse implementations now need to accept an openStream function that returns 
returns an option in case of some empty responses.
### New Features
- You can now specify custom JsonParsers via Gateway.jsonParsers -attribute 
(check out the BunnyMunch module and JsonBunny for an alternative to JSONReader)
- Added timeout field to Request. Also, added global maximum timeouts to Gateway.
