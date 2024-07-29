# Utopia Access
**Access** is the common http module providing tools which are shared between server- and client-side software.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)

## Main Features
[Headers](https://github.com/Mikkomario/Utopia-Scala/blob/master/Access/src/utopia/access/http/Headers.scala) 
as an immutable data-structure
- Instead of having to remember each individual header field name, you can use computed properties for accessing, 
  plus simple methods for modifying the most commonly used headers.
- Datetime, content type, etc. headers are automatically parsed for you
- **Headers** class still supports custom- and less commonly used headers with a String-based interface

Enumerations for common http structures
- Http methods are treated as objects instead of strings
- Http status codes and -groups
  - You don't need to remember all http status codes by heart. Instead, you can use standard enumerations like
  **OK**, **NotFound**, **BadRequest** and so on (see 
  [utopia.access.http.Status](https://github.com/Mikkomario/Utopia-Scala/blob/master/Access/src/utopia/access/http/Status.scala)).
- A simple interface for web content types
  - Instead of handling content types as Strings, you can use a simple enum-like structure
  - Includes many of the commonly used content types out-of-the-box
    
## Implementation Hints

### You should get familiar with these classes
- [Method](https://github.com/Mikkomario/Utopia-Scala/blob/master/Access/src/utopia/access/http/Method.scala) - 
  It's good to understand the most common http methods and their functions
- [Status](https://github.com/Mikkomario/Utopia-Scala/blob/master/Access/src/utopia/access/http/Status.scala) - 
  If you're developing server-side applications, get to know the most common statuses. On client side,
  research into statuses your server is probable to use.
- **Headers** - Useful to know when you need to specify authentication or type of content, for example.