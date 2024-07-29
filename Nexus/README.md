# Utopia Nexus
A simple library for creating restful APIs on top of existing servlet libraries.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)

## Main Features
Server-side models for http requests and responses
- Supports both streamed and buffered responses
    - For example, buffered responses are used when responding with a JSON model while streamed responses
    are better suited for responding with raw file data.

Support for restful server architecture
- [RequestHandler](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/RequestHandler.scala), 
  along with custom [Resources](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/Resource.scala) 
  handle traversal in hierarchical resource structures.
    - NotFound (404) and MethodNotAllowed (405) cases are handled automatically
    - You can add individual feature implementations as **Resource**s either directly under the
      **RequestHandler** or under other **Resource**s.
    - Optionally, you can make your **Resource**s extendable with custom functionality from lower modules
      - This is typically the case only when you're developing libraries for others to use

Envelopes inside the restful architecture
- Optionally, the resource-generated results are automatically wrapped in envelopes using the method you prefer.
    - You can choose between JSON or XML and whether you wish to wrap the result in an envelope or return it raw.
    - You can also define custom envelopes
    - Preferred settings are passed as implicit parameters
    
## Implementation Hints
If you're creating a REST-server, you will most likely need to create an **implicit ServerSettings** instance, and a
**RequestHandler** that uses a [Context](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/Context.scala) 
instance you specify (you can use [BaseContext](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/BaseContext.scala) 
if you don't need custom functionality).

Note that this library doesn't specify the underlying servlet functionality 
(i.e. receiving the actual http request from the client, nor delivering the actual response). 
For this purpose we have the [Nexus-for-Tomcat](https://github.com/Mikkomario/Utopia-Scala/tree/master/NexusForTomcat) 
module, which is specific to the [Tomcat](https://tomcat.apache.org/) platform. However, you can just as well 
create your own wrapper for a server-side http library of your choice. For reference, 
check the **Nexus-for-Tomcat** module's source code (a rather short piece to study).

For reference, check the **Nexus-Test-Server**'s source code. 

### You should get familiar with these classes
- **RequestHandler** - your main interface when creating REST-servers
- **Resource** - All of your custom REST-nodes should extend this trait or one of its sub-traits.
- [ResourceSearchResult](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/ResourceSearchResult.scala) - 
  You will need this enumeration in your **Resource** implementations.
- [Result](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/result/Result.scala) - 
  In REST-context, you normally specify operation result (**Success**, **Failure**, etc.) 
  using the class **Result**. You can convert a **Result** to a **Response** by calling its `.toResponse` function.
- [Request](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/http/Request.scala) - 
  You will need information from **Request** when forming a **Response** or a **Result**