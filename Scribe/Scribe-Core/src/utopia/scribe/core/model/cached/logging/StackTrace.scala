package utopia.scribe.core.model.cached.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.util.NotEmpty

object StackTrace
{
	// TYPES    ---------------------
	
	/**
	  * The Java version of the StackTraceElement class
	  */
	type JStackTraceElement = java.lang.StackTraceElement
	
	
	// OTHER    --------------------
	
	/**
	  * @param t A throwable
	  * @return The stack trace from that throwable item.
	  *         None if no stack trace could be extracted.
	  */
	def from(t: Throwable): Option[StackTrace] = from(t.getStackTrace)
	/**
	  * Converts a Java stack trace into a StackTraceElement
	  * @param elements The Java stack trace elements to convert
	  * @return A stack trace element from the specified Java versions
	  */
	def from(elements: IterableOnce[JStackTraceElement]) = {
		val elementIterator = elements.iterator
		elementIterator.nextOption().map { top =>
			_from(top) { elementIterator.nextOption() }
		}
	}
	// nextElement will be called until it returns None
	private def _from(element: JStackTraceElement)(nextElement: => Option[JStackTraceElement]): StackTrace = {
		val className = NotEmpty(element.getClassName.split('.').takeRightWhile { _.head.isUpper }.toVector) match {
			case Some(elements) => elements.mkString(".")
			case None => element.getClassName
		}
		apply(className, element.getMethodName, element.getLineNumber, nextElement.map { _from(_)(nextElement) })
	}
}

/**
  * Contains information about an exception's stack trace.
  * See the Java version of this class for more detailed information.
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  * @constructor Creates a new stack trace element
  * @param className Name of the class where this event occurred
  * @param methodName Name of the method where this event occurred
  * @param lineNumber Index of the line (1-based) where this event occurred
  * @param cause Stack trace element / event that occurred before this element. None if this was the first occurrence.
  */
case class StackTrace(className: String, methodName: String, lineNumber: Int, cause: Option[StackTrace] = None)
{
	// COMPUTED -------------------------
	
	/**
	  * @return An iterator that returns stack trace (elements) from the top element (this) to the bottom element.
	  *         Contains at least 1 item.
	  */
	def topToBottomIterator = OptionsIterator.iterate(Some(this)) { _.cause }
	
	/**
	  * @return A vector that contains all elements in this stack from the top (this) to the bottom.
	  *         Always contains at least 1 element.
	  */
	def topToBottom = topToBottomIterator.toVector
	/**
	  * @return A vector that contains all elements in this stack from the bottom to the top (this).
	  *         Always contains at least 1 element.
	  */
	def bottomToTop = topToBottom.reverse
}
