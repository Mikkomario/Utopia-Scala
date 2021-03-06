package utopia.genesis.util

import utopia.inception.handling.Handleable
import utopia.inception.handling.mutable.HandlerRelay

/**
  * Setups are used for quickly setting up a program
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
trait Setup
{
	// ABSTRACT	--------------------
	
	/**
	  * @return The handler relay for this setup
	  */
	def handlers: HandlerRelay
	
	/**
	  * Starts the program based on this setup
	  */
	def start(): Unit
	
	
	// OTHER	--------------------
	
	/**
	  * Registers specified objects to the handlers in this setup
	  * @param objects The objects that will be registered
	  */
	def registerObjects(objects: IterableOnce[Handleable]) = handlers ++= objects
	
	/**
	  * Registers specified objects to the handlers in this setup
	  * @param first An object
	  * @param second Another object
	  * @param more More objects
	  */
	def registerObjects(first: Handleable, second: Handleable, more: Handleable*): Unit = registerObjects(
		Vector(first, second) ++ more)
	
	/**
	  * Registers a single object to the handlers in this setup
	  * @param obj An object
	  */
	def registerObject(obj: Handleable) = handlers += obj
	
	/**
	 * Removes specified objects from the handlers in this setup
	 * @param objects Objects to remove
	 */
	def removeObjects(objects: Iterable[Handleable]): Unit = handlers --= objects
	
	/**
	 * Removes specified objects from the handlers in this setup
	 * @param first First object to remove
	 * @param second Second object to remove
	 * @param more More objects to remove
	 */
	def removeObjects(first: Handleable, second: Handleable, more: Handleable*): Unit =
		removeObjects(Vector(first, second) ++ more)
}
