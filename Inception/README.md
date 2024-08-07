# Utopia Inception

## Notice
This module has reached its end-of-life, will not receive further development and will be removed in a future release.  
All features from this module are now present in a rewritten form in the **Genesis** module.

## Parent Modules
- Utopia Flow

## Main Features

**Handler** & **Handleable** traits for receiving and distributing events between multiple instances
- Supports both mutable and immutable implementations
- Mutable implementations allow temporary deactivation & reactivation of instances
- Permanent deactivation of instances is available through **Mortal** and **Killable** traits

**HandlerRelay** for handling multiple mutable **Handler**s

Filter classes for filtering incoming events

## Implementation Hints
You will need to focus on inception mostly at the point where you are familiar with the handling system and want to
create your own implementation. You will probably get quite far by simply utilizing the existing **Handler**
implementations.

### Classes you should be aware of
- **HandlerRelay** - Used for collecting and keeping track of multiple mutable **Handlers**
- **Handleable** - A common trait for most of the event-related receivers. You need to select between the mutable and
  the immutable implementation or build your own.