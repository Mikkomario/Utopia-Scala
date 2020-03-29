UTOPIA DISCIPLE
---------------

Required Libraries
------------------
    - Utopia Flow
    - Utopia Access
    - Apache Http Core (4.4.9)
    - Apache Http Client (4.5.5)
    - Commons Codec (1.10)
    - commons Logging (1.2)

Purpose
-------

    Utopia Disciple is a tool for making http & https queries on client side. The interface is very easy to use and
    supports JSON and XML formatting from the get-go.

Main Features
-------------

    Simple Request and Response models
        - Immutable Requests make request creation very streamlined so that you don't need to "remember" to call
        additional methods after request construction.
        - Support for both streamed and buffered responses

    Singular interface for all request sending and response receiving
        - Gateway object wraps the most useful Apache HttpClient features and offers them via a couple of simple methods
        - Support for both callback -style as well as for Future style
        - Support for parameter encoding
        - Supports various response styles, including JSON and XML, as well as custom response styles or raw data
        - Supports file uploading

Usage Notes
-----------

    Before using Disciple, please call utopia.flow.generic.DataType.setup()

    You can modify various settings, including the maximum number of simultaneous connections per host, via GateWay
    object's public properties.


v1.1    ---------------------------------

New Features
------------

    Support for parameter encoding and response decoding (specified by the Content-Type header)

    A number of new interface methods in Gateway


Updates & Changes
-----------------

    Gateway method structure was improved. This will cause many conflicts in implementations using v1.


Required Libraries
------------------
    - Utopia Flow v1.6.1+
    - Utopia Access v1.1.2+
    - Apache httpcore-4.4.9
    - Apache httpclient-4.5.5
    - commons-codec-1.10
    - commons-logging-1.2