# Utopia Annex

## Parent Modules
- Utopia Flow
- Utopia Access
- Utopia Disciple

## Main Features

Advanced request interface with Api, QueueSystem and RequestQueue classes
- Support request deprecation
- Request queueing
- Advanced response model that allows custom handling of different failures 
  (E.g. errors based on server response status, response parsing failure or request timeout)

Models that support offline use and slower server responses
- Shcrödinger traits for handling states where server results may or may not be available
- Spirit trait for replicating data before it has been sent to the server

## Implementation Hints

### Classes you should be aware of
- **QueueSystem** and **Api** - You need instances of both of these traits to make requests effectively
- **RequestQueue** - You will be implementing this trait in your server interface classes
- **Spirit** and **Shcrodinger** (including sub-traits of **Schrodinger**) - You will need to implement 
  these traits in your data models
- **GetRequest**, **PostRequest** and **DeleteRequest** - Standard models for making requests to server