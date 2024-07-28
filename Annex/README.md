# Utopia Annex
**Annex** provides advanced client-side http interfaces, 
providing special support for unstable and slow internet connections.

## Parent Modules
- Utopia Flow
- Utopia Access
- Utopia Disciple

## Main Features
Advanced request interface with **ApiClient**, **QueueSystem** and **RequestQueue**
- Supports offline use-cases, including request deprecation / retraction
  - I.e. If your request can't be sent fast enough (due to lack of internet access or other queued requests), 
    the **RequestQueue** may be automatically advised to remove the queued request before it is sent.
- Request queueing
- Advanced **Response** / **RequestResult** models that allow custom handling of different failures 
  (E.g. errors based on server response status, response parsing failure and request timeout)

**Schrodinger** traits for simulating server responses before they are acquired. 
These are especially useful in situations where internet connection is unstable or slow, 
especially in GUI applications, as the use of **Schrodinger**s enables responsive interfaces, even when offline.

## Implementation Hints

### Classes you should be aware of
- **QueueSystem** and **ApiClient** - You need instances of both of these traits to make requests effectively
- **RequestQueue** - You will be implementing and/or this trait for request queueing
  - Specifically, **PersistingRequestQueue** trait enables you to cover situations where the application is closed 
    before a request completes.
- **Shcrodinger** and its sub-traits - Using **Schrodinger**s will allow you to create more reactive logic and interfaces
- **ApiRequest**, **GetRequest** and **DeleteRequest** - Implement these traits to create wrapped request-handling logic