package utopia.genesis.handling.immutable

import utopia.genesis.handling
import utopia.genesis.handling.Actor
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.{Handleable, Handler}

object ActorHandler
{
	/**
	  * An empty actor handler
	  */
	val empty = apply(Vector())
	
	/**
	  * @param actors Actors in the new handler (default = empty)
	  * @param parent Parent handleable (default = None)
	  * @return A new handler with specified 'actors', dependent from 'Parent'
	  */
	def apply(actors: TraversableOnce[Actor], parent: Option[Handleable] = None) = new ActorHandler(actors)
	
	/**
	  * @param actor A single actor
	  * @return A new Handler containing specified 'actor', dependent from 'parent'
	  */
	def apply(actor: Actor) = new ActorHandler(Vector(actor))
	
	/**
	  * @return A new handler with all specified actors
	  */
	def apply(first: Actor, second: Actor, more: Actor*) = new ActorHandler(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the ActorHandler trait
  * @param initialElements The initial elements in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class ActorHandler(initialElements: TraversableOnce[Actor] = Vector()) extends
	Handler[Actor](initialElements) with handling.ActorHandler with Handleable