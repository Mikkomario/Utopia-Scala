# Utopia Ambassador - List of Changes

## v1.1 (in development)
### Breaking Changes
- **GoogleRedirector** is now a class and not an object, since it takes construction parameters.
  - Use `GoogleRedirector.default` instead of **GoogleRedirector** itself.
### New Features
- **GoogleRedirector** now supports two optional parameters: 
  1) Whether user should be prompted to select the applicable account and
  2) Whether user consent should be asked even if provided already
### New Methods
- **AuthUtils**
  - Added `testTaskAccess(taskScopes: Iterable[TaskScope], availableScopeIds: => Iterable[Int])`
- **DbUser** (single) (**AuthDbExtensions**)
  - Added `isAuthorizedForServiceTask(serviceId: Int, taskId: Int)`
- Added service ids access to user authentication token access point, as well as to 
  **DbUser** via **AuthDbExtensions**
### Bugfixes
- **AuthTokenWithScopesFactory** would previously fail upon target creation because of wrong join order.
  - Fixed by changing the primary table in **TokenScopeFactory**