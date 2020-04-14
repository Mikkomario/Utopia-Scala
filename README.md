# Utopia
*Makes programming easy while still letting you dive deep*

## Requirements
You will only need **Scala sdk v2.13** and **Java jdk 8** to use *Utopia*. Some specific modules may 
require additional libraries but most of *Utopia* is built on Scala and Java source code.

### Why Scala?
Based on my personal programming experience, I've found Scala to be the most scalable and 
flexible language and most suited for this kind of a generic framework and **core philosophy**.

*Utopia* relies heavily on strong typing and generics and benefits greatly from implicits. 
Based on my current experience, only Java, Scala and possibly Kotlin provide a suitable 
foundation (although I haven't used Kotlin yet). *Utopia* started out as a Java project 
but I've found Scala to work better in every area.

## Core Philosophy
The main purpose of the Utopia library is two-fold:
1) I want programming to be both **easy** and **fast** for you
2) I want to allow you to operate on **higher abstraction levels** when you need it

### Why?
Many libraries and frameworks **feel awesome when you first start to use them**.   
I personally used GameMaker from a very early age and I was really happy with how fast and easy it was to develop 
simple programs.  

Unfortunately, when you eventually wish to **dive deeper** and access some advanced features like generics - 
usually in order to maximize the scalability and "dryness" of your code - 
**many frameworks simply don't let you do that**.  

When I was studying Swift 3, for example, I found the language to be very "swift" and flow easily and concisely.  
However, the moment when I wanted to access generics or covariance, the language boundaries became apparent.  
Apple developers had access to such features but not their customers.

That's why I want to give you the pleasurable experience of **smooth and easy programming**, 
while still **giving you access** to higher abstraction layers.

### What are abstraction levels?
Here's an example of abstraction layers:  
- **Layer 1:** There are universal (moral) principles, like what's good and what's bad, what creates good results and 
what creates bad results. Universal principles manifest themselves in a multitude of life areas.  
- **Layer 2:** We humans have observed these principles and have developed a set of laws and rules that help us 
live along these principles. These can be rather specific but still oftentimes leave space for interpretation.  
- **Layer 3:** Our everyday actions are then physical manifestations of our governing principles, rules and rituals. 
These are very specific and tangible.

The way this works in programming is that we have very abstract tools that cannot by themselves be used 
without extension but which crystallize the essence or the core of some feature. In Scala these are often traits.

We then have customizable components or tools that can be used as they are, but leave defining the specifics 
to the user. Due to their customizable nature, these tools can be used in various different contexts.

Finally we have concrete implementations that are specific to the business case they exist in (this is what you 
normally deal with when programming).

The lower / more concrete you go on abstraction layers, the easier everything gets but the 
less freedom you have. Working on very high abstraction layers if often more difficult and you 
have to write a lot of code yourself. Working on lower abstraction layers if often straightforward 
but you are limited to the built-in options.

In *Utopia*, I've made my best to provide you with these higher abstraction level solutions the way that's 
useful for you, while also acknowledging that you may come to a place where you wish to override some of my 
implementations. Let's say you're not so happy about how a specific UI component performs, you can write your own and 
replace my implementation with it - as long as you follow the very high level guidelines placed. The higher you want to 
customize, the more you will need to write yourself - but when you want, you can! In other words, you're not locked out from the 
higher abstraction layers when you need to edit them.

### Use of External Libraries
This abstraction level concept is also why **I've opted to use as few external libraries as possible**.  
I want to give **you** the freedom to **choose** which libraries suit you best while still 
providing you with a easy-to-use foundation.

For example, the JSON interface I've written is not very efficient. 
If you wish to use a standardized JSON interface instead, that's completely possible. And If you wish to utilize that 
with typeless values and other features offered by *Utopia*, you can do what I've done and write a similar interface 
yourself.

The times I have opted to use external libraries (Utopia Disciple and Utopia Nexus for Tomcat), you can find out that 
most of the implementation code is in-fact independent from the libraries used. If you want to use different libraries, 
you don't need to write that much code by yourself.

## Pros and cons of using Utopia
Many people, especially early on, have criticized me for taking on such a project and "reinventing the wheel".  
However, I've found this library to be of very much practical use to me and I honestly think it can be a 
blessing to you as well.

Here's why you should and should not use *Utopia*.  

**Pros:**
- Utopia tools have greatly improved **my programming efficiency** in terms of **speed** and **code quality**. 
There is so much work you get to skip by utilizing these simple tools.
- Usually when people **don't** use this library, they end up taking shortcuts that often lead to lower code quality. 
It takes a great deal of discipline to take the "high road" when you're faced with your urgent business requirements 
and when you're not specifically trying to create a scalable framework.
- Using *Utopia* has made my job a lot **easier**, to the extent that I can forget many of the tools 
I would otherwise need every day. For example, you don't really need to know SQL in order to use the **Vault** module 
and operate with databases.
- *Utopia* is designed to be relatively **beginner-friendly**. With Genesis, for example, you can create a visual test 
program with just a few lines of code.
- *Utopia* consists of a number of modules for different purposes and there's a **synergy effect** that takes 
place between these modules. I would say that usually frameworks don't play together as well as *Utopia* does, 
mainly because they focus on a rather limited area and therefore can't share lay a new foundation for the other tools. 

**Cons:** 
- **Utopia will spoil you**. Once you get used to the privileges you've acquired by using *Utopia*, it feels 
quite bad to work with areas and languages where you don't have those features available.
- At the moment **I develop *Utopia* by myself**. This means that everything takes time to develop. 
If you need a specific area covered or a feature developed you may contact me or **contribute**
    - I don't have a testing team or a quality assurance team to make sure everything works smoothly. 
    I test new features and fix the bugs as I encounter them.
    - In case I was to quit developing this library (not intending to), you would have to perform some updates yourself.
- Historically, many *Utopia* modules have undergone **major changes** over time. When I learn new ways of doing things 
and my skills develop, I often revisit and revision my old code. This means that if you wish to stay up-to-date with 
the library, you will have to deal with some compile errors caused by changes in the library.
- Your specific business case may have some requirements this library doesn't cover 
(Eg. advanced information security or code efficiency in terms of speed). In these cases you should 
consider either using another library or helping me overcome the issue.
- At the time of writing, *Utopia* has very few resources in form of tutorials and instructions. I intend 
to remedy this in the near future and I would appreciate it if you would contact me for more information.

One thing you should also consider is that **I've prioritized readability and ease-of-use over speed and memory 
optimization**. I believe that, in general, our hardware will continue to advance at a much faster rate than our 
programming capacity and skill. If you specifically need high-level performance and optimization to meet a specific 
business need (Eg. big data analytics), I recommend you to rely less on *Utopia* tools in that area. In less 
critical areas (performance-wise), I would recommend you to use these tools, however.

## High-Level Module Descriptions
*Utopia* project consists of a number of modules. Below is a short description of the purpose of each module.

### Utopia Flow
*A foundation that makes everything run smoothly*  

**Utopia Flow** makes many things in Scala even easier than they are and offers some very useful data structures 
and data-related features. **Utopia Flow** is the building block and **foundation for all the other Utopia modules**.  
**Flow** has the highest level of abstraction amongst all modules and can be used in both front and backend 
development in almost any kind of project. **Flow** is a **standard inclusion for all of my projects** these days, 
whether they be servers, desktop clients or real-time games.

### Utopia Vault
*All the benefits of SQL - with no SQL required*

**Utopia Vault** is a module specialized in **database interactions**. **Vault** offers you a number of abstraction 
levels to operate on when it comes to database interactions. I especially enjoy the model based approach where 
you don't need to write a single string of SQL. But unlike other noSQL-frameworks, this one actually still lets 
you operate directly on SQL when or if you need it.

### Utopia Access
*Single solution for both server and client*

**Access** contains http-related tools and models that are common to both client and server side. Wouldn't it be 
nice if you could, instead of learning two frameworks, just learn one? No more need for writing everything twice, 
http status codes, content types, headers etc. are now readily available for you in a very simple format.

**Access** leaves the implementation to it's sub-modules: **Nexus** and **Disciple**. And in case you want 
to create your own, you can.

### Utopia Nexus
*Server architecture for those who don't like servers*

Ever wished you had a nice, up-to-standard **REST-server** but you find the ordeal too intimidating? **Nexus** makes 
creating servers so easy you'll be surprised. I surprise myself with it all the time. What you get is a good 
set of **quality data models** to work with, along with a way to create restful interfaces that let's you **focus 
on you business logic** instead of focusing how to get the technology to work. Don't want to use REST-architecture? 
You don't have to! You can still create the interface with the same foundation and building blocks any way you 
want to.

#### Utopia Nexus for Tomcat
**Nexus doesn't force you to use any specific server-implementation**. There's a sub-module for **Nexus** called 
**Nexus for Tomcat** which let's you get started with Tomcat and **Nexus** - and it's just a single file.

### Utopia Disciple
*Http client made easy and simple*

**Disciple** utilizes models from **Access**, and provides some of its own for client side http request 
and response handling. The solution is based on Apache HttpClient under the hood, but you don't need to 
understand that library in order to use **Disciple**. Basically **Disciple** allows you to make http requests 
with just basic understanding about http methods, statuses and some common headers.

In case you wish to use **Disciple** interface with some other http client library, contact me and I will separate 
the dependency - it's only a single file anyway.

### Utopia Inception & Utopia Genesis
*A foundation for anything real-time and/or visual*

**Utopia Inception** and **Utopia Genesis** have their background in real-time 2D game development, although 
they're by no means limited to such purposes and are most often used as a foundation for standard client side programs.

**Inception** allows you to easily distribute various types of events among a multitude of listeners that may come and 
go during a program's runtime, a feature that is very often needed in interactive client side programs.

**Genesis** provides you with a powerful set of shapes and tools for doing everything visual, especially in 2D. 
You have images, animations, curves, 2D shapes, 3D vectors, affine transformations, mouse events, keyboard events, 
real-time action events - everything you need when doing anything visual. Without **Genesis**, you would normally 
have to rely on awt-tools, which are less flexible, less scalable, less functional and less easy to use.

The only problem with **Genesis** and **Inception** is that the standard Swing framework isn't build upon them, 
but that's why we have **Utopia Reflection**.

### Utopia Reflection
*A GUI framework that actually works and does what you want it to do*

I personally have a love-hate relationship with the Swing framework. On the other hand, it's one of the few 
GUI frameworks for Java/Scala. On the other hand, it consistently keeps frustrating me with its limitations, 
difficulty of use and by simply not working.

**Reflection** is a swing-like GUI framework that relies on Swing as little as possible. First of all **Reflection** 
let's you use the models from **Genesis**, also providing it's own addition. Second, **Reflection** handles layout 
like it should be handled (much like Swift handles layout with StackPanels and constraints). Third, **Reflection** 
has a built-in support for **your** own localization feature, if you wish to implement one.

There are a number of pre-existing component implementations.You also have access to all the higher abstraction 
level interfaces, which let's you create your own components with relative ease, in case you want to.

### Utopia Conflict
*Advanced collision detection, yet simple interface*

**Utopia Conflict** let's you include collision checking and collision events in your program. Collision detection 
is a required feature in most 2D games but it's also one of the toughest to implement from scratch. I've done the 
vector mathematics for you here and wrapped the logic in a familiar **Inception** style handling system.

You probably don't need to use **Conflict** in your normal business software, but if you happen to be creating a 2D 
game or a physics-based software, this will most likely help you a lot in getting started.

## Module Hierarchy
*Utopia* modules have following dependency-hierarchy. Modules lower at the list depend from those higher in the list.
- Utopia Flow
    - Utopia Vault
    - Utopia Access
        - Utopia Nexus
            - Utopia Nexus for Tomcat
        - Utopia Disciple
    - Utopia Inception
        - Utopia Genesis
            - Utopia Reflection
            - Utopia Conflict
           
Basically every other *Utopia* module is dependent from **Flow**. All http-related modules are dependent from 
**Access** and all 2D visual modules are dependent from **Inception** and **Genesis**.

## Main Features per Module
Below I've listed some individual features you may be interested in using, in case you decide to use these modules.

### Utopia Flow
    Typeless data handling
        - Value and Model classes offer a way to handle data without type information, conveniently converting between
        various supported types like Double, Int, String and Vector
        - This allows one to represent and handle typeless data like SQL or JSON values
    
    JSON and XML support & Integration with models and typeless values
        - Flow offers full support for JSON and XML parsing + writing that fully utilizes the power of the typeless values
        - Conversion between JSON and Model / Value is seamless and converts between supported types under the hood
        - Please note that the current implementation of JSON parser prioritizes accessibility over performance and is not
        most efficient at this time. You may wish to use another parser for very large json files where performance
        becomes an issue.
    
    Various data structures
        - Tree and Graph supports
        - Support for weakly referenced lists and pointer-like data structures
        - Mutable concurrent collections (Volatile, VolatileFlag, VolatileList and VolatileOption) may be used in
        multi-threaded environments where some data is shared between multiple threads.
    
    Tools for asynchronous programs
        - ThreadPool implementation for generating a scaling ExecutionContext
        - WaitUtils for synchronous waiting
        - Loop and Breakable traits for looping, asynchronous operations, as well as support for shutdown hooks
        - WaitFor -simplification of Future waiting available through extension
    
    Data Caching
        - utopia.flow.caching package contains various tools for caching individual or multiple pieces of data either
        temporarily or permanently, asynchronously or synchronously
    
### Utopia Vault
    Database connection and pooled connection handling
        - Using database connections is much more streamlined in Vault, which handles many repetitive and necessary
        background tasks like closing of result sets and changing database.
        - Query results are wrapped in immutable Result and Row classes, from which you can read all the data you need
        - Value insertion and statement preparation will be handled automatically for you
    
    SQL Statements with full support for typeless values and models
        - Vault uses Flow's Value and Model classes which means that all data types will be handled automatically
        under the hood.
    
    Template Statements that make database interactions much simpler and less prone to errors
        - Insert, Update, Delete, Select, SelectAll, SelectDistinct, Limit, OrderBy, MaxBy, MinBy and Exists statements
        - Easy to write conditions with Where and Extensions
        - You don't need to know specific syntax for these statements. All you need to know is what they do and
        in which order to chain them.
    
    Automatic table structure and table reference reading
        - Use Tables object to read table and reference data directly from the database
        - This means that you only need to update your database and all models will automatically reflect those changes.
        - Column names that use underscores '_' are converted to camel case syntax more appropriate for scala / java
        environments (eg. "row_id" is converted to "rowId")
    
    Advanced joining between tables using Join and SqlTarget
        - Once reference data has been set up (which is done automatically in Tables object), you can join tables
        together without specifying any columns or conditions. Vault will fill in all the blanks for you.
        - If you wish to manually specify joined columns, that is also possible
    
    Storable, Readable, Factory and Access traits for object-oriented database interactions
        - Storable trait allows you to push (update or insert) model data to database with minimum syntax
        - Readable trait allows you to pull (read) up to date data from database to your model
        - Mutable DBModel class implements both of these traits
        - Factory traits can be used for transforming database row data into your object models
            - You will be able to include data from multiple tables, if you wish. Simply use
            FromResultFactory or FromRowFactory.
        - Access traits allow you to create simple interfaces into database contents and to hide the actual sql-based
        implementation
        - These traits allow you to use a MariaDB / MySQL server in a noSQL, object-oriented manner
    
### Utopia Access
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
        
### Utopia Nexus
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
        
### Utopia Nexus for Tomcat
    Conversion from utopia.nexus.http.Response to javax.servlet.http.HttpServletResponse using Response.update(...)
        - Requires importing utopia.nexus.servlet.HttpExtensions._
    
    Conversion from javax.servlet.http.HttpServletRequest to utopia.nexus.http.Request using HttpServletRequest.toRequest
        - Requires importing utopia.nexus.servlet.HttpExtensions._
    
    An Echo server implementation (utopia.nexus.test.servlet.EchoServlet.scala) for creating a simple test server

### Utopia Disciple
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
    
### Utopia Inception
    Handler & Handleable traits for receiving and distributing events between multiple instances
        - Supports both mutable and immutable implementations
        - Mutable implementations allow temporary deactivation & reactivation of instances
        - Permanent deactivation of instances is available through Mortal and Killable traits
    
    HandlerRelay for handling multiple mutable Handlers as a program context for Handleable instances
    
    Filter classes for filtering incoming events

### Utopia Genesis
    2D Shapes with 3D Vectors
        - Greatly simplifies advanced vector mathematics
        - Scala-friendly Support for basic everyday tools like Point, Size and Bounds
        - More advanced shapes from Line to triangular and rectangular shapes to advanced 2D polygonic shapes
        - Affine transformations (translation, rotation, scaling, shear) that fully support all of these shapes
        - Vector projection fully supported with shape intersection and collision handling in mind
            - This feature is extended in the Utopia Conflict project
        - Typeless value support for the basic shapes
        - Supporting classes for velocity and acceleration included for movement vectors
    
    Advanced 2D graphics with Drawable trait
        - Easy painting of all 2D shapes
        - Real-time asynchronous drawing events dispatched through standardized handling system
        - Multi-perspective drawing with affine transformations and Camera trait
        - Easy to setup with Canvas and MainFrame
    
    Advanced Mouse and Keyboard events
        - Handleable and Handler design style
        - Both mutable and immutable implementations provided
        - Listeners can be configured to receive only certain subset of events
        - Separate handling of mouse movement, mouse buttons and keyboard buttons
        - Easy to setup with CanvasMouseEventGenerator and ConvertingKeyListener
    
    Real-time asynchronous action events
        - Non-locked frame rate while still keeping standard program "speed"
        - Won't break even when frame rate gets low (program logic prioritized over slowdown, customizable)
        - Simple to use with Actor and ActorHandler traits
        - Easy to setup with ActorLoop
    
    New Color representation
        - Built-in support for both RGB and HSL color styles
        - Various color transformations
        - Implicit conversions between different styles
        - Alpha (transparency) support
    
    Images and image processing
        - New Image class for immutable image representation
        - Image transformations for various image-altering operations
        - Full support for image scaling and resizing simply by using affine transformations at draw time
        - Strip class for handling animated images
        
    Animation support
        - Animation and TimedAnimation traits for creating new animations
        - AnimatedTransform and TimedTransform traits for animated mapping functions
        - Animator trait for real-time animation drawers
        - Concrete SpriteDrawer implementation for drawing animated images
        
### Utopia Reflection
    New UI component style
        - ComponentLike and it's sub-traits are based on Genesis shapes instead of their awt counterparts
        - Completely overhauled and more reliable non-awt event system that relies on Genesis events
        and Inception handling system
        - Completely scala-friendly interface

    Stacking layout framework
        - Stacking layout system is built to work completely independent from the awt layout system
        - Allows both top to bottom and bottom to top layout changes - your layout will dynamically adjust to changes
        in component contents and components will automatically place themselves within their parent component's bounds
        - Dynamic size specifications are easy to make by utilizing simple classes like StackLength, StackSize and
        StackInsets

    Localization support
        - All components that display text are built around localization
        - LocalString, LocalizedString, Localizer and DisplayFunction make handling localization easy
        - Localization is done mostly implicitly behind the scenes
        - You can skip all localization simply by defining an implicit NoLocalization localizer

    Custom drawing
        - Many of the components support custom drawing over their boundaries, which allows for animation,
        advanced borders, image overlays etc.
        - Custom drawers utilize easy to use Drawer class from Genesis

    Custom components
        - Buttons and Labels are provided from the get-go
        - Various easy to use containers like the Stack, Framing and SwitchPanel are also available for component layout
        - Pre-built Frame, Dialog and Pop-Up classes for various types of Windows
        - Advanced selection components like TabSelection, DropDown, SearchFrom and TextField make requesting user
        input simple
        - ScrollView and ScrollArea allow for 1D and 2D scrolling without a lot of know-how or code from your side

    Implicit component build context
        - Contextual constructor options in existing components allow you to skip repetitious style definitions by
        passing an implicit ComponentContext -instance instead
        - This makes standardized layout styles easy to implement and use

    Automatic container content management
        - ContentManager and it's subclasses handle displaying of item lists for you
        
### Utopia Conflict
    2D collision handling with Collidable, CollisionListener, CollidableHandler and CollisionHandler traits
        - Follows the Handleable + Handler design logic from Genesis and Inception
        - Both mutable and immutable implementations available
        - Supports advanced polygonic shapes from Genesis, as well as circles
        - Collision events provide access to collision (intersection) points as well as a minimum translation
        vector (MTV) which helps the receiver to resolve the collision situation (with translation, for example)
        
## Implementation Hints
Below are some hints that hopefully help you to use these modules in your own projects.

### Utopia Flow
#### What you should know before using Flow
When you use **Flow** or its sub-modules, you want to call utopia.flow.generic.**DataType.setup()** at the 
beginning or your App.

When you wish to instantiate typeless values, please enable implicit value conversions by importing 
**utopia.flow.generic.ValueConversions._**

#### Extensions you should be aware of
- utopia.flow.util.**CollectionExtensions**
    - Collection utility updates, like support for multimaps and optional return values instead of throwing
    indexOutOfBounds
- utopia.flow.util.**TimeExtensions**
    - Functional additions to java.time classes
    - Conversion between java.time.Duration and scala.concurrent.duration.Duration
    - Easy creation of scala.concurrent.duration.FiniteDuration by using numeric extension
- utopia.flow.async.**AsyncExtensions**
    - Utility updates to Future
- utopia.flow.generic.**ValueConversions**
    - Implicit conversions from value supported classes (Int, Double, String, Vector[Value], etc.) to Value
- utopia.flow.util.**AutoClose**
    - Provides consume and tryConsume methods for autocloseable instances (like streams, etc.).
    This does java's try-with style resource handling functionally.
- utopia.flow.util.**StringExtensions**
    - Utility methods for String
- utopia.flow.util.**FileExtensions**
    - A number of new scala-friendly methods for java.nio.file.Path
- utopia.flow.util.**NullSafe**
    - When working with Java classes, you sometimes need to perform null-checks. In case you do, you can simply 
    convert a possibly null value into an option by calling .toOption
    - You should only import this extension in contexts where nulls are being received from Java methods
    
#### You should get familiar with these classes
- **Value** - When you need to use attributes but you can't define their exact typing below Any
- immutable.**Model**[Constant] - When you need to group a number of values together to form an object
- **ThreadPool** - When you need an implicit ExecutionContext (you will find out when)
- **JSONReader** - When you need to parse a string or a file into a JSON value / object
- **XmlElement**, **XmlReader** & **XmlWriter** - When you need to deal with XML
- **Loop** - When you need to loop a function or a process in background
- **WaitUtils** - When you need to block / wait for a period of time
- **PointerWithEvents** - When you need a mutable value with property change events
- **StringFrom** & **LinesFrom** - When you need to read file or stream data into strings
- **TryCache** - When you need to cache function results that may fail

### Utopia Vault
#### Required external libraries
You will need an external JDBC driver implementation in your classpath for **Vault** to work. I usually use 
**MariaDB Java Client** myself.

#### What you should know before using Vault
Unless you're using a local test database with root user and no password, please specify connection settings
with **Connection**.settings = ConnectionSettings(...) or **Connection**.modifySettings(...)

The default driver option (None) in connection settings should work if you've added mariadb-java-client-....jar
to your build path / classpath. If not, you need to specify the name of the class you wish to use and make
sure that class is included in the classpath.

If you want to log errors or make all parsing errors fatal, please change **ErrorHandling**.defaultPrinciple.

#### Extensions you should be aware of
- utopia.vault.sql.**Extensions**
    - Allows you to use values (or value convertible items) as condition elements
    - Usually works in combination with utopia.flow.generic.**ValueConversions**
    
#### You should get familiar with these classes
- **ConnectionPool** - When you need to open new database connections
- **Connection** - When you need to use database connections (usually passed as an implicit parameter)
- **Tables** - When you need to refer to database tables
- **Result** & **Row** - When you read database query results
- **Table** & **Column** - When you need to deal with database tables and columns
- **Storable** & **StorableWithFactory** - When you're creating models for noSQL database interaction
- **StorableFactory**, **FromRowFactory**, **FromResultFactory**, etc. - 
When you need to read model data from the database
- **SingleModelAccess** & **ManyModelAccess** - When you need to create interfaces for 
reading model data from the database
- **Select**, **SelectAll**, **Update**, **Insert** & **Delete** - When you need to create SQL queries
- **Where** & **Limit** - When you need to limit SQL queries

### Utopia Access
####You should get familiar with these classes
- **Method** - It's good to understand the most common http methods and their functions
- **Status** - If you're developing server-side applications, get to know the most common statuses. On client side, 
research into statuses your server is probable to use.
- **Headers** - Useful to know when you need to specify authentication or type of content, for example.

### Utopia Nexus
#### What you should know before using Nexus
If you're creating a REST-server, you will most likely need to create an **implicit ServerSettings** instance, and a 
**RequestHandler** that uses a **Context** instance you specify (you can use **BaseContext** if you don't need custom 
functionality).

#### You should get familiar with these classes
- **RequestHandler** - your main interface when creating REST-servers
- **Resource** - All of your custom REST-nodes should extend this trait
- **ResourceSearchResult** - You will need this enumeration in your **Resource** implementations.
- **Result** - In REST-context, you normally specify operation result (Success, Failure, etc.) with **Result**. You can 
convert a **Result** to a **Response** by calling .toResponse
- **Request** - You will need information from **Request** when forming a **Response** or a **Result**

### Utopia Nexus for Tomcat
#### Required external libraries
**Nexus for Tomcat** requires **servlet.api**.jar in order to work. You can acquire one from your 
Tomcat's lib directory. Also, when using Tomcat with scala, you will need to add **Scala library** 
and **Scala reflect** -jars to either Tomcat lib directory or into your webapp's lib directory. 

#### What you should know before using Nexus for Tomcat
Tomcat will require you to implement a sub-class of javax.servlet.http.**HttpServlet**. In your servlet class, 
please import utopia.nexus.servlet.**HttpExtensions**._

There's an example implementation at utopia.nexus.test.servlet.**EchoServlet** 
and a test web.xml file "web-example.xml".

By default, Tomcat doesn't support the http **PATCH** method, but you can work around this by overriding service(...) 
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

### Utopia Disciple
#### Required external libraries
**Utopia Disciple** requires following jars from **Apache HttpClient**. The listed versions (v4.5) are what I've used in 
development. You can likely replace them with later versions just as well.
- httpclient-4.5.5.jar
- httpcore-4.4.9.jar
- commons-codec-1.10.jar
- commons-logging-1.2.jar

#### You should get familiar with these classes
- **Gateway** - This is your main interface for performing http requests and for specifying global request settings
- **Request** - You need to form a **Request** instance for every http interaction you do
- **BufferedResponse** - When you need to deal with server responses (status, response body, etc.)

### Utopia Inception
You will need to focus on inception mostly at the point where you are familiar with the handling system and want to 
create your own implementation. You will probably get quite far by simply utilizing the existing **Handler** 
implementations.

#### Classes you should be aware of
- **HandlerRelay** - Used for collecting and keeping track of multiple mutable **Handlers**
- **Handleable** - A common trait for most of the event-related receivers. You need to select between the mutable and 
the immutable implementation or build your own.

### Utopia Genesis
#### What you should know before using Genesis
You can get started quickly by utilizing **DefaultSetup** class. You can also create your own implementation of 
**Setup**, in case I would still recommend you to refer to **DefaultSetup** source code.

#### Extensions you should be aware of
- utopia.genesis.util.**Extensions**
    - Provides approximately equals -feature (~==) for doubles
    
#### You should get familiar with these classes
- **Vector3D** & **Point** - Your standard way of representing points in 2D and 3D space.
- **Size** & **Bounds** - These size representations are used especially often when dealing with UI components
- **Axis** & **Axis2D** - Many shapes and features refer to this enumeration
- **Transformation** - When you need to deal with affine transformations (translation, rotation, scaling, etc.)
- **Color** - When you need to deal with colors (replaces java.awt.Color)
- **Image** - When you need to draw images
- **Drawable** - Implement this when you need an object to be drawn in real-time
- **KeyStateListener** - Implement this when you wish to receive keyboard events
- **MouseButtonStateListener** - Implement this when you wish to listen to mouse button events
- **MouseMoveListener** - Implement this when you wish to listen to mouse move events
- **Actor** - Implement this when you need an object to receive real-time action or 'tick' events

### Utopia Reflection
#### Extensions you should be aware of
- utopia.reflection.shape.**LengthExtensions**
    - Allows you to generate **StackLength** instances simply by writing 4.upscaling, 2.downscaling, 8.upTo(10) etc.
- utopia.reflection.localization.**LocalString**
    - Allows one to skip localization for specific strings by calling .noLanguageLocalizationSkipped, 
    .local etc. on a string.
    
#### You should get familiar with these classes
- **SingleFrameSetup** - Lets you get started with your test App as smoothly as possible 
(replaces **DefaultSetup** from Genesis)
- **ComponentContextBuilder** & **ComponentContext** - You will most likely need these to specify common component 
creation parameters (eg. Font used). Seek for .contextual -constructors in components to utilize these.
- **Stack** - You go-to container when presenting multiple components together
- **StackLength**, **StackSize** & **StackInsets** - Basic building blocks for dynamic sizes used in most components
- **StackLayout** & **Alignment** - These enumerations are used for specifying content placement in **Stacks** and 
other components.
- **Font** - Replaces java.awt.Font
- **LocalizedString**, **Localizer** & **NoLocalization** - When you need to present localized text
- **TextLabel** - Along with other **Label** classes, these basic components let you draw text, images, etc.
- **TextButton** & **TextAndImageButton** - When you need to create interactive buttons
- **TextField**, **DropDown** & **Switch** - These, among other components, allow user to input data to your program.
- **Framing** - Very useful when you want to surround a component with margins. Often also works by calling 
.framed(...) on a component.
- **ScrollView** - When you need a scrollable view. Check **ScrollArea** when you need 2D scrolling.
- **Dialog**, **Frame** & **PopUp** - In case you need to display multiple windows during a program
- **Refreshable** - One of the many input traits that allows you to display content on a UI component.
- **ComponentLike** - All components extend this trait so you should at least know what it contains.
- **Stackable** & **CachingStackable** - In case you need to write your own components that support stack layout system.
- **CustomDrawer** - In case you need to implement your own custom drawer. It's useful to check the sub-classes as well.
- **ContainerContentManager** - When you need to present a changing list of items in a **Stack** or another container.

#### Example code to get you started
The following code-template is an easy way to get started with your App and tests:

    // Set up typeless values
    GenesisDataType.setup()

    // Set up localization context
    implicit val localizer: Localizer = NoLocalization // You can specify your own Localizer here

    // Creates component context
    val actorHandler = ActorHandler()
    val baseCB = ComponentContextBuilder(actorHandler, ...)

    implicit val baseContext: ComponentContext = baseCB.result

    val content = ... // Create your frame content here

    implicit val exc: ExecutionContext = new ThreadPool("<your program name>").executionContext
    new SingleFrameSetup(actorHandler, Frame.windowed(content, "<your program name>", Program)).start()

### Utopia Conflict
#### Extensions you should be aware of
- utopia.conflict.collision.**Extensions**
    - Enables collision checking methods in both **Polygonics** and **Circles**
    
#### You should get familiar with these classes
- **DefaultSetup** - This **Setup** implementation replaces **DefaultSetup** in Genesis (contains collision handling).
- **Collidable** - Extend this if you want other objects to collide with your class instance
- **CollisionListener** - Extend this if you want to receive collision events
- **CollisionHandler** - Used for checking collisions between items and for delivering collision events. You should 
have one in your **Setup**