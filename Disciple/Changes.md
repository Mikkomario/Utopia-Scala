# Utopia Disciple - List of Changes

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