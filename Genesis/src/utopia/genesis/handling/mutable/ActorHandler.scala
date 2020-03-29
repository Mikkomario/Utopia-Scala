package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

object ActorHandler
{
    /**
      * @param actors Initial actors in the new handler
      * @return A new handler with specified actors
      */
    def apply(actors: TraversableOnce[handling.Actor] = Vector()) = new ActorHandler(actors)
    
    /**
      * @param actor The initial actor
      * @return A new handler that contains the specified actor
      */
    def apply(actor: handling.Actor) = new ActorHandler(Vector(actor))
    
    /**
      * @return A new handler with all of the provided actors
      */
    def apply(first: handling.Actor, second: handling.Actor, more: handling.Actor*) = new ActorHandler(Vector(first, second) ++ more)
}

/**
 * ActorHandlers are used for calling act method of numerous Actors in succession. The handler also
 * makes sure that only actors which have the correct handling state are called
 * @author Mikko Hilpinen
 * @since 23.12.2016
 */
class ActorHandler(initialElements: TraversableOnce[handling.Actor]) extends DeepHandler[handling.Actor](initialElements) with handling.ActorHandler