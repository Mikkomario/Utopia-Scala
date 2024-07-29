# Utopia Exodus
**Exodus** provides server-side user-management, greatly facilitating authorization, for example.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Nexus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Nexus)
- [Utopia Metropolis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Metropolis)
- [Utopia Citadel](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel)

## Main Features
Ready server structure for user management, localization and authorization
- Existing REST nodes implement all the main features of a user-management server
- The existing REST nodes also support custom extensions

Support for email validation / two-step authentication
- Foundation for adding your own implementation of email validation 
  (this project doesn't provide a concrete implementation, however)
  
## Available Extensions
- [utopia.exodus.database.UserDbExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Exodus/src/utopia/exodus/database/UserDbExtensions.scala)
  - Adds new authentication-related methods to individual user access points

## Implementation Hints
For REST API interface details, please refer to [API Documentation]

Please insert the associated database structure in this project and in **Utopia Citadel** 
into your database in order to use this project.

You can find the database-related files from:
- [Citadel/data](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel/data)
- [Exodus/data](https://github.com/Mikkomario/Utopia-Scala/tree/master/Exodus/data)

Before using Exodus, you must call `ExodusContext.setup(...)` method.
- This removes the need to call `CitadelContext.setup(...)` separately

You will need to add [ExodusResources](https://github.com/Mikkomario/Utopia-Scala/blob/master/Exodus/src/utopia/exodus/rest/resource/ExodusResources.scala) 
values to your [RequestHandler](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/RequestHandler.scala) 
when setting up the server.

### Classes you should be aware of
- [AuthorizedContext](https://github.com/Mikkomario/Utopia-Scala/blob/master/Exodus/src/utopia/exodus/rest/util/AuthorizedContext.scala) - 
  You may use this context in your 
  [Resource](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/Resource.scala) 
  implementations to make sure incoming requests are properly authorized.
- [EmailValidator](https://github.com/Mikkomario/Utopia-Scala/blob/master/Exodus/src/utopia/exodus/util/EmailValidator.scala) - 
  If you want to implement two-step authentication / email validation, implement this trait

[API Documentation]: https://documenter.getpostman.com/view/2691494/TVmPAx1m