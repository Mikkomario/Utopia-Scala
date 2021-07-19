# Utopia Exodus

## Parent Modules
- Utopia Flow
- Utopia Vault
- Utopia Access
- Utopia Nexus
- Utopia Metropolis
- Utopia Citadel

## Main Features
Ready server structure for user management, localization and authorization
- Existing REST nodes implement all the main features of a user-management server
- The existing REST nodes also support custom extensions

Support for email validation / two-step authentication
- Foundation for adding your own implementation of email validation 
  (this project doesn't provide a concrete implementation, however)

## Implementation Hints
For REST API interface details, please refer to [API Documentation]

Please insert the associated database structure in this project and in **Utopia Citadel** 
into your database in order to use this project.

Before using Exodus, you must call `ExodusContext.setup(...)` method.
- This removes the need to call `CitadelContext.setup(...)` separately

You will need to add **ExodusResources** values to your **RequestHandler** when setting up the server.



### Classes you should be aware of
- **AuthorizedContext** - You may use this context in your **Resource** implementations to make sure incoming 
  requests are properly authorized.
- **EmailValidator** - If you want to implement two-step authentication / email validation, implement this trait

[API Documentation]: https://documenter.getpostman.com/view/2691494/TVmPAx1m