# Utopia Nexus for Tomcat
Enables Nexus library on the [Apache Tomcat](https://tomcat.apache.org/) platform.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Nexus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Nexus)

## Required external libraries
**Nexus for Tomcat** requires [servlet.api.jar](https://github.com/Mikkomario/Utopia-Scala/tree/master/NexusForTomcat/lib) 
in order to work. You can acquire one from your
Tomcat's lib directory. Also, when using Tomcat with scala, you will need to add **Scala library**
and **Scala reflect** -jars to either Tomcat lib directory or into your webapp's lib directory.

## Main Features
Conversion from utopia.nexus.http.**Response** to **javax.servlet.http.HttpServletResponse** using 
`Response.update(...)`
    - Requires `import utopia.nexus.servlet.HttpExtensions._`

Conversion from javax.servlet.http.HttpServletRequest to utopia.nexus.http.**Request** using 
`HttpServletRequest.toRequest`
    - Requires `import utopia.nexus.servlet.HttpExtensions._`

Standard **HttpServlet** implementation / parent class (**LogicWrappingServlet**)
- Use **ApiLogic** to set up the servlet functionality very quickly

## Implementation Hints
Tomcat will require you to implement a sub-class of javax.servlet.http.**HttpServlet**. In your servlet class,
please import 
[utopia.nexus.servlet.HttpExtensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/NexusForTomcat/src/utopia/nexus/servlet/HttpExtensions.scala)._

Alternatively, extend 
[LogicWrappingServlet](https://github.com/Mikkomario/Utopia-Scala/blob/master/NexusForTomcat/src/utopia/nexus/servlet/LogicWrappingServlet.scala) 
and just implement its required functions. 
When combined with [ApiLogic](https://github.com/Mikkomario/Utopia-Scala/blob/master/NexusForTomcat/src/utopia/nexus/servlet/ApiLogic.scala), 
all you need is a [RequestHandler](https://github.com/Mikkomario/Utopia-Scala/blob/master/Nexus/src/utopia/nexus/rest/RequestHandler.scala) 
implementation.

For an example implementation, check out **Nexus-Test-Server**'s source code.

By default, Tomcat doesn't support the http **PATCH** method, but you can work around this by overriding `.service(...)`
method in **HttpServlet**. **LogicWrappingServlet** does this already. If you need to create a custom 
**HttpServlet** which uses **PATCH**, check **LogicWrappingServlet**'s source code.

In your custom servlet implementation, you will first need to convert the **HttpServletRequest** to a **Request** by calling
`.toRequest`. When you've successfully formed a **Response**, you can pass it to tomcat by calling `.update(...)` on that
response.

If you want to support multipart requests, please add the following annotation over your servlet class definition:

    @MultipartConfig(
            fileSizeThreshold   = 1048576,  // 1 MB
            maxFileSize         = 10485760, // 10 MB
            maxRequestSize      = 20971520, // 20 MB
            location            = "D:/Uploads" // Replace this with your desired file upload directory path (optional)
    )