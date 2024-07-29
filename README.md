# Utopia
Utopia is a collection of libraries that provide easy-to-use interfaces for various programming concepts.  
The use-cases of these libraries are varied and range from restful API-creation to GUI applications.

## Requirements
You will only need **Scala sdk v2.13** and **Java jdk 8** to use *Utopia*. Some modules may 
require additional libraries but most of *Utopia* is built on Scala and Java source code alone.

### Why Scala?
Based on my personal programming experience, I've found Scala to be the most scalable and 
flexible language and most suited for this kind of generic framework. 
I also found Scala to be well compatible with my [core development philosophy](#core-philosophy).

*Utopia* relies heavily on strong typing and generic type support and benefits greatly from implicits. 
Based on my current experience, only Java, Scala and possibly Kotlin provide a suitable 
foundation (although I haven't used Kotlin yet). *Utopia* started out as a Java project, 
but I've found Scala to work better in every area.

This project version is built using Scala v2.13.12

## Core Philosophy
The main purpose of the Utopia library is two-fold:
1) I want programming to be both **easy** and **fast** for you
2) I want to allow you to switch between **higher and lower abstraction levels** when you need to

### Why?
Many libraries and frameworks **feel awesome when you first start to use them**, but...  

I personally used GameMaker from a very early age, and I was really happy with how fast and easy it was to develop 
simple programs.  

Unfortunately, when you eventually wish to **dive deeper** and access some advanced features like generics - 
usually in order to maximize the scalability and "dryness" of your code - 
**many frameworks simply don't let you do that**.  

When I was studying Swift 3, for example, I found the language to be very "swift" and flow easily and concisely.  
However, the moment when I wanted to access generics (and specifically covariance), 
the language boundaries became apparent.  
Apple developers had access to such features but not their customers.

(In general, my development philosophy is quite contrary to that of Apple's, at least when it comes to control)

That's why I want to give you the pleasurable experience of **smooth and easy programming**, 
while **giving you access** to both higher and lower abstraction layers.

### What are abstraction levels?
Here's an example of abstraction layers from highest to lowest:  
- **Layer 1 (principles):** There are universal (moral) principles, like what's good and what's bad, 
what creates good results and what creates bad results. Universal principles manifest themselves in a 
multitude of life areas.  
- **Layer 2 (procedures & formulas):** We humans have observed these principles and have developed a set of laws and rules 
that help us live along these principles. These can be rather specific but still oftentimes leave space for interpretation.  
- **Layer 3 (implementation details):** Our everyday actions are then physical manifestations of our 
governing principles, rules and rituals. These are very specific and tangible.

The way this works in programming is that we have very abstract tools that cannot by themselves be used 
without extension or concrete implementation, but which crystallize the essence or the core of some feature. 
In Scala these often manifest as traits.

We then have customizable components or tools that can be used as they are, but leave defining the specifics 
to the user. Due to their customizable nature, these tools can be used in various different contexts.

Finally, we have concrete implementations that are specific to the business case they exist in (this is what you 
normally deal with when programming).

The lower / more concrete you go on abstraction layers, the easier everything gets but the 
less freedom you have. Working on very high abstraction layers if often more difficult, and you 
have to write a lot of code yourself. Working on lower abstraction layers if often straightforward, 
but you are limited to the built-in options.

In *Utopia*, I've made my best to provide you with access to both higher and lower abstraction levels. 
While I aim to create simple user-friendly interfaces, I also acknowledge that you may come to a place where you 
need to override some of my implementations. 

For example, Let's say you're not so happy about how a specific UI component performs. In such a situation, 
you can write your own and replace my implementation with it - as long as you follow the very high level 
guidelines / logic required for the interaction with other *Utopia* features. The higher you want to 
customize, the more you will need to write yourself - but when you want, you can! 
In other words, you're not locked out from editing / extending the higher abstraction layers.

### Use of External Libraries
This abstraction level concept is also why **I've opted to use as few external libraries as reasonably possible**.  
I want to give **you** the freedom to **choose** which libraries suit you best while still 
providing you with an easy-to-use foundation.

The times I have opted to use external libraries (e.g. in case of Utopia Disciple and Utopia Nexus for Tomcat), 
you can find out that most of the implementation code is in-fact independent of the libraries used. 
If you want to use different libraries, you don't need to write that much code by yourself.

## Pros and cons of using Utopia
Many people, especially early on, have criticized me for taking on such a project and "reinventing the wheel".  
However, I've found this library to be very useful in my own work, and I honestly think it can be a 
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
- *Utopia* consists of a number of modules for different purposes and there's a **synergy effect** that takes 
  place between these modules. I would say that usually frameworks don't play together as well as *Utopia* does, 
  mainly because they focus on a rather limited area and therefore can't share lay a new foundation for the other tools. 

**Cons:** 
- **Utopia will spoil you**. Once you get used to the privileges you've acquired by using *Utopia*, it feels 
  quite bad to work with areas and languages where you don't have these features available.
- Utopia is quite a large set of libraries at this point, and it uses its own data structures and approaches a lot. 
  If you use Utopia, it will likely affect your code in a lot of areas, which makes it more difficult to transition 
  away from using it.
  - Also, the learning curve may be steep, depending on where you start, since the submodules rely heavily on 
    features from other modules, requiring you to learn both the lower and higher level modules.
- At the moment **I develop *Utopia* by myself**. This means that everything takes time to develop. 
  If you need a specific area covered or a feature developed you may contact me or **contribute**
    - I don't have a testing team or a quality assurance team to make sure everything works smoothly. 
      I test new features and fix the bugs as I encounter them. I don't always find all bugs in a timely fashion.
    - In case I was to quit developing this library (not intending to), you would have to perform some updates yourself.
- Historically, many *Utopia* modules have undergone **major changes** over time. When I learn new ways of doing things 
  and my skills develop, I often revisit and revision my old code. This means that if you wish to stay up-to-date with 
  these libraries, you will have to deal with some compile errors caused by these changes.
- Your specific business case may have some requirements this library doesn't cover 
  (E.g. advanced information security or code efficiency in terms of speed). In these cases you should 
  consider either using another library or helping me overcome these issues.
- At the time of writing, *Utopia* has few resources in form of tutorials and instructions. I intend 
  to remedy this in the future and I would appreciate it if you would contact me for more information.

One thing you should also consider is that 
**I've often prioritized readability and ease-of-use over speed and memory optimization**. I believe that, in general, 
our hardware will continue to advance at a much faster rate than our 
programming capacity and skill. If you specifically need high-level performance and optimization to meet a specific 
business need (E.g. big data analytics), I recommend you to rely less on *Utopia* tools in that area. In less 
critical areas (performance-wise), I would recommend you to use these tools, however.

## High-Level Module Descriptions
*Utopia* project consists of a number of modules. Below is a short description of the purpose of each module.

For additional information on each module, visit their README files directly.

### General use -modules
These modules are widely used and offer support tools for all kinds of applications.

#### Utopia Flow
[Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow) provides the standard set-of-tools that are used 
in all the other Utopia modules.

**Utopia Flow** makes many things in Scala even easier than they are by default and offers some very useful data 
structures and data-related features. **Utopia Flow** is a standard building block and the 
**foundation for all the other Utopia modules**.  
**Flow** has the highest level of abstraction amongst all modules and can be used in both frontend and backend 
development, and is useful in almost any kind of Scala project.  

**Flow** is a **standard inclusion for all of my projects** these days, 
whether they be servers, desktop clients, command-line apps or real-time games.

#### Utopia BunnyMunch
A fast and simple json parser.

[Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch) is a very simple model, 
meant to replace the inefficient **JsonReader** implementation from *Flow*. 
*BunnyMunch* uses a very fast [Jawn](https://github.com/typelevel/jawn) json parsing library internally, 
but offers the same **Value**-based interface as Flow's JsonReader.

#### Utopia Paradigm
[Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm) is a library focused on vector 
mathematics and 2D shapes.

**Utopia Paradigm** introduces a number of models that are often used in visual and/or mathematical software,
such as points, bounds, polygons, matrices, affine transformations, colors and so on.
While these models are most valuable in front-end UI applications, they may also be utilized in backend calculations.

The challenge with these kinds of models, previously, was that they were mostly available through AWT or some other
Java library, built around mutability and all sorts of nasty stuff. **Paradigm** models, however, are
all immutable and suitable for a Scala approach to front-end programming.

### Modules for database interaction
These modules facilitate interactions with MySQL-based databases (including MariaDB).

#### Utopia Vault
[Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault) is the main Utopia library for interacting 
with MySQL-based databases.  
However, what's great with Vault is that you don't need much SQL know-how to use it.

**Vault** offers you a number of abstraction 
levels to operate on when it comes to database interactions. I especially enjoy the model based approach where 
you don't need to write a single line of SQL. But unlike many other noSQL-frameworks, this one actually still lets 
you operate directly on SQL when or if you need it.

##### Utopia Vault Coder
[Vault Coder](https://github.com/Mikkomario/Utopia-Coder/tree/master/Vault-Coder) is a command-line tool 
for converting your model concepts into a database structure + advanced Scala code.

This **Vault** add-on helps you by generating much of the model and database interaction code you would
otherwise have to write manually, saving a lot of your time. If you're using **Utopia Vault** or any of the
dependent modules (especially if you're using **Citadel**), I highly recommend this tool. 
Please check the [README](https://github.com/Mikkomario/Utopia-Coder/tree/master/Vault-Coder) 
of this application for more details.

#### Utopia Trove
[Trove](https://github.com/Mikkomario/Utopia-Scala/tree/master/Trove) is a small library which allows you to host a 
MariaDB database directly from your application.

**Utopia Trove** is a rather simple library that offers an easy-to-use interface 
to [Vorburger's MariaDB4j](https://github.com/vorburger/MariaDB4j) library 
(see `ch.vorburger.mariaDB4j:mariaDB4j:3.0.1` in Maven) that is used for setting up a local embedded database 
directly from Java (in this case Scala) code. Most of the heavy lifting is done by the Vorburger's library, 
but **Trove** adds its own flavor: Database structure setup, version control and updates, 
as well as an easy one-line database setup and shutdown functions.

### Http modules
These modules provide tools for http clients and http servers

#### Utopia Access
[Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access) is the common http module providing tools 
which are shared between server- and client-side software.

Http status codes, content types, headers, etc. are now readily available for you in a very simple format.
This allows you to use the same models on both ends of your full-stack development and saves you the time from
studying or writing two separate interfaces.

**Access** leaves the implementation to its submodules: **Nexus** and **Disciple**. And, in case you want
to create your own http library implementation, you can.

#### Utopia Nexus
[Nexus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Nexus) provides a simple interface for building 
advanced restful APIs.

Have you ever wished you had a nice, up-to-standard **Rest-API**, but you found the idea of building one quite
overwhelming? **Nexus** makes API-setup so easy you'll be surprised; I surprise myself with it all the time.
What you get is a good set of **quality data models** to work with, along with a way to create restful interfaces
that let you **focus on you business logic** instead of focusing how to get the technology to work.

Don't want to use REST-architecture? You don't have to! You can still create the interface with the same foundation
and the same building blocks, any way you want to.

##### Utopia Nexus for Tomcat
[Nexus for Tomcat](https://github.com/Mikkomario/Utopia-Scala/tree/master/NexusForTomcat) is a specific extension 
of **Nexus**, allowing you to use it on top of the
[Apache Tomcat](https://tomcat.apache.org/) platform.

**Nexus doesn't force you to use any specific server-implementation or library**, such as Tomcat.
While this gives you more freedom in terms of implementation, it can also be a challenge
if you just want to get started quickly. That's why there's **Nexus for Tomcat**,
which lets you quickly set up **Nexus** over a Tomcat instance, and to do it without much effort at all.

#### Utopia Disciple
[Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple) is a client-side http library, wrapping
[Apache's HttpClient](https://hc.apache.org/httpcomponents-client-4.5.x/index.html).

**Disciple** utilizes models from **Access**, and provides some of its own for client side http request
and response handling. The solution is based on Apache HttpClient under the hood, but you don't need to
understand that library in order to use **Disciple**. Basically **Disciple** allows you to make http requests
with just basic understanding about http methods, statuses and some common headers.

In case you wish to use **Disciple** interface with some other http client library, contact me and I will separate
the dependency - it's only a single file anyway.

#### Utopia Annex
[Annex](https://github.com/Mikkomario/Utopia-Scala/tree/master/Annex) is an extension of the **Disciple** library, 
providing more advanced interfaces specifically suited for
use-cases where fast internet access is not guaranteed.

**Utopia Annex** goes two steps further than **Disciple**, by offering you advanced request and response models,
as well as connection management.

By utilizing **Annex**, your application won't suffer even when the server-connection breaks for a moment.
**Annex** is your go-to client-side http interface solution when you need your application to work in environments where
network connection is not 100% reliable. This module is actually in use at Helsinki-Vantaa airport in Finland,
where time-critical operations must be performed 24/7 and all data needs to be reliably captured and delivered -
even when the internet connection breaks.

I would recommend you to use **Annex** in cases where you have considerable data-interaction with your server.
For individual requests, I would recommend you to stick with **Disciple**, since it's more light-weight.

### GUI modules
These modules provide tools for interactive visual applications

#### Utopia Genesis
[Utopia Genesis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Genesis) provides a foundation for interactive 
GUI applications, focusing on GUI events and drawing.

**Utopia Genesis** has its background in real-time 2D game development, although
it's by no means limited to such purposes and is most often used as a foundation for standard client-side 
GUI applications.

**Genesis** provides you with a powerful set of tools for building interactive visual software.
You have images, mouse events, keyboard events, real-time action events, etc. Without **Genesis**, you would normally
have to rely on AWT-tools, which are less flexible, less scalable, less functional and less easy to use.

The problem with **Genesis** is that the standard Swing framework isn't build upon them,
but that's why I've built **Utopia Reach** and **Utopia Reflection** GUI libraries.

#### Utopia Firmament
[Firmament](https://github.com/Mikkomario/Utopia-Scala/tree/master/Firmament) provides a set of generic traits and 
tools used in both of my component layout / GUI frameworks 
(i.e. **Reach** and **Reflection**). Since these are separate modules these days, the common features were 
gathered into this module.

I would suggest checking **Utopia Reach** and then checking the 
[README of this module](https://github.com/Mikkomario/Utopia-Scala/tree/master/Firmament), in case 
you find it interesting.

#### Utopia Reach
[Reach](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach) is a GUI library with minimal reliance on 
AWT and Swing.

Over the time I've progressed from **1)** having to use the Swing framework and hating it, 
to **2)** building my own framework (**Reflection**) around the Swing framework, hating the limitations of Swing 
even more, to **3)** building a new framework (**Reach**) with as little AWT as possible - a huge amount of 
work to write, but absolutely worth it.

**Utopia Reach** uses only two heavily wrapped Swing components: 
**1)** a **JFrame** or **JDialog** and **2)** an empty **JPanel**.  
**Reach** implements its own focus system, event distribution system, layout system, you name it. 
It is heavily inspired by Swift's declarative GUI programming. All layout updates are automatized. Very rarely 
do you need to specify the exact sizes of components. Another major difference between Swing and Reach is that 
Reach fluently supports multi-threading (a feat that was supposed to be impossible with Swing, I learnt).

One major principle which makes Reach superior to Swing is that all components and data structures are read-only by 
default and react to well-specified pointers. This makes it easier to track, who caused what change (and bug). 
This doesn't mean the resulting system will be simple. There tend to be a lot of interactive dependencies and 
different event-based actions when building GUIs.

Finally, **Reach** obviously doesn't have the same look and feel as Swing does. 
The built-in component designs are somewhat inspired by [Material Design](https://m3.material.io/).
However, as with all the other *Utopia* modules, **Reach** allows you to design and implement your own style on 
top of the existing capabilities. The only requirement is that you can't really rely on Swing or Awt components, 
nor on the standard OS look-and-feel. If you would rather still use mostly Swing, consider using **Reflection** instead.

##### Reach Coder
[Reach Coder](https://github.com/Mikkomario/Utopia-Coder/tree/master/Reach-Coder) is a utility application for 
generating factory classes for your custom Reach components based on 
json definitions. This application is most suited for customizable components, 
like those introduced in the **Reach** library.

#### Utopia Reflection
[Reflection](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reflection) is a Scala- and Utopia-friendly 
wrapper to the **Swing** framework.

**Reflection** is a Swing-like GUI framework, where components are wrapped and managed through a 
well-defined (but open) interface. Swing dependencies are clearly separated from the generic models and traits. 
First of all **Reflection** lets you use the models from **Genesis**, also providing its own additions. 
Second, **Reflection** handles layout like it should have been handled 
(much like Swift handles layout with StackPanels and constraints).

There are a number of pre-existing component implementations (swing-based). 
You also have access to all the higher abstraction level interfaces, 
which lets you create your own components with relative ease, in case you want to.

At this time **Reflection** is getting overshadowed with the more recent **Reach** library. 
I would personally only recommend using **Reflection** on a new project if you still, for some reason, want to 
mix Utopia with Swing layout.

##### Utopia Reach in Reflection
[Reach-in-Reflection](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach-in-Reflection), a very small module (one file), 
enables you to use **Utopia Reach** inside the **Utopia Reflection** framework.

Earlier, **Reach** used to be a sub-feature of **Reflection**, but is now an independent module/system. However, 
in order to support existing projects where **Reach** and **Reflection** intertwine, there's this module.

#### Utopia Conflict
[Utopia Conflict](https://github.com/Mikkomario/Utopia-Scala/tree/master/Conflict) is a small collision-handling 
library built on top of **Paradigm** and **Genesis**.

**Utopia Conflict** lets you include collision checking and collision events in your program. Collision detection
is a required feature in most 2D games, but it's also one of the toughest to implement from scratch. I've done the
vector mathematics for you here and wrapped the logic in a familiar **Genesis**-style handling system.

You probably don't need to use **Conflict** in your normal business software, but if you happen to be creating a 2D
game or a physics-based software, this will most likely help you a lot in getting started.

### Modules that act as interfaces to specific technologies
These modules provide more advanced interfaces to some standard information technologies.

#### Utopia Courier
[Courier](https://github.com/Mikkomario/Utopia-Scala/tree/master/Courier) is a library for sending and receiving emails.

**Utopia Courier** model is focused on email integration and supports both sending and writing emails.
This module wraps the JavaMail API, providing a much cleaner and more effective interface.

#### Utopia Manuscript
**Manuscript** provides interfaces for reading .xls and .xlsx documents, commonly known as Excel files.

**Manuscript** is built on top of [Apache POI](https://poi.apache.org/), providing a more simple yet more powerful 
interface, utilizing soft typing from the **Flow** module.

#### Utopia Terra
[Terra](https://github.com/Mikkomario/Utopia-Scala/tree/master/Terra) deals with GPS-location information, 
providing multiple vector-based projections for this data.

If you find yourself in a situation
where you need to use latitude-longitude information, Utopia Terra is your friend.
This module allows you to easily convert angular latitude-longitude data into linear vector format,
where transitions and distances are more reasonable to calculate.

#### Utopia Echo
**Echo** provides an interface to LLMs (i.e. Large Language Models) and specifically to the 
[Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md).

With **Echo**, you can utilize LLMs more easily and directly from within your application code.

### Modules for specific application needs
These modules provide more specialized implementations for various common issues in applications.

#### Utopia Scribe
The [Scribe](https://github.com/Mikkomario/Utopia-Scala/tree/master/Scribe) module provides a detailed logging system 
specifically designed for use-cases where both front and back end 
are implemented using Scala. Logging entries are delivered from the clients to the server, and are readable using 
a custom application.

**Scribe** allows you to deliver very detailed logging entries, which helps in debugging. 
This module is most suited for more complex projects, where local file logging is not enough.

**Scribe** is divided into three submodules:
1. [Scribe Core](https://github.com/Mikkomario/Utopia-Scala/tree/master/Scribe/Scribe-Core), 
  which is common to both server- and client-side
2. [Scribe Api](https://github.com/Mikkomario/Utopia-Scala/tree/master/Scribe/Scribe-Api), 
  which requires database access and is intended for server-side use
3. [Scribe Client](https://github.com/Mikkomario/Utopia-Scala/tree/master/Scribe/Scribe-Client), 
  which sends logging information to a server using the **Scribe Api** module

#### Models for user management and authentication
These modules provide tools for somewhat advanced user management

##### Utopia Metropolis
[Metropolis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Metropolis) is a module which provides models for 
user management. This module is extended with **Disciple** and **Exodus**.

The purpose of these modules is to give you a 
pre-built user-management and session-based authentication system for your server and optionally client, also. 
I've noticed that these features need to exist in so many server applications that I decided to make this the 
one-time solution to this problem.

##### Utopia Citadel
[Citadel](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel) provides database interactions related 
with user management (based on **Metropolis**).

**Utopia Citadel** may be used in both server-side and client-side context. On server-side, use **Utopia Exodus**, 
which extends this module. On client-side, consider using **Utopia Trove** to set up a local database.

###### Utopia Citadel Description Importer
The **Citadel** module comes with this utility application for importing localized item descriptions into your database 
without difficult SQL operations. If you're utilizing **Utopia Citadel** or any of the dependent modules in 
your project, please make sure to check out the 
[README file of this application](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel-Description-Importer) 
for more details.

##### Utopia Exodus
[Exodus](https://github.com/Mikkomario/Utopia-Scala/tree/master/Exodus) provides server-side user-management, 
greatly facilitating authorization, for example.

The main point of **Utopia Exodus** is to handle the cumbersome user management and user authentication, 
that belong to any valid server-side application - and to do it well.  
By using **Utopia Exodus**, you can set up the required systems very quickly and get to what only you can implement: 
Your application business logic.

##### Utopia Ambassador
[Ambassador](https://github.com/Mikkomario/Utopia-Scala/tree/master/Ambassador) is a module specifically designed 
for managing the OAuth process.

**Utopia Ambassador** has one main goal: Handle OAuth server-side process properly, so that you don't have to. 
If you already have a **Utopia Exodus** server, this module is very easy to add on top of it; You 
will be able to skip tens of hours of work in dealing with 3rd party OAuth. 
**Ambassador** comes with all rest nodes towards both 
the web client and the 3rd party redirection, so you will only need to add your business logic.

#### Utopia Logos
**Logos** provides a database interface for storing and indexing large amounts of text effectively.

**Logos** is still in its beta stages, so not all features are present in a stable form.  
Logos is suitable, for example, for indexing document or email contents for optimized search and machine-based analysis.