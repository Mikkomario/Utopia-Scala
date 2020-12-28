# Utopia Disciple - List of Changes
## v1.3.1
### Deprecations
- Gateway.introduceStatus and .introduceStatuses are now deprecated. Status.introduce should be used instead.
### New Methods
- StringBody.type
  - Added .urlEncodedForm(...) method which can be used for creating url encoded forms as request bodies

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