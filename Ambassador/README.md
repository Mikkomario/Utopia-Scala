# Utopia Ambassador
*OAuth can be tough to implement - What if someone did it for you?*

## Parent Modules
- Utopia Flow
- Utopia Vault
- Utopia Access
- Utopia Disciple
- Utopia Nexus
- Utopia Metropolis
- Utopia Citadel
- Utopia Exodus

## Main Features
Includes all server-side features for proper OAuth with 3rd party systems (except account creation). 
This includes:
- Full database structure and interface for service-specifications, 
  authentication tokens and authentication attempts
- Tracks authentication requirements for each task and targets authentication for individual tasks at a time
  - Supports alternative scopes for tasks (i.e, when any of a set of scopes is able to perform some task)
- Automatically refreshes the access token when the initial access token has expired and 
  there is a refresh token available. This means that the authentication process only needs to be completed once.
- Authenticates and tracks all phases of an OAuth process separately in order to prevent 
  malicious use of this service
  - The system also registers initiated but never completed authentication attempts. These can be used 
    to improve user interaction in the client.
- Optionally this system supports authentications that are initiated from the 3rd party platform 
  (E.g. Zoom register button)
- Client side may specify custom state and custom redirection urls which are used when redirecting the user 
  back to the (web) client

At this time this module is intended to be used in contexts with a single server API and possibly multiple 
browser-based web clients. This version doesn't yet support **Journey** or desktop clients.  

This module initially targets Google and Zoom as 3rd party OAuth targets. 
Other services may require some tweaks in order to be fully supported. However, I made the base of this module 
with scalability in mind so that other services can be supported when necessary.
- If you need to use this module with some other OAuth service and need some changes, please contact me. 
  I'll be glad to help you.
  
## Implementation Hints
API documentation is yet to be written...  

When you use this module, please insert the database structure from **Citadel**, **Exodus** and this module. 
You will find the sql files under these modules from **sql** folders.  
Also, please use the **Citadel-Description-Importer** tool to import descriptions for the initial items. 
You will find the descriptions to import under **data** folders in **Citadel** and **Exodus**.

Please also follow the instructions in **Exodus** README. Namely: Call `ExodusContext.setup()` and 
add **ExodusResources** to your **RequestHandler**.

From this module, you should call `ExodusTaskExtensions.apply()` and add a **ServicesNode** instance 
to your **RequestHandler**.
- In order to set up **ServicesNode**, you will need to construct at least one **AuthRedirector** 
  implementation (or Use **DefaultRedirector** (Zoom) or **GoogleRedirector**).
- You will also need to construct at least one **AcquireTokens** implementation. For example, between 
  Zoom and Google, there are slight variations that can be handled by passing different constructor parameters.
  - Please also be aware that in order to construct an **AcquireTokens** instance, 
    you need a **Gateway** instance. See **Utopia Disciple** for more details about that.
      - You can usually share a single **Gateway** instance between multiple **AcquireTokens** instances. 
        You need to create multiple instances only when the 3rd party services use different query parameter 
        encoding options.
        
In your actual database, you need to set up (insert) the contents of the following tables:
- **oauth_service**: List your accessed 3rd party services (like Google and/or Zoom) here
- **oauth_service_settings**: All services should have a single set of settings
- **scope**: List here the scopes that you need to access in the 3rd party services
- **task_scope**: List here which scopes are required in which task
- **scope_description** (optional): Optionally, you may describe the scopes that you have defined. 
  These descriptions will be included in responses towards clients.
        
The authentication process from the client's point of view goes like this:
1) Test if you have access to the desired feature with GET tasks/{taskId}/access-status
    - The response will show if you're already authorized and which services you need to access
    - If you're already authorized, skip to part 4
2) Send a session-key authorized POST request to services/{service id or name}/auth/preparations
    - The client will receive an authentication token in the response
3) Direct the user to services/{service id or name}/auth and include the authentication token as 
   *token* -query parameter
    - The user will be redirected back to the client once the process completes. You can specify redirection 
      rules in the preparation phase (2)
4) Use the feature which required 3rd party authentication
    
On the server-side in part 4, you need to acquire a valid access token using a **AcquireTokens** instance.  
This module doesn't provide a direct interface to the 3rd party systems besides authentication, but 
you may use **Utopia Disciple** module for the remaining http queries.