# Utopia Access
*A foundation for anything http*

## Main Features
Headers as an immutable structure
- Instead of having to remember each individual header field name, you can use computed properties for reading
and simple methods for modifying the most commonly used headers.
- Date time, content type, etc. headers are automatically parsed for you
- Headers class still allows use of custom- or less commonly used headers with strings
- Headers have value semantics

Enumerations for common http structures
- Http methods are treated as objects and not strings
- Http status codes and -groups
    - You don't need to remember all http status codes by heart. Instead, you can use standard enumerations like
    Ok, NotFound, BadRequest and so on (see utopia.access.http.Status).
- Easy handling of web content types
    - Instead of handling content types as strings, you can use simple enum-like structure
    - Includes many of the commonly used content types from the get-go
    
## Implementation Hints

### You should get familiar with these classes
- **Method** - It's good to understand the most common http methods and their functions
- **Status** - If you're developing server-side applications, get to know the most common statuses. On client side,
  research into statuses your server is probable to use.
- **Headers** - Useful to know when you need to specify authentication or type of content, for example.