# Nexus Test Server
A simple servlet implementation to use as an example, for http client testing, 
or for checking that your Tomcat setup is functioning correctly.

## Parent Modules
This implementation uses the following Utopia modules:
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Nexus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Nexus)
- [Nexus-for-Tomcat](https://github.com/Mikkomario/Utopia-Scala/tree/master/NexusForTomcat)

## Required External Libraries
This server implementation is built on top of [Apache Tomcat](https://tomcat.apache.org/), and uses its 
servlet-api.jar. You can find this jar from 
[Nexus-for-Tomcat/lib](https://github.com/Mikkomario/Utopia-Scala/tree/master/NexusForTomcat/lib).

## Main Features
API nodes for testing
- **EchoNode** (`api/{version}/echo`) returns information about the incoming request. 
  This is useful when testing http client libraries.
- **SlowNode** (`api/{version}/slow`) can be used for simulating slow or delayed request-handling

## Implementation Hints

### Deployment
In order to deploy this server locally or otherwise, do the following:
1. Set up **Tomcat** (standalone) - This can be as easy as extracting the Tomcat zip file
2. Set up this web application by moving this module's files to the webapps directory under Tomcat's home directory 
  (see the applied file structure below)
3. Start **Tomcat**
4. Test this server by firing a test request to `http://localhost:8080/<directry name>/api/v1/echo`. 
  You can use **Postman**, any browser, etc. Tomcat's default port is 8080, but you can customize that 
  in Tomcat's configuration files. `<directory name>` refers to the directory within `webapps` under which you 
  placed this module's files.
5. Once you're done, shut down the **Tomcat**

#### Deployed Files
Create a new directory under `webapps`. You can name it whatever you want. For example, use name `test`.  
Under this directory, create a directory called `WEB-INF`.

Under `WEB-INF`, create two directories: `classes` and `lib`.

Place the built .class files from this module, as well as from Flow, BunnyMunch, Access, 
Nexus and Nexus-for-Tomcat into the `classes` folder. Alternatively, you should be able to place these libraries as 
jar files into the `lib` folder.

Place scala-library and scala-reflect jars to the `lib` folder. Version 2.12.13 was used in development, 
but other 2.13 versions should work as well.

Finally, place the `web.xml` from the `data` directory to the `WEB-INF` folder.