package utopia.genesis.handling.event

import utopia.flow.operator.filter.Filter
import utopia.flow.util.Mutate
import utopia.flow.view.template.eventful.{Changing, Flag}

/**
  * Common trait for factories used for creating event listeners
  * @tparam Event Type of events received
  * @tparam Repr Type of this factory
  */
trait ListenerFactory[Event, +Repr]
{
    // ABSTRACT --------------------------
    
    /**
      * @return Condition on which the created listeners should be informed of events
      */
    def condition: Flag
    /**
      * @return Filter applied by the created listeners on incoming events
      */
    def filter: Filter[Event]
    
    /**
      * @param filter A new filter to apply (overrides the existing filter)
      * @return Copy of this factory that uses the specified filter for the incoming events
      */
    def usingFilter(filter: Filter[Event]): Repr
    /**
      * @param condition A condition for receiving event information (overrides existing condition)
      * @return Copy of this factory that uses (only) the specified condition for managing listening-states
      */
    def usingCondition(condition: Flag): Repr
    
    
    // OTHER    -------------------------
    
    /**
      * @param filter A filter applied to incoming events.
      *               Only events accepted by this filter will trigger this listener / wrapped function.
      * @return Copy of this factory that applies the specified filter to the created listeners.
      *         If there were already other filters specified, combines these filters with logical and.
      */
    def filtering(filter: Filter[Event]): Repr = usingFilter(this.filter && filter)
    /**
      * @param f A mapping function to apply to the current event-filter used by this factory
      * @return Copy of this factory with the mapped filter applied
      */
    def mapFilter(f: Mutate[Filter[Event]]) = usingFilter(f(filter))
    
    /**
      * @param condition A condition that must be met for this listener to receive event data
      * @return Copy of this factory that applies this filter to created listeners.
      *         If there was a condition already in place, both of these conditions will be applied
      *         (using logical and).
      */
    def conditional(condition: Changing[Boolean]): Repr = usingCondition(this.condition && condition)
    
    /**
      * @param f A mapping function applied to this factory's listening condition
      * @return Copy of this factory that applies the mapped condition instead
      */
    def mapCondition(f: Mutate[Flag]): Repr = usingCondition(f(condition))
}
