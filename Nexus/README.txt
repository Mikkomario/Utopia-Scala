UTOPIA NEXUS    --------------------------------

Required Libraries
------------------
    - Utopia Flow
    - Utopia Access

Purpose
-------

    Utopia Nexus is designed to be used as the standard structure for creating http/https servers. This project
    doesn't provide the http interface, however, only the structures and tools for creating a valuable service on
    top of such an interface.

    This means that you can use Nexus with various different http implementations, be it based on java sockets,
    apache or whatever. There's a separate sub-project that integrates nexus with Tomcat. This sub-project goes
    with the name "Nexus for Tomcat".

Main Features
-------------

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

    Support for envelopes when using restful architecture
        - The resource generated results are automatically wrapped in envelopes using the method you prefer.
            - You can choose between JSON or XML and whether you wish to wrap the result in an envelope or return it raw.
            - You can also define custom envelopes
            - Preferred settings are passed as implicit parameters

Usage Notes
-----------

    At program startup, call utopia.flow.generic.DataType.setup() so that you can use typeless values and JSON.

    In restful applications, remember to create implicit ServerSettings instance to use in your restful context.

    RequestHandler takes context as a parameter, you may use BaseContext or create your own.
        - For example, if you use Vault, you may wish to pass a database connection as a part of your context (optional).


v1.3    ---------------------

    New Features
    ------------

        ServerSettings now also include expected parameter encoding (none by default)

        StreamedBody now contains a variation of writeTo that accepts a java.nio.file.Path


    Updates & Changes
    -----------------

        ServerSettings no longer specify file upload location

        StreamedBody.writeToFile was deprecated

        Resource search results (Ready, Follow, Error, Redirected) were placed under ResourceSearchResult object.
        This will cause existing import statements to fail.
        Simply replace import utopia.nexus.<result> with import utopia.nexus.ResourceSearchResult.<result>

        toString added to Request

        RequestHandler now includes error messages in cases where a child resource cannot be found

        ResultParser implementations are now case classes


    Required Libraries
    ------------------
        - Utopia Flow v1.6.1+
        - Utopia Access v1.1.2+


v1.2    -----------------------

    Required Libraries
    ------------------
        - Utopia Flow v1.6+
        - Utopia Access v1.1.1+

    Updates & Changes
    -----------------

        Response and result now take values as parameters instead of models (when parsing JSON responses). This allows
        one to return model arrays, for example.

        Result.Failure can now be created using a non-optional message (but no headers)


v1.1    -----------------------

    Required Libraries
    ------------------
        - Utopia Flow v1.5+
        - Utopia Access v1.1+

    New Features
    ------------

        Enveloping support added

    Updates & Changes
    -----------------

        Restful tools refactored
            - Resources now work different

        Fixes to support the latest version of Access

        Minor utility updates to Response and other classes