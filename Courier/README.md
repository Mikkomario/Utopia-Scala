# Utopia Courier
An interface for sending and receiving emails

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)

## Required External Libraries
- javax.mail - v1.6.2 was used in development
- activation - v1.1.1 was used in development

Both of these jars can be found within the [lib](https://github.com/Mikkomario/Utopia-Scala/tree/master/Courier/lib) 
directory.

See more information about [Java Mail's licences](https://javaee.github.io/javamail/JavaMail-License).  
Activation is available under the CDDL license. See: https://mvnrepository.com/artifact/javax.activation/activation/1.1.1

## Main Features
Email Interactions
- Sending email easily directly from your application.
  - There are pre-built configurations for SMTP, SMTPS and gmail.
- Reading email directly from the mail server
  - Supports filtering, pagination and message iteration without having to read and parse all messages beforehand
  - Supports custom mail processing to convert them to your desired model type.
    - Standard [Email](https://github.com/Mikkomario/Utopia-Scala/blob/master/Courier/src/utopia/courier/model/Email.scala) 
      model is provided as the default processing implementation
- Powerful immutable message models
  - Unlike Java Mail API's mutable models, **Courier** uses immutable data structures which are safer and easier 
    to understand

## Implementation Hints

### Sending Email
First, you need to construct an (implicit) 
[WriteSettings](https://github.com/Mikkomario/Utopia-Scala/blob/master/Courier/src/utopia/courier/model/write/WriteSettings.scala) 
instance. You have four options for doing this:
- SMTPS: `WriteSettings(host, port, authentication)`
- SMTP: `WriteSettings.simple(host)`
- G-Mail: `GMailWriteSettings(emailAddress, password)`
- Create a custom **WriteSettings** implementation

Next, you need to construct the **Email** model to send. For this, use:
- `EmailHeaders.outgoing(sender, recipients, subject)`
  - There are many ways to construct a 
    [Recipients](https://github.com/Mikkomario/Utopia-Scala/blob/master/Courier/src/utopia/courier/model/write/Recipients.scala) 
    instance. You can just use an implicit conversion from 
    a **String**, a **Seq** or a **Map**, or construct the instance using another method.
- `EmailContent(message, attachments)`
- `Email(headers, content)`

Finally, you will need to send the email using `EmailSender().send(email)` or `EmailSender().sendBlocking(email)`.

### Reading Email
First, you need to construct an (implicit) 
[ReadSettings](https://github.com/Mikkomario/Utopia-Scala/blob/master/Courier/src/utopia/courier/model/read/ReadSettings.scala) 
instance. Your options are:
- `PopReadSettings(host, authentication)` (using pop3)
- `ImapReadSettings(host, authentication)` (using IMAP)
- Create a custom **ReadSettings** implementation

Next, construct a new 
[EmailReader](https://github.com/Mikkomario/Utopia-Scala/blob/master/Courier/src/utopia/courier/controller/read/EmailReader.scala) 
instance
- `EmailReader.default` and `EmailReader.defaultWithAttachments(Path)` parse **Email** instances
- You can utilize a custom filter by using `EmailReader.filteredDefault(...)`
- Alternatively, you can create your own **FromEmailBuilder** implementation and pass it to 
  `EmailReader(...)` or `EmailReader.filtered(...)`. This will allow you to return data types besides **Email**.

Finally, use one of the **EmailReader** methods to read email from the server
- `.apply(...)` asynchronously reads all the parsed messages into a vector, returning a future
- `.readBlocking(...)` works like `.apply(...)`, except that it runs synchronously
- `.iterateAsync(...)(...)` asynchronously iterates over the read messages, returning a future with the 
  specified function completion. `.iterateBlocking(...)(...)` does the same synchronously.
  - These are the most memory-efficient functions, unless you're simply intending to parse the read messages 
    into a **Vector** anyway