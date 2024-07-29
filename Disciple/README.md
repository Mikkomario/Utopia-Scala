# Utopia Disciple
**Disciple** is a client-side http library, wrapping
[Apache's HttpClient](https://hc.apache.org/httpcomponents-client-4.5.x/index.html).

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)

## Required external libraries
**Utopia Disciple** requires following jars from 
[Apache HttpClient]([Apache HttpClient](https://hc.apache.org/httpcomponents-client-4.5.x/index.html)). 
The listed versions (v5.1.2) are what I've used in development. 
You can likely replace them with later versions just as well.
- httpclient5-5.1.2.jar
- httpcore5-5.1.2.jar
- httpcore5-h2-5.1.2.jar
- commons-codec-1.15.jar
- slf4j-api-1.7.25.jar

These jars are available in this module's [lib](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple/lib) 
folder, also.

Apache HttpClient & HttpCore libraries are available under [Apache 2.0 license](https://www.apache.org/licenses/).  
[SLF4J](https://www.slf4j.org/index.html) is available under the [MIT license](https://www.slf4j.org/license.html).

## Main Features
Simple [Request](https://github.com/Mikkomario/Utopia-Scala/blob/master/Disciple/src/utopia/disciple/http/request/Request.scala) 
and [Response](https://github.com/Mikkomario/Utopia-Scala/blob/master/Disciple/src/utopia/disciple/http/response/Response.scala) 
models
- Immutable **Request**s make request creation very streamlined
- Support for both streamed and buffered responses

Advanced response-parsing interface via 
[ResponseParser](https://github.com/Mikkomario/Utopia-Scala/blob/master/Disciple/src/utopia/disciple/http/response/ResponseParser.scala)
- Supports both synchronous (i.e. blocking) and asynchronous (i.e. parallel or streamed) response-parsing

Singular interface class for all request sending and response receiving
- [Gateway](https://github.com/Mikkomario/Utopia-Scala/blob/master/Disciple/src/utopia/disciple/apache/Gateway.scala) 
  class wraps the most useful 
  [Apache HttpClient](https://hc.apache.org/httpcomponents-client-4.5.x/index.html) features and offers them 
  via a couple of simple methods
- Supports both callback -style and **Future**-style response-handling
- Supports parameter encoding
- Supports various response styles, including JSON and XML, as well as custom response styles and raw data
- Supports uploading of files and other streamed content

## Implementation Hints

### You should get familiar with these classes
- **Gateway** - This is your main interface for performing http requests and for specifying global request settings
- **Request** - You need to form a **Request** instance for every http interaction you do
- [BufferedResponse](https://github.com/Mikkomario/Utopia-Scala/blob/master/Disciple/src/utopia/disciple/http/response/BufferedResponse.scala) - 
  When you need to deal with server responses (status, response body, etc.)
- **ResponseParser** - When you need response-parsing logic beyond reading simple **Value** or **XmlElement** 
  response content.