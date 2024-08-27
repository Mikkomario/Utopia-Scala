package utopia.flow.util.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

object CollectSingleFailureLogger
{
	/**
	 * Creates a new logger that wraps another pointer and processes messages using the specified processor function
	 * @param pointer A pointer to wrap
	 * @param processMessage A function for processing error messages.
	 *                       Accepts 2 parameters:
	 *                          1) Logged message, and
	 *                          2) Whether an error was already specified in this log entry.
	 *
	 *                       Returns an error to store (if possible), or None if no error should be stored.
	 * @return A new logger
	 */
	def wrapProcessingMessages(pointer: EventfulPointer[Option[Throwable]])
	                          (processMessage: (String, Boolean) => Option[Throwable]) =
		new CollectSingleFailureLogger(pointer, processMessage)
	
	/**
	 * Creates a new logger that wraps another pointer and ignores any string-based log-entries
	 * (i.e. only handles Throwables)
	 * @param pointer        A pointer to wrap
	 * @return A new logger
	 */
	def wrapIgnoringMessages(pointer: EventfulPointer[Option[Throwable]]) = new CollectSingleFailureLogger(pointer)
	
	/**
	 * Creates a new logger that processes messages using the specified processor function
	 * @param f A function for processing error messages.
	 *                       Accepts 2 parameters:
	 *                       1) Logged message, and
	 *                       2) Whether an error was already specified in this log entry.
	 *
	 *                       Returns an error to store (if possible), or None if no error should be stored.
	 * @return A new logger
	 */
	def processingMessages(f: (String, Boolean) => Option[Throwable]) =
		new CollectSingleFailureLogger(processMessage = f)
	
	/**
	  * @param messageLogger A logger used for recording logged messages that have no associated throwable
	  * @return A logger that collects logged throwables and forwards encountered messages to the specified logger
	  *         in situations where no throwable has been attached.
	  */
	def delegatingMessagesTo(messageLogger: Logger) =
		processingMessages { (message, logged) =>
			if (!logged)
				messageLogger(message)
			None
		}
	
	/**
	 * Creates a new logger that ignores any string-based log-entries (i.e. only handles Throwables)
	 * @return A new logger
	 */
	def ignoringMessages() = new CollectSingleFailureLogger()
}

/**
 * This logging implementation collects encountered failures into a pointer.
 * Only one failure is stored at a time. Entries without errors are delegated.
 * @author Mikko Hilpinen
 * @since 19.10.2023, v2.3
 */
class CollectSingleFailureLogger(failureContainer: EventfulPointer[Option[Throwable]] = EventfulPointer.factory(SysErrLogger).empty,
                                 processMessage: (String, Boolean) => Option[Throwable] = (_, _) => None)
	extends Logger with ChangingWrapper[Option[Throwable]]
{
	// IMPLEMENTED  ---------------------
	
	override implicit def listenerLogger: Logger = SysErrLogger
	override protected def wrapped: Changing[Option[Throwable]] = failureContainer.readOnly
	
	override def apply(error: Option[Throwable], message: String): Unit = {
		// Stores the encountered error, if one was present and if there is space
		error.foreach(store)
		// Processes the message, if specified
		val messageError = message.notEmpty.flatMap { processMessage(_, error.isDefined) }
		// May also store the error produced by the message-processing logic
		messageError.foreach(store)
	}
	
	
	// OTHER    ------------------------
	
	/**
	 * Removes and returns the currently collected failure, making space to collect another
	 * @return Currently collected failure. None if no failure was collected.
	 */
	def pop() = failureContainer.pop()
	
	private def store(error: Throwable) =
		failureContainer.update { current => if (current.isDefined) current else Some(error) }
}
