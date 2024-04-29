package utopia.flow.util.logging

import utopia.flow.collection.mutable.builder.CompoundingVectorBuilder

/**
 * "Logs" errors and other entries by queueing them for later processing.
 *
 * @author Mikko Hilpinen
 * @since 26.04.2024, v2.4
 */
class LogQueue extends Logger
{
	// ATTRIBUTES   -----------------------
	
	private val builder = new CompoundingVectorBuilder[(Option[Throwable], String)]()
	
	
	// COMPUTED ---------------------------
	
	/**
	 * Accesses the queued log entries without unqueing any.
	 * @return All queued log entries so far.
	 */
	def queued = builder.toVector
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(error: Option[Throwable], message: String): Unit = builder += (error -> message)
	
	
	// OTHER    ---------------------------
	
	/**
	 * Removes and returns all of the queued log entries
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
	def logAllUsing(log: Logger) = queued.foreach { case (error, message) => log(error, message) }
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
		res.foreach { case (error, message) => log(error, message) }
		res
	}
	/**
	 * Logs, removes and returns all queued log entries
	 * @param log Logger to process the queued entries with
	 * @return All queued (and now logged) entries
	 */
	def popAndLogAll()(implicit log: Logger) = popAndLogAllUsing(log)
	
	/**
	 * Removes any queued log entries
	 */
	def clear() = builder.clear()
}
