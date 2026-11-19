package utopia.flow.util.logging

import utopia.flow.collection.mutable.ShiftingBuffer

import scala.sys.process.ProcessLogger

/**
 * A logger for system processes that buffers the stdout and stderr output.
 * @param maxLength Maximum number of lines to buffer at once. Default = 1024.
 * @author Mikko Hilpinen
 * @since 19.11.2026, v2.9
 */
class BufferingProcessLogger(maxLength: Int = 1024) extends ProcessLogger
{
	// ATTRIBUTES   --------------------
	
	private val buffer = new ShiftingBuffer[(String, Boolean)](maxLength)
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return An iterator that yields the captured stdout lines.
	 *         Doesn't include stderr output.
	 */
	def outputIterator = iterator.filterNot { _._2 }.map { _._1 }
	/**
	 * @return An iterator that yields the captured stderr lines.
	 *         Doesn't include stdout output.
	 */
	def errorIterator = iterator.filter { _._2 }.map { _._1 }
	
	/**
	 * @return An iterator that yields the captured lines.
	 *         Each line is coupled with a boolean that indicates whether the line originated from stderr.
	 */
	def iterator = buffer.iterator
	
	
	// IMPLEMENTED  --------------------
	
	override def toString: String = iterator.map { _._1 }.mkString("\n")
	
	override def out(s: => String): Unit = buffer += (s -> false)
	override def err(s: => String): Unit = buffer += (s -> true)
	
	override def buffer[T](f: => T): T = f
	
	
	// OTHER    ------------------------
	
	/**
	 * @return The currently buffered output.
	 *         Each line is coupled with a boolean that indicates whether the line originated from stderr.
	 */
	def result() = buffer.result()
	
	/**
	 * Clears and returns the collected records.
	 * @return The currently buffered output.
	 *         Each line is coupled with a boolean that indicates whether the line originated from stderr.
	 */
	def popAll() = {
		val r = result()
		clear()
		r
	}
	
	/**
	 * Clears the collected records
	 */
	def clear() = buffer.clear()
}
