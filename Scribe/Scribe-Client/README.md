# Utopia Scribe Client
This document describes the **client-specific Scribe logging system** functions introduced in this module. 
For more information concerning the Scribe logging system in general, see **Scribe Core** module instead.

## Parent modules
This library requires the following Utopia modules to be added to your class path in order to work:
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch)
- [Scribe Core](https://github.com/Mikkomario/Utopia-Scala/tree/master/Scribe/Scribe-Core)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple)
- [Utopia Annex](https://github.com/Mikkomario/Utopia-Scala/tree/master/Annex)

## Main features
Client-side Scribe implementation that periodically sends log entries to the server
- Request intervals and local caching may be configured
- Supports offline mode - log entries are sent when connection with the server is established

Maximum logging limit -support that prevents recursive logging (optional feature)

## Setting up Scribe Client
In order to use the Scribe Client, you first need a functioning **QueueSystem** instance (**Utopia Annex** feature). 
We will not cover the creation of this instance in this document.

Start by constructing a new **MasterScribe** instance.  
Here you specify the following settings:
- Url to the server endpoint that receives the log entries
  - Typically, this is the path where you've located the **LoggingNode** in the server-side implementation (see **Scribe Api**)
- Backup logging implementation to record any failures within this interface
- Duration how long issues should be kept in the client
  - By specifying a longer duration, you may reduce the amount of requests that are performed, 
    as consecutive log entries are sent in a single bulk request.
- Optionally, you may specify a time threshold after which entries are not considered valid
  - This mostly concerns the offline use-case
- You may also specify a maximum logging limit in order to prevent recursive logging
  - If this limit is reached, all logging is stopped
- Finally, you may specify a function that is applied to all outgoing log entries
  - Here you can, for example, add certain client-specific details that apply to all entries sent via this interface

Optionally, you may now construct a root Scribe instance.  
Optionally, you may register this root instance into a **Synagogue**, along with any backup Logger implementations 
you may wish to use.

Finally, proceed to construct new **Scribe** instances by either:
1. Calling `.in(subContext: String)` from your **Synagogue** instance (recommended)
2. Calling `.in(subContext: String)` from your **root Scribe** instance
3. Creating new Scribe instances directly