package utopia.genesis.handling

import utopia.inception.handling.Handleable

import scala.concurrent.duration.FiniteDuration

/**
 * Actors are instances that are called between certain intervals to perform some logical,
 * (duration based) operations.
 * @author Mikko Hilpinen
 * @since 23.12.2016
 */
trait Actor extends Handleable
{
    // ABSTRACT ------------------
    
    /**
     * This method will be called at a short intervals (usually >= 60 times a second). The instance
     * should update it's status here. This method should only be called when the object's handling
     * state allows it.
     * @param duration The duration since the last act
     */
    def act(duration: FiniteDuration)
    
    
    // COMPUTED ------------------
    
    /**
      * @return Whether this actor should be considered active
      */
    def isActive = allowsHandlingFrom(ActorHandlerType)
}
