# Utopia Nexus for Tomcat

## Main Features
Conversion from utopia.nexus.http.Response to javax.servlet.http.HttpServletResponse using Response.update(...)
    - Requires importing utopia.nexus.servlet.HttpExtensions._

Conversion from javax.servlet.http.HttpServletRequest to utopia.nexus.http.Request using HttpServletRequest.toRequest
    - Requires importing utopia.nexus.servlet.HttpExtensions._

An Echo server implementation (utopia.nexus.test.servlet.EchoServlet.scala) for creating a simple test server

## Implementation Hints

### Required external libraries
**Nexus for Tomcat** requires **servlet.api**.jar in order to work. You can acquire one from your
Tomcat's lib directory. Also, when using Tomcat with scala, you will need to add **Scala library**
and **Scala reflect** -jars to either Tomcat lib directory or into your webapp's lib directory.

### What you should know before using Nexus for Tomcat
Tomcat will require you to implement a sub-class of javax.servlet.http.**HttpServlet**. In your servlet class,
please import utopia.nexus.servlet.**HttpExtensions**._

There's an example implementation at utopia.nexus.test.servlet.**EchoServlet**, 
and a test web.xml file "web-example.xml".

By default, Tomcat doesn't support the http **PATCH** method, but you can work around this by overriding .service(...)
method in HttpServlet.

In your servlet implementation, you will first need to convert the **HttpServletRequest** to a **Request** by calling
.toRequest. When you've successfully formed a **Response**, you can pass it to tomcat by calling .update(...) on that
response.

If you want to support multipart requests, please add the following annotation over your servlet class definition:

    @MultipartConfig(
            fileSizeThreshold   = 1048576,  // 1 MB
            maxFileSize         = 10485760, // 10 MB
            maxRequestSize      = 20971520, // 20 MB
            location            = "D:/Uploads" // Replace this with your desired file upload directory path (optional)
    )