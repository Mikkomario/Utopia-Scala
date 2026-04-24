package utopia.flow.util.logging

import utopia.flow.collection.mutable.builder.CompoundingSeqBuilder
import utopia.flow.generic.model.immutable.Model

/**
 * "Logs" errors and other entries by queueing them for later processing.
 *
 * @author Mikko Hilpinen
 * @since 26.04.2024, v2.4
 */
class LogQueue extends Logger
{
	// ATTRIBUTES   -----------------------
	
	private val builder = new CompoundingSeqBuilder[(Option[Throwable], String, Model)]()
	
	
	// COMPUTED ---------------------------
	
	/**
	 * Accesses the queued log entries without enqueueing any.
	 * @return All queued log entries so far.
	 */
	def queued = builder.currentState
	
	
	// IMPLEMENTED  -----------------------
	
	//noinspection ScalaUnnecessaryParentheses
	override def apply(error: Option[Throwable], message: String, details: Model): Unit =
		builder += ((error, message, details))
	
	
	// OTHER    ---------------------------
	
	/**
	 * Removes and returns all the queued log entries
	 */
	def popAll() = {
		val res = builder.result()
		builder.clear()
		res
	}
	
	/**
	 * Logs all queued log entries. Won't remove any of them, however.
	 * @param log Logger to process the queued entries with.
	 */
	def logAllUsing(log: Logger) =
		queued.foreach { case (error, message, details) => log(error, message, details) }
	/**
	 * Logs all queued log entries. Won't remove any of them, however.
	 * @param log Logger to process the queued entries with.
	 */
	def logAll()(implicit log: Logger) = logAllUsing(log)
	
	/**
	 * Logs, removes and returns all queued log entries
	 * @param log Logger to process the queued entries with
	 * @return All queued (and now logged) entries
	 */
	def popAndLogAllUsing(log: Logger) = {
		val res = popAll()
		res.foreach { case (error, message, details) => log(error, message, details) }
		res
	}
	/**
	 * Logs, removes and returns all queued log entries
	 * @param log Logger to process the queued entries with
	 * @return All queued (and now logged) entries
	 */
	def popAndLogAll()(implicit log: Logger) = popAndLogAllUsing(log)
	
	/**
	 * @param log Logging implementation used for handling recorded messages
	 * @return All collected throwables
	 */
	def popErrorsAndLogMessagesUsing(log: Logger) = {
		popAll().flatMap { case (error, message, details) =>
			if (message.nonEmpty || details.nonEmpty)
				log(message, details)
			error
		}
	}
	/**
	 * @param log Implicit logging implementation used for handling recorded messages
	 * @return All collected throwables
	 */
	def popErrorsLogMessages(implicit log: Logger) = popErrorsAndLogMessagesUsing(log)
	
	/**
	 * @param log Logging implementation used for handling recorded throwables
	 * @return All messages which were not associated with a throwable
	 */
	def popMessagesAndLogErrorsUsing(log: Logger) =
		popAll().flatMap { case (error, message, details) =>
			error.foreach { log(_, message, details) }
			if (message.nonEmpty || details.nonEmpty)
				Some(message -> details)
			else
				None
		}
	/**
	 * @param log Implicit logging implementation used for handling recorded throwables
	 * @return All messages which were not associated with a throwable
	 */
	def popMessagesLogErrors(implicit log: Logger) = popMessagesAndLogErrorsUsing(log)
	
	/**
	 * Removes any queued log entries
	 */
	def clear() = builder.clear()
}
