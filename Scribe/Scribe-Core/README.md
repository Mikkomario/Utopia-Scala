# Utopia Scribe Core
**Utopia Scribe** module provides an interface for detailed logging on both server and client side. 
This document describes the core module, which provides the common functionality between these 
two use-cases.

## Parent modules
You will need to add these **Utopia** modules to your class hierarchy in order to use the **Scribe** modules:
- **Flow**
- **BunnyMunch**

## Main Features
**Scribe** provides an interface for detailed and hierarchical logging entries
- Applying specific details to your logging entries helps when debugging
- Detailed context and version information shows you where exactly an error has been recorded

## Scribe system architecture
The **Scribe** system is typically implemented so that:
1. Recorded entries are stored in a server-side database
2. Server-side issues are recorded directly to that database
3. An API-interface is provided, through which the clients may send log entries over to the server
4. Clients periodically send encountered issues to the server (optional feature)

Log entries are converted into 3 levels of items:
1. **Issues**
2. **Issue Variants**
3. **Issue Occurrences**

**Issues** represent a type of entry that may be repeated over time. 
These should represent **unique items of interest**, although that is not always possible.

**Issue Variants** represent **alterations** within individual Issues, 
where certain **key information** has changed. 
There should typically be only a few (e.g. 1-20) variants for a single Issue.

**Issue Occurrences** represent individual logging entries. These may be repeated and there may be even hundreds or 
thousands of instances per Issue or Issue Variant.

## Scribe interface
Scribe interface implementation will be specific to the applicable environment, 
i.e. whether you're implementing Scribe on the server-side or on the client-side. 
However, in both use-cases, you will be using the same 
[Scribe interface](https://github.com/Mikkomario/Utopia-Scala/blob/development/Scribe/Scribe-Core/src/utopia/scribe/core/controller/logging/ScribeLike.scala).  
In this section, we will cover how to set up and use this interface.

### The root Scribe instance
You typically start by creating a **root Scribe** instance and by registering 
it to your shared [Synagogue instance](#the-synagogue-instance) (optional). 
The **root Scribe** used is specific to whether you're working on 
server-side or on client-side, and will be introduced in either the **Scribe Api** or **Scribe Client** module.

### Class-specific Scribe instances
Once you have a common **root Scribe** or **Synagogue** instance, you create **class-specific sub-instances** 
based on that root instance. For each class-specific instance, you specify a **unique context**.

A class-specific **Scribe** instance is typically declared using syntax:
`implicit val scribe: Scribe = <synagogue reference>.in("module.function.MyClassName")`
where `module.function.MyClassName` represents a unique context parameter. 

You may use any string as a context parameter. It is, however, recommended that you follow these guidelines:
- Make every context parameter **unique**, so that you can more easily recognize where issues occur
- Use some **separator** to differentiate between nested contexts
  - In our example, we used `.` as a separator

When you use this class-specific **Scribe** instance in a specific sub-context, such as a function or a method, 
you may append the context by calling `.in(subContext: String)`.  
For example, calling `scribe.in("subFunction")` would result in a **Scribe** instance using context 
`module.function.MyClassName.subFunction`. 
This way you don't need to manually repeat the common context portion in every logging entry.

### Recording Issues
When you need to record a new log entry, whether it is for debugging or for error recording, 
you may specify the following information to your context-specific **Scribe** instance:
1. Possible [sub-context](#sub-context) via `.in(subContext: String)`
2. Issue [Severity](#issue-severity) via `.apply(severity: Severity)`
3. Possible [Issue Variant details](#variant-details) via 
  `.variant(key: String, detail: Value)` or `.variant(details: Model)`
4. Encountered **error**, if applicable
5. An additional **message**, if appropriate
6. **Situational details** in **Model** format

Details 1-3 are specified using separate methods that create new **Scribe** instances. 
You're free to save that instance to a local variable in order to copy those details between multiple entries.  
Details 4-6 are specified when creating the exact logging entry, and are specific to that entry.

Scribe instances will also need to have implicit access to the current version of your software. 
Typical way to implement this is to define an implicit **Version** instance somewhere and introduce it via import. 
This is oftentimes the same place where you might specify an implicit ConnectionPool (Vault) or 
ExecutionContext instance as well.

Next we will cover the role of these fields in more detail.

#### Sub-context
The role of a sub-context is to specify the (unique) location where the issue occurred. 
If two entries share the same context and same level of Severity, they are considered to describe the same issue. 
Therefore, it is important to specify enough details in the context, so that your logging entries won't all look like 
"some error somewhere".

I personally recommend specifying a unique sub-context on function or method level, where possible.

#### Issue Severity
The Severity enumeration is an important detail when recording and processing Issue data. 
If Severity is defined accurately within the code, the person scouring through the logging entries will have 
much easier time focusing on the most important items.

Available Severity levels are as follows, from least to most important / severe:
1. **Debug** - Used for recording situational information that is useful when debugging other issues, 
  but of **no importance by default**
2. **Info** - Used for recording details that might be of interest, but which **don't indicate any kind of problem**
3. **Warning** - Used for indicating **potential** problems which may or may not require any action
4. **Recoverable** - Used for indicating problems that are **possible to overcome by the software**. 
  For example, if the software encounters a network error that causes delays but not a complete failure of the 
  sending process overall, it should use the Recoverable Severity level.
5. **Unrecoverable** - Used for indicating problems from which the software can't recover from. 
  This typically causes some function to not get completed successfully. 
  This is also the **default Severity level** when no other value is defined.
6. **Critical** - Used for problems that require near immediate action and those that are of special importance. 
  This Severity level is appropriate, for example, in situations where an error renders the whole system unusable.

If you need more information about the different Severity levels, please also check the scaladocs in the 
[source code](https://github.com/Mikkomario/Utopia-Scala/blob/development/Scribe/Scribe-Core/src/utopia/scribe/core/model/enumeration/Severity.scala).

#### Variant details
There is yet another way to differentiate between recorded issues, and that is by specifying different 
**Issue Variant** details. These details are specified using the Model format, 
allowing a wide range of data types to be used. Do remember to import `utopia.flow.generic.casting.ValueConversions._ `
when specifying these values, in order to enable implicit type-casting.

Each unique set of Issue Variant details creates a new Issue Variant, which is treated as separate group of 
Issue Occurrences. Because of this, you should choose such details that fulfill the following conditions:
1. They define **important distinctions** between environments where an issue might occur
2. They are **limited in number** (e.g. range from 2 to 20 options)

If you define too many unimportant variant details, you will have too many entries to scan trough when debugging. 
On the other hand, if you don't differentiate between the important use-case differences, you may have a hard 
time targeting your research to those use-cases.

Applicable **software version always forms an implicit variant detail**, so you don't need to specify it manually.

I personally use details such as:
- Identifier of the client which the error concerns (in case there are a small number of clients)
- Http status code returned in the server response

If a detail is common to all entries made using a client-side software instance, 
you may choose to introduce it in your MasterScribe instance instead, but more on that in **Scribe Client**.

#### Error, message and details
The actual logging entry may consist of any combination of an encountered error (Throwable instance), 
a custom message and custom details.

Like with Issue Variant details, these details are introduced in Model format.  
You may choose to use such details as:
- Exact error message returned in a server response
- Exact input parameters that were used when the issue occurred

Here, it is better to record too much than too little information. 
Old entries will be merged in the server over time (optional feature), 
so you don't have to worry so much about filling your database with unnecessary details.

#### Examples
Here are some code examples of recording various imaginary issues. 
Every example assumes that a class-specific val scribe: Scribe has been defined and is accessible in that context.

Logging a detailed issue variant when the server rejects a request:
```
scribe.in("request.result.failure").variant("status", response.status.code)
    .apply("Server rejected the request", details = Model.from("message" -> response.message))
```
Debug logging:
```
scribe.in("process.input").debug("Started processing input", details = Model.from("input" -> input))
```
Storing a Scribe instance locally:
```
// A Scribe instance is stored in a local variable in order to avoid manually repeating the status and the context
val _scribe = scribe.in("request").variant("status", response.status.code)
// Case: Request succeeded => Logs and processes the received data
if (response.isSuccess) {
    _scribe.in("success").info("Successfully retrieved model data")
    ...
}
// Case: Request failed => Logs as an error and tries again after a short delay
else {
    _scribe.in("failure).recoverable("Failed to retrieve model data from the server. Attempts again later.", 
        details = Model.from("message" -> response.message, "retryDelaySeconds" -> retryDelay.toPreciseSeconds))
    ...
}
```

### The Synagogue instance
The [Synagogue](https://github.com/Mikkomario/Utopia-Scala/blob/development/Scribe/Scribe-Core/src/utopia/scribe/core/controller/logging/Synagogue.scala) 
class provides a **Scribe** interface by wrapping a root **Scribe** instance, along with 
alternative **Logger** implementations. The **Synagogue** instance delegates logging to the wrapped **Scribe** instance, 
reverting back to the specified alternatives in cases where logging fails for one reason or another. 
This way you will have a logging implementation available even in situations where the primary implementation 
fails or is not available.

## Other implementation hints
If you want to use the Scribe logging system with Try and TryCatch instances, please 
import `utopia.scribe.core.util.logging.TryExtensions._`