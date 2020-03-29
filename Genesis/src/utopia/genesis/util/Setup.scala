package utopia.genesis.util

import utopia.inception.handling.Handleable
import utopia.inception.handling.mutable.HandlerRelay

import scala.concurrent.ExecutionContext

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
	  * @param context The execution context for asynchronous operations. You can use utopia.flow.async.ThreadPool
	  *                for creating the context, for example.
	  */
	def start()(implicit context: ExecutionContext): Unit
	
	
	// OTHER	--------------------
	
	/**
	  * Registers specified objects to the handlers in this setup
	  * @param objects The objects that will be registered
	  */
	def registerObjects(objects: TraversableOnce[Handleable]) = handlers ++= objects
	
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
}
