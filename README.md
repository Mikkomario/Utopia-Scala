# Utopia
*Makes programming easy while still letting you dive deep*

## Requirements
You will only need **Scala sdk v2.13** and **Java jdk 8** to use *Utopia*. Some specific modules may 
require additional libraries but most of *Utopia* is built on Scala and Java source code.

### Why Scala?
Based on my personal programming experience, I've found Scala to be the most scalable and 
flexible language and most suited for this kind of generic framework and **core philosophy**.

*Utopia* relies heavily on strong typing and generics and benefits greatly from implicits. 
Based on my current experience, only Java, Scala and possibly Kotlin provide a suitable 
foundation (although I haven't used Kotlin yet). *Utopia* started out as a Java project, 
but I've found Scala to work better in every area.

This project version is built using Scala v2.13.12

## Core Philosophy
The main purpose of the Utopia library is two-fold:
1) I want programming to be both **easy** and **fast** for you
2) I want to allow you to operate on **higher abstraction levels** when you need it

### Why?
Many libraries and frameworks **feel awesome when you first start to use them**.   
I personally used GameMaker from a very early age, and I was really happy with how fast and easy it was to develop 
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

Finally, we have concrete implementations that are specific to the business case they exist in (this is what you 
normally deal with when programming).

The lower / more concrete you go on abstraction layers, the easier everything gets but the 
less freedom you have. Working on very high abstraction layers if often more difficult, and you 
have to write a lot of code yourself. Working on lower abstraction layers if often straightforward, 
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
providing you with an easy-to-use foundation.

The times I have opted to use external libraries (Utopia Disciple, Utopia Nexus for Tomcat), you can find out that 
most of the implementation code is in-fact independent of the libraries used. If you want to use different libraries, 
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
- Utopia is quite a large set of libraries at this point, and it uses its own data structures and approaches a lot. 
  If you use Utopia, it will likely affect your code in lots of areas, which makes it more difficult to transition 
  away from using it.
- At the moment **I develop *Utopia* by myself**. This means that everything takes time to develop. 
If you need a specific area covered or a feature developed you may contact me or **contribute**
    - I don't have a testing team or a quality assurance team to make sure everything works smoothly. 
    I test new features and fix the bugs as I encounter them.
    - In case I was to quit developing this library (not intending to), you would have to perform some updates yourself.
- Historically, many *Utopia* modules have undergone **major changes** over time. When I learn new ways of doing things 
and my skills develop, I often revisit and revision my old code. This means that if you wish to stay up-to-date with 
the library, you will have to deal with some compile errors caused by changes in the library.
- Your specific business case may have some requirements this library doesn't cover 
(E.g. advanced information security or code efficiency in terms of speed). In these cases you should 
consider either using another library or helping me overcome the issue.
- At the time of writing, *Utopia* has very few resources in form of tutorials and instructions. I intend 
to remedy this in the future and I would appreciate it if you would contact me for more information.

One thing you should also consider is that 
**I've often prioritized readability and ease-of-use over speed and memory optimization**. I believe that, in general, 
our hardware will continue to advance at a much faster rate than our 
programming capacity and skill. If you specifically need high-level performance and optimization to meet a specific 
business need (E.g. big data analytics), I recommend you to rely less on *Utopia* tools in that area. In less 
critical areas (performance-wise), I would recommend you to use these tools, however.

## High-Level Module Descriptions
*Utopia* project consists of a number of modules. Below is a short description of the purpose of each module.

### Utopia Flow
*It's the box of tools you want to take with you anywhere*  

**Utopia Flow** makes many things in Scala even easier than they are by default and offers some very useful data 
structures and data-related features. **Utopia Flow** is a standard building block and the 
**foundation for all the other Utopia modules**.  
**Flow** has the highest level of abstraction amongst all modules and can be used in both front and backend 
development in almost any kind of Scala project.  

**Flow** is a **standard inclusion for all of my projects** these days, 
whether they be servers, desktop clients, command-line apps or real-time games.

### Utopia BunnyMunch
*Speed and ease-of-use combined when it comes to json parsing*

**Utopia BunnyMunch** is a very simple model, meant to replace the inefficient JSONReader implementation from *Flow*. 
*BunnyMunch* uses a very fast *Jawn* json parsing library internally, but offers the same Value-based 
interface as the JSONReader.

### Utopia Courier
*Intuitive email interactions*

**Utopia Courier** model is focused on email integration and supports both sending and writing emails. 
This module wraps the JavaMail API, providing a much cleaner interface.

### Utopia Vault
*All the benefits of SQL - with no SQL required!*

**Utopia Vault** is a module specialized in **database interactions**. **Vault** offers you a number of abstraction 
levels to operate on when it comes to database interactions. I especially enjoy the model based approach where 
you don't need to write a single line of SQL. But unlike other noSQL-frameworks, this one actually still lets 
you operate directly on SQL when or if you need it.

#### Utopia Vault Coder
*Allows you to skip hours of work at project setup*

This **Vault** add-on helps you by generating much of the model and database interaction code you would
otherwise have to write manually, saving a lot of your time. If you're using **Utopia Vault** or any of the
dependent modules (especially if you're using **Citadel**), I highly recommend this tool. 
Please check the README file of this application for more details.

### Utopia Trove
*Embedded databases made easy*

**Utopia Trove** is a rather simple library that offers an easy-to-use interface 
to [Vorburger's MariaDB4j](https://github.com/vorburger/MariaDB4j) library 
(see ch.vorburger.mariaDB4j:mariaDB4j:2.4.0 in Maven) that is used for setting up a local embedded database 
directly from Java (in this case Scala) code. Most of the heavy lifting is done by the Vorburger's library, 
but **Trove** adds its own flavor: Database structure setup, version control and updates, 
as well as an easy one-line database setup and shutdown functions.

### Utopia Paradigm
*Shapes, colors and vector mathematics at your disposal*

**Utopia Paradigm** introduces a number of models that are often used in visual and/or mathematical software, 
such as points, bounds, polygons, matrices, affine transformations, colors and so on. 
While these models are most valuable in front-end UI applications, they may also be utilized in backend calculations.

The challenge with these kinds of models, previously, was that they were mostly available through AWT or some other 
Java library, built around mutability and all sorts of nasty stuff. **Paradigm** models, however, are 
all immutable and suitable for a Scala approach to front-end programming.

### Utopia Inception & Utopia Genesis
*Event-based user-interaction and visuals, quickly*

**Utopia Inception** and **Utopia Genesis** have their background in real-time 2D game development, although
they're by no means limited to such purposes and are most often used as a foundation for standard client side programs.

**Inception** allows you to easily distribute various types of events among a multitude of listeners that may come and
go during a program's runtime, a feature that is very often needed in interactive client side programs.

**Genesis** provides you with a powerful set of tools for building interactive visual software.
You have images, mouse events, keyboard events, real-time action events. Without **Genesis**, you would normally
have to rely on awt-tools, which are less flexible, less scalable, less functional and less easy to use.

The problem with **Genesis** and **Inception** is that the standard Swing framework isn't build upon them,
but that's why I've built **Utopia Reach** and **Utopia Reflection**.

### Utopia Firmament
*A solid foundation for building interactive GUIs with dynamic layout*

**Firmament** provides a set of generic traits and tools used in both of my component layout / GUI frameworks 
(i.e. **Reach** and **Reflection**). Since these are separate modules these days, the common features were 
gathered into this module.

I would suggest checking **Reach** and/or **Reflection** and then checking the README of this module, in case 
you find either of those modules interesting.

### Utopia Reach
*A GUI framework for those who REALLY hate Swing*

Over the time I've progressed from **1)** having to use the Swing framework and hating it, 
to **2)** building my own framework (**Reflection**) around the Swing framework, hating the limitations of Swing 
even more, to **3)** building a new framework (**Reach**) with as little AWT as possible - a huge amount of work to write, 
but absolutely worth it.

**Utopia Reach** uses only two heavily wrapped Swing components: 1) a JFrame or JDialog and 2) an empty JPanel. 
**Reach** implements its own focus system, event distribution system, layout system, you name it. 
It is heavily inspired by Swift's declarative GUI programming. All layout updates are automatized. Very rarely 
do you need to specify the exact sizes of components. Another major difference between Swing and Reach is that 
Reach fluently supports multi-threading (a feat that was supposed to be impossible with Swing, I learned).

One major principle which makes Reach superior to Swing is that all components and data structures are read-only by 
default and react to well-specified pointers. This makes it easier to track, who caused what change (and bug). 
This doesn't mean the resulting system will be simple. There tend to be a lot of interactive dependencies and 
different event-based actions when building GUIs.

Finally, **Reach** obviously doesn't have the same look and feel as Swing does. 
The built-in component designs are somewhat inspired by Material Design.
However, as with all the other *Utopia* modules, **Reach** allows you to design and implement your own style on 
top of the existing capabilities. The only requirement is that you can't really rely on Swing or Awt components, 
nor on the standard OS look-and-feel. If you would rather still use mostly Swing, consider using **Reflection** instead.

### Reach Coder
Reach Coder is a utility application for generating factory classes for your custom Reach components based on 
json definitions. This application is most suited for customizable components, 
like those introduced in the **Reach** library.

### Utopia Reflection
*An AWT-based GUI framework that actually works and does what you want it to do*

**Reflection** is a Swing-like GUI framework, where components are wrapped and managed through a 
well-defined (but open) interface. Swing dependencies are clearly separated from the generic models and traits. 
First of all **Reflection** lets you use the models from **Genesis**, also providing its own additions. 
Second, **Reflection** handles layout like it should have been handled 
(much like Swift handles layout with StackPanels and constraints).

There are a number of pre-existing component implementations (swing-based). Y
ou also have access to all the higher abstraction level interfaces, 
which lets you create your own components with relative ease, in case you want to.

At this time **Reflection** is getting overshadowed with the more recent **Reach** library. 
I would personally only recommend using **Reflection** on a new project if you still, for some reason, want to 
mix it with Swing layout.

### Utopia Reach in Reflection
This very small module (one file) enables you to use **Utopia Reach** inside the **Utopia Reflection** framework.

Earlier, **Reach** used to be a sub-feature of **Reflection**, but is now an independent module/system. However, 
in order to support existing projects where **Reach** and **Reflection** intertwine, there's this module.

### Utopia Conflict
*Advanced collision detection, yet simple interface*

**Utopia Conflict** lets you include collision checking and collision events in your program. Collision detection
is a required feature in most 2D games, but it's also one of the toughest to implement from scratch. I've done the
vector mathematics for you here and wrapped the logic in a familiar **Inception** style handling system.

You probably don't need to use **Conflict** in your normal business software, but if you happen to be creating a 2D
game or a physics-based software, this will most likely help you a lot in getting started.

### Utopia Access
*Single solution for both server and client*

**Access** contains http-related tools and models that are common to both client- and server-side. Wouldn't it be 
nice if you could, instead of learning two frameworks, just learn one? No more need for writing everything twice; 
Http status codes, content types, headers, etc. are now readily available for you in a very simple format.

**Access** leaves the implementation to its submodules: **Nexus** and **Disciple**. And, in case you wanted 
to create your own implementation, you can.

### Utopia Nexus
*Server architecture for those who don't like servers that much*

Have you ever wished you had a nice, up-to-standard **REST-API**, but you found the idea of building one quite 
overwhelming? **Nexus** makes API-setup so easy you'll be surprised; I surprise myself with it all the time. 
What you get is a good set of **quality data models** to work with, along with a way to create restful interfaces 
that let you **focus on you business logic** instead of focusing how to get the technology to work. 

Don't want to use REST-architecture? You don't have to! You can still create the interface with the same foundation 
and the same building blocks, any way you want to.

#### Utopia Nexus for Tomcat
**Nexus doesn't force you to use any specific server-implementation or library**, such as Tomcat. 
There's a submodule for **Nexus** called **Nexus for Tomcat** which lets you quickly set up **Nexus** 
over a Tomcat instance - and it's just a single file.

### Utopia Disciple
*Http client made easy and simple*

**Disciple** utilizes models from **Access**, and provides some of its own for client side http request 
and response handling. The solution is based on Apache HttpClient under the hood, but you don't need to 
understand that library in order to use **Disciple**. Basically **Disciple** allows you to make http requests 
with just basic understanding about http methods, statuses and some common headers.

In case you wish to use **Disciple** interface with some other http client library, contact me and I will separate 
the dependency - it's only a single file anyway.

### Utopia Annex
*A superpower for client-side request and resource management*

**Utopia Annex** goes two steps further than **Disciple**, by offering you advanced request and response models, 
as well as connection management. 
By utilizing **Annex**, your application won't suffer even when the server-connection breaks for a moment. 
**Annex** is your go-to client-side http interface solution when you need your application to work in environments where 
network connection is not 100% reliable. This module is actually in use at Helsinki-Vantaa airport in Finland, 
where time-critical operations must be performed 24/7 and all data needs to be reliably captured and delivered - 
even when the internet connection breaks.

I would recommend you to use **Annex** in cases where you have considerable data-interaction with your server. 
For individual requests, I would recommend you to stick with **Disciple**, since it's more light-weight.

### Utopia Scribe
The Scribe module provides a detailed logging system specifically designed for use-cases where both front and back end 
are implemented using Scala. Logging entries are delivered from the clients to the server, and are readable using 
a custom application.

Scribe allows you to deliver very detailed logging entries, which helps in debugging. 
This module is most suited for more complex projects, where local file logging is not enough.

### Utopia Metropolis
*Client-Server user management and authorization made simple*

**Utopia Metropolis** is the base module for both **Utopia Exodus** (server-side) and **Utopia Journey** (client-side) modules. 
**Metropolis** provides the common features required in both of these modules. The purpose of these modules is to give you a 
pre-built user-management and session-based authentication system for your server and optionally client also. 
I've noticed that these features need to exist in so many server applications that I decided to make this the 
one-time solution to this problem.

### Utopia Citadel
*A working user management database in minutes instead of in hours*

**Utopia Citadel** module extends the **Utopia Metropolis** module, implementing most of the database interactions. 
**Utopia Citadel** may be used in both server-side and client-side context. On server-side, please use **Utopia Exodus** 
that extends this module. On client-side, consider using **Utopia Trove** to set up a local database.

#### Utopia Citadel Description Importer
The **Citadel** module comes with this utility application for importing localized item descriptions into your database 
without difficult SQL operations. If you're utilizing **Utopia Citadel** or any of the dependent modules in 
your project, please make sure to check out the README file of this application for more details.

### Utopia Exodus
*Server-side foundation that lets you skip right to your business logic*

The main point of **Utopia Exodus** is to handle the cumbersome user management and user authentication, 
that belong to any valid server-side application - and to do it well.  
By using **Utopia Exodus**, you can set up the required systems very quickly and get to what only you can implement: 
Your application business logic.

### Utopia Ambassador
*Look! Someone did the whole OAuth process for you!*

**Utopia Ambassador** has one main goal: Handle OAuth server-side process properly, so that you don't have to. 
If you already have a **Utopia Exodus** server, this module is very easy to add on top of it; You 
will be able to skip tens of hours of work in dealing with 3rd party OAuth. 
**Ambassador** comes with all rest nodes towards both 
the web client and the 3rd party redirection, so you will only need to add your business logic.

### Utopia Terra
Utopia Terra provides tools for dealing with location and GPS data. If you find yourself in a situation 
where you need to use latitude-longitude information, Utopia Terra is your friend. 
This module allows you to easily convert angular latitude-longitude data into linear vector format, 
where transitions and distances are more reasonable to calculate. 

### Utopia Journey (incomplete)
*Leverage your Exodus server with a pre-built client interface*

Please note that this module is in very early development and not ready for production use.

**Utopia Journey** is the client-side interface to **Utopia Exodus** server, handling request authorization and 
local session management. Having a pre-built and easy-to-use interface for server interactions in your client 
takes you many steps ahead and saves you the trouble of worrying about a secure interface.

## Module Hierarchy
*Utopia* modules have following dependency-hierarchy. Modules lower at the list depend on those higher in the list.
- **Flow** - The standard library
    - **BunnyMunch** - Json parsing
      - **Scribe Core** - Detailed logging system (generic)
    - **Courier** - Email integration
    - **Vault** - MySQL integration
        - **Trove** - Hosting MySQL DB from within an application
    - **Inception** - Event delivery support
    - **Paradigm** - Vectors and shapes
        - **Genesis** - Images, text and drawing (also requires Inception)
            - **Reach** - Non-Swing UI
            - **Reflection** - Swing-Wrapping UI
              - **Reach in Reflection** (also requires Reach)
            - **Conflict** - Collision handling
        - **Terra** - Location and GPS
    - **Access** - Http base library
        - **Nexus** - Server-side http base library
            - **Nexus for Tomcat** - Nexus integration for Apache Tomcat
            - **Scribe Api** - Server-side logging implementation (also requires Scribe Core and Vault)
        - **Disciple** - Client-side http base library
            - **Annex** - Advanced client-side http interface
              - **Scribe Client** - Client-side logging implementation (also requires Scribe Core)
    - **Metropolis** - User management base library
        - **Citadel** (also requires Vault) - User-management DB base library
            - **Exodus** (also requires Nexus) - Server with user management
                - **Ambassador** (also requires Disciple) - OAuth support
        - **Journey** (also requires Annex) - Client-side user management / Exodus integration
           
Basically every other *Utopia* module is dependent from **Flow**. All http-related modules are dependent from 
**Access** and all 2D visual modules are dependent from **Paradigm**. **Nexus** is the base 
module for server-side operations while **Disciple** is the foundation of client-side server interactions. 
All user-management projects extend **Metropolis**.

Additional details about each module are listed in their own README files.