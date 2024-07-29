# Utopia Ambassador
*OAuth can be tough to implement - What if someone did it for you?*

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple)
- [Utopia Nexus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Nexus)
- [Utopia Metropolis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Metropolis)
- [Utopia Citadel](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel)
- [Utopia Exodus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Exodus)

## Main Features
Includes all server-side features for proper OAuth with 3rd party systems (except account creation). 
This includes:
- Full database structure and interface for service specifications, 
  authentication tokens and authentication attempts
- Tracks authentication requirements for each task and targets authentication for individual tasks at a time
  - Supports alternative scopes for tasks (i.e, when any of a set of scopes is able to perform some task)
- Automatically refreshes the access token when the initial access token has expired and 
  there is a refresh token available. This means that the authentication process only needs to be completed once.
- Authenticates and tracks all phases of an OAuth process separately in order to prevent 
  malicious use of this service
  - The system also registers initiated but never completed authentication attempts. These can be used 
    to improve user interaction in the client.
- Optionally, this system supports authentications that are initiated from the 3rd party platform 
  (E.g. Zoom's register button)
- The client-side may specify custom state and custom redirection urls which are used when redirecting the user 
  back to the (web) client

At this time this module is intended to be used in contexts with a single server API and possibly multiple 
browser-based web clients. This version doesn't yet support **Journey** or desktop clients.  

This module initially targets Google and Zoom as 3rd party OAuth targets. 
Other services may require some tweaks in order to be fully supported. However, I made the base of this module 
with scalability in mind so that other services can be supported when necessary.
- If you need to use this module with some other OAuth service and need some changes, 
  please contact me so that we can collaborate on its implementation.
  
## Available Extensions
- [utopia.ambassador.database.AuthDbExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Ambassador/data/backup/database/AuthDbExtensions.scala)
  - Adds authentication-related methods to individual user and task access points
- [utopia.ambassador.rest.resource.extensions.ExodusTaskExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Ambassador/src/utopia/ambassador/rest/resource/extensions/ExodusTaskExtensions.scala)
  - Adds authentication checking node under individual task nodes
  
## Implementation Hints
API documentation is yet to be written...  

When you use this module, please insert the database structure from **Citadel**, **Exodus** and this module. 
You will find the sql files under these modules from **sql** folders.  
Also, please use the 
[Citadel-Description-Importer tool](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel-Description-Importer) 
to import descriptions for the initial items. 
You will find the descriptions to import under **data** folders in **Citadel** and **Exodus**.

Links to data folders:
- [Citadel](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel/data)
- [Exodus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Exodus/data)
- [Ambassador](https://github.com/Mikkomario/Utopia-Scala/tree/master/Ambassador/data)

Please also follow the instructions in [Exodus README](https://github.com/Mikkomario/Utopia-Scala/tree/master/Exodus). 
Namely, call `ExodusContext.setup()` and add 
[ExodusResources](https://github.com/Mikkomario/Utopia-Scala/blob/master/Exodus/src/utopia/exodus/rest/resource/ExodusResources.scala) 
to your [RequestHandler](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/RequestHandler.scala).

From this module, you should call `ExodusTaskExtensions.apply()` and add a 
[ServicesNode](https://github.com/Mikkomario/Utopia-Scala/blob/master/Ambassador/src/utopia/ambassador/rest/resource/service/ServicesNode.scala) 
instance to your **RequestHandler**.
- In order to set up **ServicesNode**, you will need to construct at least one 
  [AuthRedirector](https://github.com/Mikkomario/Utopia-Scala/blob/master/Ambassador/src/utopia/ambassador/controller/template/AuthRedirector.scala) 
  implementation (or Use **DefaultRedirector** (Zoom) and/or **GoogleRedirector**).
- You will also need to construct an 
  [AcquireTokens](https://github.com/Mikkomario/Utopia-Scala/blob/master/Ambassador/src/utopia/ambassador/controller/implementation/AcquireTokens.scala) 
  instance, for which you need to specify service-specific configurations.
  - Please also be aware that in order to construct an **AcquireTokens** instance, 
    you need a [Gateway](https://github.com/Mikkomario/Utopia-Scala/blob/master/Disciple/src/utopia/disciple/apache/Gateway.scala) 
    instance. See [Utopia Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple) 
    for more details about this interface.
      - You can usually share a single **Gateway** instance between multiple 
      [TokenInterfaceConfiguration](https://github.com/Mikkomario/Utopia-Scala/blob/master/Ambassador/src/utopia/ambassador/model/cached/TokenInterfaceConfiguration.scala) 
      instances. 
        - You need to create multiple instances only when the 3rd party services use different query parameter 
        encoding options.
        
In your actual database, you need to set up (insert) the contents of the following tables:
- **oauth_service**: List your accessed 3rd party services (like Google and/or Zoom) here
- **oauth_service_settings**: All services should have a single set of settings
- **scope**: List here the scopes that you need to access in the 3rd party services
- **task_scope**: List here which scopes are required in each task, where applicable
- **scope_description** (optional): Optionally, you may describe the scopes that you have defined. 
  These descriptions will be included in responses to your client applications.
        
The authentication process from the client's point of view goes like this:
1) Test if you have access to the desired feature with `GET tasks/{taskId}/access-status`
    - The response will show if you're already authorized and which services you need to access
    - If you're already authorized, skip to part 4
2) Send a session-key authorized POST request to `services/{service id or name}/auth/preparations`
    - The response will contain an authentication token
3) Direct the user to `services/{service id or name}/auth` and include the authentication token as 
   `token` -query parameter
    - The user will be redirected back to the client once the OAuth process completes. You can specify redirection 
      rules in the preparation phase (2)
4) Proceed to access and use the feature for which you requested 3rd party authentication
    
On the server-side in part 4, you need to acquire a valid access token using a **AcquireTokens** instance.  
This module doesn't provide a direct interface to the 3rd party systems besides authentication, but 
you may use **Utopia Disciple** module for the required http queries.