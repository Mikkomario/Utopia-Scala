package utopia.flow.util.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

import scala.language.implicitConversions

object CollectSingleFailureLogger
{
	// COMPUTED ---------------------------
	
	/**
	 * Creates a new logger instance
	 * @return A factory for creating a new logger that collects a single failure
	 */
	def newInstance =
		SysErrLogger.use { implicit backup => new CollectSingleFailureLoggerFactory(Pointer.eventful.empty) }
	
	
	// IMPLICIT ---------------------------

	// Implicitly converts this object into a new factory wrapping a new pointer
	implicit def objectAsFactory(o: CollectSingleFailureLogger.type): CollectSingleFailureLoggerFactory = o.newInstance
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param pointer Pointer to wrap. Will receive the first encountered error.
	 * @return A factory for constructing a new logger
	 */
	def wrap(pointer: EventfulPointer[Option[Throwable]]) = new CollectSingleFailureLoggerFactory(pointer)
	
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
	@deprecated("Deprecated for removal. Please use .wrap(EventfulPointer).processingMessagesUsing(...) instead", "v2.9")
	def wrapProcessingMessages(pointer: EventfulPointer[Option[Throwable]])
	                          (processMessage: (String, Boolean) => Option[Throwable]) =
		wrap(pointer).processingMessagesUsing { (message, _, hadError) => processMessage(message, hadError) }
	/**
	 * Creates a new logger that wraps another pointer and ignores any string-based log-entries
	 * (i.e. only handles Throwables)
	 * @param pointer        A pointer to wrap
	 * @return A new logger
	 */
	@deprecated("Deprecated for removal. Please use .wrap(EventfulPointer).ignoringMessages instead", "v2.9")
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
	@deprecated("Deprecated for removal. Please use .newInstance.processingMessagesUsing(...) instead", "v2.9")
	def processingMessages(f: (String, Boolean) => Option[Throwable]) =
		newInstance.processingMessagesUsing { (message, _, logged) => f(message, logged) }
	
	
	// NESTED   ------------------------------
	
	class CollectSingleFailureLoggerFactory(pointer: EventfulPointer[Option[Throwable]])
	{
		/**
		 * @return A logger implementation that ignores all messages & details
		 *         that are not associated with any Throwable
		 */
		def ignoringMessages = new CollectSingleFailureLogger(pointer, (_, _, _) => None)
		
		/**
		 * @param messageLogger A logger used for recording logged messages that have no associated Throwable
		 * @return A logger that collects logged Throwables, and forwards encountered messages to the specified logger
		 *         in situations where no throwable has been attached.
		 */
		def delegatingMessagesTo(messageLogger: Logger) =
			processingMessagesUsing { (message, details, logged) =>
				if (!logged)
					messageLogger(message, details)
				None
			}
		
		/**
		 * @param f A function for processing attached error messages & details.
		 *          Receives 3 values:
		 *              1. Logged message
		 *              1. Logged details
		 *              1. Whether this entry was originally linked to a Throwable (logged)
		 * @return A new logger which handles message data using the specified function
		 */
		def processingMessagesUsing(f: (String, Model, Boolean) => Option[Throwable]) =
			new CollectSingleFailureLogger(pointer, f)
	}
}

/**
 * This logging implementation collects encountered failures into a pointer.
 * Only one failure is stored at a time. Entries without errors are delegated.
 * @author Mikko Hilpinen
 * @since 19.10.2023, v2.3
 */
class CollectSingleFailureLogger(failureContainer: EventfulPointer[Option[Throwable]] = EventfulPointer.factory(SysErrLogger).empty,
                                 processMessage: (String, Model, Boolean) => Option[Throwable] = (_, _, _) => None)
	extends Logger with ChangingWrapper[Option[Throwable]]
{
	// IMPLEMENTED  ---------------------
	
	override implicit def listenerLogger: Logger = SysErrLogger
	override protected def wrapped: Changing[Option[Throwable]] = failureContainer.readOnly
	
	override def toString = {
		val suffix = if (failureContainer.value.isDefined) ".failed" else ".empty"
		s"Logger.single$suffix"
	}
	
	override def apply(error: Option[Throwable], message: String, details: Model): Unit = {
		// Stores the encountered error, if one was present and if there is space
		error.foreach(store)
		// Processes the message, if specified
		// May also store the error produced by the message-processing logic
		if (message.nonEmpty || details.nonEmpty)
			message.notEmpty.flatMap { processMessage(_, details, error.isDefined) }.foreach(store)
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
