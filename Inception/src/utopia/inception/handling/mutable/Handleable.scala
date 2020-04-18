package utopia.inception.handling.mutable

import utopia.inception.handling.HandlerType

import scala.collection.immutable.HashMap

/**
 * This is a mutable implementation of the Handleable trait
 * @author Mikko Hilpinen
 * @since 19.10.2016 (rewritten in 5.4.2019, v2+)
 */
trait Handleable extends utopia.inception.handling.Handleable
{
    // ATTRIBUTES   -----------------
    
    /**
      * The current handling states of this handleable instance. The handling states determine whether this instance
      * allows handling for different handler types
      */
    var handlingStates: Map[HandlerType, Boolean] = new HashMap[HandlerType, Boolean]()
    
    /**
      * This is the state used when a handling state hasn't been specified for a specific handler
      */
    var defaultHandlingState = true
    
    
    // IMPLEMENTED  -----------------
    
    override def allowsHandlingFrom(handlerType: HandlerType) = handlingState(handlerType)
    
    /**
     * The handling state (whether this instance be handled) for a certain handler type. Either a specific handling
      * state or the default handling state
      * @param handlerType The target handler type
      * @return The handling state for the specified type
     */
    def handlingState(handlerType: HandlerType): Boolean = handlingStates.getOrElse(handlerType, defaultHandlingState)
    
    /**
     * Specifies the handling state for a specific handler type
     * @param handlerType The type of the handler the state is for
     * @param state The new state for that handler
     */
    def specifyHandlingState(handlerType: HandlerType, state: Boolean) = handlingStates += handlerType -> state
    
    /**
     * Specifies the handling state for a specific handler type so that the default state is
     * no longer used. The handling state stays the same until it is changed, however.
     * @param handlerType the handlerType for which the state is specified
     */
    def specifyHandlingState(handlerType: HandlerType): Unit = specifyHandlingState(handlerType, handlingState(handlerType))
}
