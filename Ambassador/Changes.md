# Utopia Ambassador - List of Changes

## v2.3 - 23.01.2025
A minor (although breaking) update reflecting changes in **Disciple**.
### Breaking changes
- Because of changes in **Disciple**, **AcquireTokens** now requires access to an implicit **Logger**

## v2.2.1 - 04.10.2024
An update accommodating changes in Vault
### Other changes
- Built with Scala v2.13.14
- Supports Vault v1.20

## v2.2 - 28.07.2024
Matching changes introduced in **Vault v1.19**
### Breaking changes
- **AcquireTokens** now requires an implicit **JsonParser** construction parameter
### Other changes
- In some instances where **Vector** was required, **Seq** is now used

## v2.1.6 - 22.01.2024
Supports **Flow v2.3**, **Vault v1.18** and **Annex v1.7**
### Other changes
- Scala version updated to 2.13.12

## v2.1.5 - 27.09.2023
Rebuild due to parent module changes

## v2.1.4 - 01.05.2023
This update supports **Vault v1.16**

## v2.1.3 - 2.2.2023
This update supports the changes introduced in **Flow** v2.0.

## v2.1.2 - 02.10.2022
Supports changes in **Flow** v1.17

## v2.1.1 - 18.08.2022
New build / supports changes in **Flow** v1.16

## v2.1 - 06.06.2022
This update reflects some changes in the **Exodus** v4.0 update, meaning that request authorization is not based on 
scopes. See the **AmbassadorScope** object.

## v2.0.1 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v2.0 - 04.11.2021
This update refactored the inner database interaction classes by utilizing the **Vault-Coder**. 
There are a number of breaking changes involved, although the functionality / the external interface 
should remain the same.

If you notice any issues with this release, please let me (Mikko) know. 
As it stands, this module hasn't been properly tested / been used in production yet.

## v1.2.1 - 18.10.2021
This small update simply supports **Citadel** v1.3 and **Vault** v1.11 changes

## v1.2 - 3.10.2021
This update reflects changes in **Utopia Flow**, using the new **Pair** class. 
### Breaking Changes
- `.insert(Seq) `in **TokenScopeLinkModel** now accepts **Pair**s instead of tuples
  - This shouldn't require much refactoring, if any

## v1.1 - 4.9.2021
This update contains important practical bugfixes and additions, the kind of things one finds out during early 
use case testing. **GoogleRedirector** interface was updated to support a wider range of features, 
making this a breaking update for Google OAuth users.
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
