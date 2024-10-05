# Utopia Annex - List of Changes

## v1.9 - 04.10.2024
This update mostly adds new utility functions for response-parsing. 
Additionally, following the changes in Flow, there are some changes to **Schrodinger** constructors.
### Breaking changes
- Most **Schrodinger** constructors now require access to an implicit **Logger** implementation, 
  because error-logging was added to their managed state pointers.
### New features
- Added **RequestResultExtensions** that adds a couple of new methods for **Futures** that contain **RequestResults**
### New methods
- **ApiClient.PreparedRequest**
  - Added multiple new functions for parsing optional values 
    (i.e. parse variants that replace empty responses with None but parse non-empty responses)
- **Future** (**RequestResultExtensions**)
  - Added `.tryFlatMapSuccess(...)` and `.flatMapSuccessToTry(...)` for asynchronous **RequestResults**
- **ResponseParser** (**ResponseParseExtensions**)
  - Added `.rightToResponse(...)` for **ResponseParsers** of type **Either**
- **RequestQueue**
  - Added `.tryPushEither(RequestQueueable)`
### Other changes
- Built with Scala v2.13.14
- **ApiClient**`.multiParserFrom(FromModelFactory)` and **PreparedRequest**`.getMany(FromModelFactory) `
  can now handle partial failures by logging them (optional feature)

## v1.8 - 28.07.2024
A major overhaul of basic Annex concepts, namely **Response**s and response-parsing 
(following the changes in **Disciple** v1.7), **ApiRequest**s, **ApiClient**s (formerly known as **Api**s) and 
**RequestQueue** systems.

Most importantly, this update supports custom response-parsing within the request-processing itself. 
Much of the existing interfaces obviously needed an update because of this addition.

### Breaking changes
- Multiple changes to **RequestResult** and **Response.Success**
  - In general, responses are now expected to be parsed by the time they are returned by other interfaces, 
    such as **ApiClient** or **RequestQueue**
    - The parsing is completed either in:
      - An **ApiRequest** implementation
      - When calling **ApiClient** directly
    - As a consequence, these classes now utilize a generic type parameter, 
      which indicates the type of parsed response body value
      - Unfortunately this makes `unapply` in **Response.Success** function quite poorly, which may require refactoring
  - The three parameters in **Response.Success** now appear in order: `value` (renamed), `status`, `headers`
    - Previously: `status`, `body`, `headers`
- Replaced **Api** with **ApiClient**
  - **Api** is still available, but deprecated
  - `.get(...)` and `.post(...)` functions in **Api** now yield **PreparedRequest** instead of a **Future**
  - **Api** now requires the implementation of `implicit def jsonParser: JsonParser`
- Multiple changes to **ApiRequest**:
  - **ApiRequest** now accepts a type parameter
  - **ApiRequest**s are now required to implement a `send(PreparedRequest)` function, 
    which handles response-parsing.
    - This also applies to `ApiRequest.get(...)`
  - The `body` property is now of type **Either** in order to support both **Value**-sending and custom **Body** sending
    - In order to match the previous functionality, you must wrap the return values with `Left(...)`
  - Removed `ApiRequest.post(...)`
    - Similar functionality may be achieved by calling `ApiRequest.apply(...) `
      or by using the new **PersistingApiRequest** companion object.
- **DeleteRequest** no longer extends **Persisting**, nor does it implement `deprecated` nor `persistingModelPointer`
- Rewrote parts of **PersistedRequestHandler**
  - The `handle(...)` function is now expected to parse, create and perform the request, also
  - Removed the `factory` property
- **RequestQueue** no longer specifies implementation for `.push(...)`
  - A new version which includes implementation and largely matches the previous **RequestQueue** trait, 
    is **SystemRequestQueue**
- Multiple changes to **ContainerUpdateLoop**:
  - **ContainerUpdateLoop** now accepts 2 generic type parameters instead of one
  - `makeRequest(...)` is now expected to handle response parsing, also
  - `merge(...)` Now receives the parsed value
  - `merge(...)` is now called even when the response is empty, but still not if the server returns 304 / Not Modified
- Deleted a few previously deprecated **Schrodinger** classes
### Deprecations
- Multiple deprecations in **RequestResult** classes
  - Deprecated `.toEmptyTry` in favor of `.toTry`, which includes the response body on success
  - Deprecated `.singleParsedFromSuccess(...)` and `.manyParsedFromSuccess(...)` 
    in favor of `.tryParseSingle(...)` and `.tryParseMany(...)`
  - Deprecated `.body` in **Success**
    - The same is now accessible as `.value`
- Deprecated the **ResponseBody** trait + classes
- Deprecated **Spirit** and **PostSpiritRequest**, since these seem somewhat redundant with the current 
  **ApiRequest** implementation
- Deprecated `DeleteRequest.apply(...)`
  - The intended replacement is to use `PersistingApiRequest.apply(...)`
### Bugfixes
- Persisted requests are now removed from the request container regardless of whether there was a request handler 
  that was able to process them
### New features
- Added **PreparingResponseParser** trait + object + **ResponseParseExtensions** in order to make creating 
  Annex-compatible **ResponseParser**s easier.
- Added **PersistingApiRequest** trait, where the focus is mostly on its companion object, which offers multiple 
  functions for constructing **ApiRequest**s which persist between sessions.
### New methods
- **ApiRequest** (object)
  - Added `.apply(...)` and `.getValue(...)` for creating new requests
  - Added `.persisting` which provides access to constructors for persisting API-request
- **GetRequest** (object)
  - Added `.value(...)` which matches the previous `.apply(...)` implementation
- **PersistingRequestQueue**
  - Added `.withoutPersisting: RequestQueue`
- **RequestQueue**
  - Added `.tryPush(Try)` and `.tryPushSeed(Try)`
- **RequestResult**
  - Added `.map(...)` and `.tryMap(...)`
  - Added multiple functions which only apply to **RequestResult**s with content type **Value**:
    - Added `.parsingOneWith(...)`, `.parsingManyWith(...)` and `.parsingOptionWith(...)`
    - Added `.tryParseOne(FromModelFactory)`, `.tryParseMany(FromModelFactory)` and `.tryParseOption(FromModelFactory)`
### Other changes
- In many instances where **Vector** was required, **Seq** is now used
- **PersistingRequestQueue** now persists the previously persisted requests again upon sending them 
  (still removing them once sending completes). 
  - This is to handle situations where not all requests can be processed before the session terminates again.

## v1.7 - 22.01.2024
This update focuses on persisting request handling (i.e. offline support across use-sessions). 
Specifically, it adds a new type of request: **RequestSeed**, which may be useful in request-chaining across sessions.

Other updates consist mostly on style updates.
### Breaking changes
- **ApiRequest** no longer requires property `persistingModel: Option[Model]`. 
  This is now defined in **Persisting** and **ConsistentlyPersisting**, which you may extend if you want the 
  persisting functionality.
- Renamed `.isDeprecated` to `.deprecated` in **ApiRequest**
- **PersistedRequestHandler**`.handle(...)` now accepts 3 parameters (model, request & result) instead of just 1 (result)
- Replaced `offlineDelayIncreaseModifier: Double` in **QueueSystem** constructor to 
  `increaseOfflineDelay: FiniteDuration => FiniteDuration`
- Renamed **PostRequest** to **PostSpiritRequest**
- **Response.Failure** message is now **String** instead of an **Option**
- The message parameter in **ContainerUpdateLoop**`.handleFailureResponse(...)` is now **String** instead of an **Option**
### Deprecations
- Renamed **QueueSystem**`.pushSynchronous(ApiRequest)` to `.pushBlocking(ApiRequest)`
- Renamed **QueueSystem**`.isOnlinePointer` to `.onlineFlag`
### New features
- Added support for a new way of sending requests: **ApiRequestSeed**
  - This allows delayed request-generation in a persisted context. 
  - This may be useful when chaining persisting requests.
- Persisting requests now support variable persisting formats
### New methods
- **QueueSystem**
  - Added `.queuedRequestCountPointer`
- **Schrodinger**
  - Added `.pointer`, which provides read-only access to the wrapped `.fullStatePointer`
- **SchrodingerState**
  - Added `.isAlive` and `.isDead`
### Other changes
- **PersistingRequestQueue**`.start(...)` is now public instead of protected
- **Schrödinger** now uses `.mapUntil(...)` to form its derived pointers
- Scala version updated to 2.13.12

## v1.6 - 27.09.2023
Updated the **RequestResult** interface for easier and more concise use.
### Breaking Changes
- Moved **RequestNotSent** under **RequestResult**
- **NoConnection** is now **RequestSendingFailed**
- Functions that previously returned or accepted **Either[RequestNotSent, Response]** now use **RequestResult**
- Renamed **Manifest** to **Manifestation** because of Scala naming conflicts
### Deprecations
- Replaced **ShrodingerState**`.signOption: Option[Sign]` with `.estimate: UncertainBinarySign`
### New Methods
- **Schrodinger**
  - Added utility functions for adding manifest and result listeners

## v1.5 - 01.05.2023
This update doesn't introduce many changes, but the change it introduces requires refactoring.
### Breaking Changes
- Api methods now return Future[RequestResult] instead of Future[Try[Response]]

## v1.4 - 02.02.2023
This update is compatible with the latest **Flow** (v2.0) and **Disciple** (v1.6) updates.  
In addition, the **Schrödinger** concept has been completely rewritten.
### Breaking Changes
- **Api** implementations are now required to implement `implicit protected def log: Logger`. 
  This property is used for catching errors encountered during response body parsing.
### Deprecations
- Deprecated all existing **Schrödinger** traits in favor of the new implementations
### New Features
- Completely rewrote the **Schrödinger** classes to be pointer-based and to present only a read-only interface with 
  a bunch of constructors for different use-cases.
- **NoConnection** and **Response.Failure** now extend a new trait **RequestFailure**. 
  This makes **RequestResult** pattern matching easier, because all failure states may be matched into a single case.
### New Methods
- **ResponseBody**
  - Added `.parseMany(FromModelFactory)`

## v1.3 - 02.10.2022
This update introduces some smaller utility additions which aim to make this module more scalable.  
Most of the changes relate to the **ContainerUpdateLoop** class.
### Breaking Changes
- **Response** (including **Success** and **Failure**) now contains headers
- **ContainerUpdateLoop** now requires  the `.requestTimeContainer` as a **FileContainer** of **Instant** **Options**
  - See new **ValueConvertibleOptionFileContainer** class in **Flow**
### Other Changes
- **ContainerUpdateLoop** now primarily uses a **Date** header from the response as a new request time threshold, and only 
  secondarily uses local time. This is to account for situations where server clock is in different time than client clock.
- **Failure** response message parsing now first seeks for model properties "error", "description" or "message" before 
  including the whole response body as the message

## v1.2 - 18.08.2022
This update adds logging support to automated container updates, reflecting **Flow** v1.16 changes.
### Breaking Changes
- **ContainerUpdateLoop**`.merge(...)` is now required to return a tuple instead of just one value. 
  The new, second value indicates the custom wait time until next request.
- The following classes require an implicit **Logger** parameter:
  - **ContainerUpdateLoop**
  - **PersistingRequestQueue**

## v1.1 - 06.06.2022
This update reflects changes made in **Flow** update v1.15.
### Breaking Changes
- **ContainerUpdateLoop** now extends **LoopingProcess** instead of **Loop**
  - This means that `.startAsync()` is no longer available (replaced with `.runAsync()`)

## v1.0.4 - 27.01.2022
Dependency changes, according to Disciple update v1.5
### Scala
This module now uses Scala v2.13.7

## v1.0.3 - 04.11.2021
Supports changes in Flow v1.14

## v1.0.2 - 4.9.2021
This update simply supports changes in **Utopia Disciple** v1.4.2 update that added its own 
**RequestFailedException** class.
### Deprecations
- Deprecated **RequestFailedException** in favor of another exception with the same name in **Utopia Disciple**

## v1.0.1 - 13.7.2021
No changes in features, simply added support for the latest (breaking) changes in the 
**Access** (v1.4) and **Flow** (v1.10) modules.

## v1.0 - 17.4.2021
Initial release with the main features (see README)
