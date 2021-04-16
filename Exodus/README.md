# Utopia Exodus

## Main Features
Ready server structure for user management, localization and authorization
- Existing database structure and REST nodes implement all the main features of user management server

Support for email validation / two-step authentication
- Foundation for adding your own email validation implementation (no implementation provided in this project)

## Implementation Hints
You will have to import db-structure-v1.sql contents to your database.

Before using Exodus, you must call **ExodusContext.setup(...)** method.

You will need to add **ExodusResources** values to your RequestHandler when setting up the server.

### Classes you should be aware of
- **AuthorizedContext** - You may use this context in your Resource implementations to make sure incoming 
  requests are properly authorized.
- **EmailValidator** - If you want to implement two-step authentication / email validation, implement this trait
- **Tables** - When you need to access or refer to tables inside your database, please use this object