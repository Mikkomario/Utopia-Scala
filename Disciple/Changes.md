# Utopia Disciple - List of Changes
## v1.3 (Beta)
### Breaking Changes
- Divided http package into two sub-packages: request and response
- Created a new set of interaction methods to Gateway.
    - Removed success/failure request method. 
    - Deprecated other old methods.
    - New interface methods don't wrap all results in Try. Especially the value-based 
    buffered result methods are now more error-resilient.
### New Features
- You can now specify custom JsonParsers via Gateway.jsonParsers -attribute 
(check out the BunnyMunch module and JsonBunny for an alternative to JSONReader)
- Added timeout field to Request. Also, added global maximum timeouts to Gateway.