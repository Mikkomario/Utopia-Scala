package utopia.inception.handling.immutable

import utopia.flow.collection.VolatileList
import utopia.inception.handling.{HandlerType, Mortal}

object Handler
{
	type Handleable = utopia.inception.handling.Handleable
	
	/**
	  * Creates a new handler with specified elements
	  * @param hType The type of the handler
	  * @param elements The elements added to the handler (default = empty)
	  * @tparam A The handled elements
	  * @return A new handler with specified elements
	  */
	def apply[A <: Handleable](hType: HandlerType, elements: TraversableOnce[A] = Vector()) = new Handler(elements)
	{
		val handlerType = hType
	}
	
	/**
	  * Creates a new handler with a single element
	  * @param handlerType The type of the handler
	  * @param element The element to be put to the handler
	  * @tparam A The element type
	  * @return A handler with a single element
	  */
	def apply[A <: Handleable](handlerType: HandlerType, element: A): Handler[A] = apply(handlerType, Vector(element))
	
	/**
	  * @param handlerType The type of the handler
	  * @param first The first element
	  * @param second The second element
	  * @param more More elements
	  * @tparam A Element type
	  * @return A handler with all provided elements
	  */
	def apply[A <: Handleable](handlerType: HandlerType, first: A, second: A, more: A*): Handler[A] = apply(handlerType, Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the Handler trait. This handler is safe to use in multithreaded environments.
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
abstract class Handler[A <: utopia.inception.handling.Handleable](initialElements: TraversableOnce[A])
	extends utopia.inception.handling.Handler[A] with Mortal
{
	// ATTRIBUTES	--------------------
	
	private val elements = VolatileList(initialElements)
	
	
	// IMPLEMENTED	--------------------
	
	override def aliveElements = elements.updateAndGet { _.filterNot(considersDead) }
	
	// Immutable handlers die once they get empty, since an empty handler has no purpose
	override def isDead = aliveElements.isEmpty
}
