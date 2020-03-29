UTOPIA INCEPTION
----------------

Required Libraries
------------------
    - Utopia Flow (since version 2+)


Purpose
-------

    Utopia Inception offers standardized event dispatching tools for more complex hierarchical event structures.


Main Features
-------------

    Handler & Handleable traits for receiving and distributing events between multiple instances
        - Supports both mutable and immutable implementations
        - Mutable implementations allow temporary deactivation & reactivation of instances
        - Permanent deactivation of instances is available through Mortal and Killable traits

    HandlerRelay for handling multiple mutable Handlers as a program context for Handleable instances

    Filter classes for filtering incoming events


v2  ------------------------------

    Changes
    -------

        Package structure updated
            - handling -> handling + handling.mutable + handling.immutable

        Major refactoring in all Handler classes
            - Handlers separated to generic trait + mutable and immutable implementations
            - DeepHandler to replace the old Handler implementation

        Major refactoring in Handleable trait
            - Handleable separated to generic trait + mutable and immutable implementation
            - All Handleable instances are no longer Mortal, Killable as separate mutable trait

        Refactored Filter traits
            - Filter is no longer a class
            - Implicit conversion from function to filter
            - !, && and || added to Filter