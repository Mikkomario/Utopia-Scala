UTOPIA ACCESS   --------------------------------

Required Libraries
------------------
    - Utopia Flow

Purpose
-------

    Utopia Access contains general http/https -related tools and models that are used in both client- and server side.
    Access is built without any dependencies to other libraries. Those are only added by specific sub-projects.

Main Features
-------------

    Headers as an immutable structure
        - Instead of having to remember each individual header field name, you can use computed properties for reading
        and simple methods for modifying the most commonly used headers.
        - Date time, content type, etc. headers are automatically parsed for you
        - Headers class still allows use of custom- or less commonly used headers with strings
        - Headers have value semantics

    Enumerations for common http structures
        - Http methods are treated as objects and not strings
        - Http status codes and -groups
            - You don't need to remember all http status codes by heart. Instead you can use standard enumerations like
            Ok, NotFound, BadRequest and so on (utopia.access.http.Status).
        - Easy handling of web content types
            - Instead of handling content types as strings, you can use simple enum-like structure
            - Includes many of the commonly used content types from the get-go

Usage Notes
-----------

    Like with Flow, if you wish to use typeless values, you need to call utopia.flow.generic.DataType.setup() on
    program startup.


v1.1.2  ------------------------------------------

    Updates & Changes
    -----------------

        isEmpty & nonEmpty methods added to Headers

        basicAuthorization -property added to Headers


v1.1.1    ---------------------------------------

    Updates & Changes
    -----------------

        PATCH method added

        Supports Flow v1.6


    Required Libraries
    ------------------
        - Utopia Flow v1.6+


v1.1    -----------------------------------------

    Updates & Changes
    -----------------

        Headers class extended
            - Added multiple new utility methods to headers
                - Including basic authentication support

        Status, StatusGroup, Method and ContentCategory implementations are now all under their companion objects
            - This means that you need to, for example, import utopia.access.http.Status.OK instead of
            utopia.access.http.OK

    Required Libraries
    ------------------
        - Utopia Flow 1.5+