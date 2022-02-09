package utopia.inception.handling

import utopia.flow.util.CollectionExtensions._

object Handler
{
	implicit class HandleableHandler[A <: Handleable](val h: Handler[A] with Handleable) extends AnyVal
	{
		/**
		  * @return Whether this handler should be called on events of it's own type
		  */
		def handlingState = h.allowsHandlingFrom(h.handlerType)
	}
	
	implicit class MutableHandleableHandler[A <: Handleable](val h: Handler[A] with mutable.Handleable) extends AnyVal
	{
		/**
		  * Specifies the handler's own handling state
		  * @param newState Whether this handler should be called on events of it's own type
		  */
		def handlingState_=(newState: Boolean) = h.specifyHandlingState(h.handlerType, newState)
	}
}

/**
  * Handlers offer an interface for interacting with multiple handleable instances at once
  * @tparam A The type of instance handled by this handler. Needs to be supported by the specified handler type
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
trait Handler[+A <: Handleable] extends Iterable[A]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return The type of this handler
	  */
	def handlerType: HandlerType
	
	/**
	  * @return The contents of this handler. Should contain only elements that wish to remain in this handler
	  * @see considerDead(Handleable)
	  */
	def aliveElements: Seq[A]
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return A string representation of the contents of this handler
	  */
	def debugString: String =
	{
		val elements = aliveElements
		s"$handlerType with ${elements.size} items: [${elements.map {
			case handler: Handler[_] => handler.debugString
			case other => other.toString
		}.mkString(", ")}]"
	}
	
	
	// IMPLEMENTED	----------------
	
	override def foreach[U](f: A => U) = aliveElements.foreach(f)
	
	override def isEmpty = aliveElements.isEmpty
	
	override def iterator = aliveElements.iterator
	
	
	// OTHER	---------------------
	
	/**
	  * @param element A target element
	  * @return Whether the specified element should be removed from this handler, whether not already
	  */
	def considersDead(element: Handleable): Boolean = element match
	{
		case mortal: Mortal => mortal.isDead
		case _ => false
	}
	
	/**
	  * @param element A target element
	  * @return Whether the specified element allows being handled by this handler at this time. The caller may
	  * disregard this plea, however.
	  */
	def allowsHandling(element: Handleable): Boolean = element.allowsHandlingFrom(handlerType)
	
	/**
	  * A view for handling operations, may take into account target element's desire to not allow handling
	  * @param allowSkip Whether target element's desire to not be handled should be respected
	  * @return A view of target elements (or all elements if allowSkip = false)
	  */
	def handleView(allowSkip: Boolean = true) =
		if (allowSkip) aliveElements.view.filter(allowsHandling) else aliveElements
	
	/**
	  * Performs a certain operation for all target elements
	  * @param operation Operation that takes a single element
	  * @param allowSkip Whether target element's desire to not be handled should be respected
	  * @tparam U Arbitary type
	  */
	def handle[U](operation: A => U, allowSkip: Boolean = true) = handleView(allowSkip).foreach(operation)
	
	/**
	  * Performs an operation for all targets while a condition is met
	  * @param operation An operation for a single element that also returns whether the next element should be accepted
	  * @param allowSkip Whether target element's desire to not be handled should be respected
	  */
	def handleWhile(operation: A => Boolean, allowSkip: Boolean = true): Unit = { handleView(allowSkip).find { !operation(_) } }
	
	/**
	  * Performs an operation on the target items until a suitable result is found
	  * @param operation Operation that takes a single element and returns possible result
	  * @param allowSkip Whether target element's desire to not be handled should be respected
	  * @tparam B The result type
	  * @return The (first) successful result or None
	  */
	def mapFirst[B](operation: A => Option[B], allowSkip: Boolean = true) = handleView(allowSkip).findMap(operation)
}