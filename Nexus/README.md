# Utopia Nexus
*Rest API -creation made easy*

## Main Features
Server side models for http requests and responses
- Supports both streamed and buffered responses
    - For example, buffered responses are used when responding with a JSON model while streamed responses
    are better suited for responding with raw file data.

Support for restful server architecture
- RequestHandler (utopia.nexus.http), along with custom Resources (utopia.nexus.http) handle traversal
in hierarchical resource structures.
    - NotFound (404) and MethodNotAllowed (405) are also handled automatically
    - You can add individual feature implementations as resources either directly under the
    RequestHandler or under another resource.

Envelopes inside the restful architecture
- The resource-generated results are automatically wrapped in envelopes using the method you prefer (optional feature).
    - You can choose between JSON or XML and whether you wish to wrap the result in an envelope or return it raw.
    - You can also define custom envelopes
    - Preferred settings are passed as implicit parameters
    
## Implementation Hints

### What you should know before using Nexus
If you're creating a REST-server, you will most likely need to create an **implicit ServerSettings** instance, and a
**RequestHandler** that uses a **Context** instance you specify (you can use **BaseContext** if you don't need custom
functionality).

### You should get familiar with these classes
- **RequestHandler** - your main interface when creating REST-servers
- **Resource** - All of your custom REST-nodes should extend this trait
- **ResourceSearchResult** - You will need this enumeration in your **Resource** implementations.
- **Result** - In REST-context, you normally specify operation result (Success, Failure, etc.) with **Result**. 
  You can convert a **Result** to a **Response** by calling .toResponse
- **Request** - You will need information from **Request** when forming a **Response** or a **Result**