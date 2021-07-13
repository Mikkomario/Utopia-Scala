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
providing you with an easy-to-use foundation.

The times I have opted to use external libraries (Utopia Disciple, Utopia Nexus for Tomcat), you can find out that 
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

One thing you should also consider is that **I've prioritized readability and ease-of-use over speed and memory **
**optimization**. I believe that, in general, our hardware will continue to advance at a much faster rate than our 
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

### Utopia BunnyMunch
*Speed and easy-of-use combined when it comes to json parsing*

**Utopia BunnyMunch** is a very simple model, meant to replace the inefficient JSONReader implementation. 
*BunnyMunch* uses a very fast *Jawn* json parsing library internally, but offers the same Value-based 
interface as the JSONReader.

### Utopia Vault
*All the benefits of SQL - with no SQL required*

**Utopia Vault** is a module specialized in **database interactions**. **Vault** offers you a number of abstraction 
levels to operate on when it comes to database interactions. I especially enjoy the model based approach where 
you don't need to write a single string of SQL. But unlike other noSQL-frameworks, this one actually still lets 
you operate directly on SQL when or if you need it.

### Utopia Trove
*Embedded databases made easy*

**Utopia Trove** is a rather simple library that offers an easy-to-use interface to Vorburger's MariaDB4j library 
(see ch.vorburger.mariaDB4j:mariaDB4j:2.4.0 in Maven) that is used for setting up a local embedded database directly from Java 
(in this case Scala) code. Most of the heavy lifting is done by the Vorburger's library, but **Trove** adds its own flavor: 
Database structure setup, version control and updates, as well as an easy one-line database setup and shutdown functions.

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
set of **quality data models** to work with, along with a way to create restful interfaces that let's you **focus **
**on you business logic** instead of focusing how to get the technology to work. Don't want to use REST-architecture? 
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

### Utopia Annex
*A superpower for client-side request and resource management*

**Utopia Annex** goes two steps further than **Disciple** by offering you advanced request and response models, as well as 
connection management. By utilizing **Annex**, your application won't suffer even when the server-connection breaks for a moment. 
**Annex** is your go-to client-side server interface solution when you need your application to work in environments where 
network connection is not 100% reliable. The module is actually in use at Helsinki-Vantaa airport in Finland where time-critical 
operations must be performed 24/7 and all data needs to be reliably captured and delivered.

I would recommend you to use **Annex** in cases where you have considerable data-interaction with your server. 
For individual requests, I would recommend you to stick with **Disciple** since it's more light-weight.

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
but that's why we have **Utopia Reflection** and **Utopia Reach**.

### Utopia Reflection
*A GUI framework that actually works and does what you want it to do*

I personally have a love-hate (mostly hate) relationship with the Swing framework. On the other hand, it's one of the few 
GUI frameworks for Java/Scala. On the other hand, it consistently keeps frustrating me with its limitations, 
difficulty of use and by simply not working.

**Reflection** is a swing-like GUI framework that provides a foundation for both Swing-reliant and **non-Swing** GUIs (see **Utopia Reach**). 
Swing dependencies are clearly separated from the generic models and traits. First of all **Reflection** 
let's you use the models from **Genesis**, also providing it's own addition. Second, **Reflection** handles layout 
like it should be handled (much like Swift handles layout with StackPanels and constraints). Third, **Reflection** 
has a built-in support for **your** own localization feature, if you wish to implement one.

There are a number of pre-existing component implementations (swing-based). You also have access to all the higher abstraction 
level interfaces, which let's you create your own components with relative ease, in case you want to.

### Utopia Reach
*A GUI framework for those who REALLY hate Swing*

Turns out even **Reflection** was subject to limitations caused by Swing-dependency, preventing full scalability. Therefore we have **Utopia Reach**, 
a framework that uses only a single Swing component. **Reach** has the same core principles as **Reflection**, but takes things even further 
by having it's own, non Swing-dependent component, focus, paint and other systems. **Reach** even supports multithreading, which was *apparently* 
supposed to be impossible.

**Reach** obviously doesn't have the same look and feel as Swing does. The existing component designs are somewhat inspired by Material Design. 
However, as with all the other *Utopia* modules, **Reach** allows you to design and implement your own style on top of the existing capabilities. 
The only requirement when using **Reach** is that you can't really rely on Swing or Awt components. If you would rather still use mostly Swing, 
consider using **Reflection** instead.

### Utopia Conflict
*Advanced collision detection, yet simple interface*

**Utopia Conflict** let's you include collision checking and collision events in your program. Collision detection 
is a required feature in most 2D games but it's also one of the toughest to implement from scratch. I've done the 
vector mathematics for you here and wrapped the logic in a familiar **Inception** style handling system.

You probably don't need to use **Conflict** in your normal business software, but if you happen to be creating a 2D 
game or a physics-based software, this will most likely help you a lot in getting started.

### Utopia Metropolis
*Client-Server user management and authorization made simple*

**Utopia Metropolis** is the base module for both **Utopia Exodus** (server-side) and **Utopia Journey** (client-side) modules. 
**Metropolis** provides the common features required in both of these modules. The purpose of these modules is to give you a 
pre-built user management and user session system for your server and optionally client also. I've noticed that these features 
need to exist in so many server applications that I decided to make this the one-time solution to this problem.

### Utopia Citadel
*A working user management database in minutes instead of hours*

**Utopia Citadel** module extends the **Utopia Metropolis** module, implementing most of the database interactions. 
**Utopia Citadel** may be used in both server-side and client-side context. On server-side, please use **Utopia Exodus** 
that extends this module. On client-side, consider using **Utopia Trove** to set up a local database.

#### Utopia Citadel Description Importer
The **Citadel** module comes with a utility application for importing item descriptions into your database 
without difficult SQL operations. If you're utilizing **Utopia Citadel** or any of the dependent modules in 
your project, please make sure to check out the README file of this application for more details.

### Utopia Exodus
*Server-base that lets you skip right to your business logic*

The main point of **Utopia Exodus** is to handle the cumbersome user management and user authentication parts of your server-side 
application. By using **Utopia Exodus**, you can setup the required structures very quickly and get to what only you can implement: 
Your application business logic.

### Utopia Journey
*Leverage your Exodus server with a pre-built client interface*

**Utopia Journey** is the client-side interface to **Utopia Exodus** server, handling request authorization and local session management. 
Having a pre-built and easy-to-use interface for server interactions in your client takes you many steps ahead and saves you 
the trouble of worrying about a secure interface.

## Module Hierarchy
*Utopia* modules have following dependency-hierarchy. Modules lower at the list depend from those higher in the list.
- Utopia Flow
	- Utopia BunnyMunch
    - Utopia Vault
		- Utopia Trove
    - Utopia Access
        - Utopia Nexus
            - Utopia Nexus for Tomcat
        - Utopia Disciple
			- Utopia Annex
    - Utopia Inception
        - Utopia Genesis
            - Utopia Reflection
				- Utopia Reach
            - Utopia Conflict
	- Utopia Metropolis
		- Utopia Citadel (also requires Vault)
			- Utopia Exodus (also requires Nexus)
		- Utopia Journey (also requires Annex)
           
Basically every other *Utopia* module is dependent from **Flow**. All http-related modules are dependent from 
**Access** and all 2D visual modules are dependent from **Inception** and **Genesis**. **Nexus** is the base 
module for server-side operations while **Disciple** is the foundation of client-side server interactions. 
All user-management projects extend **Metropolis**.

Additional details about each module are listed in their own readmes.