package utopia.flow.util.logging

import utopia.flow.collection.mutable.ShiftingBuffer

import scala.sys.process.ProcessLogger

/**
 * A logger for system processes that captures stderr output.
 * @param maxLines Maximum number of lines to capture at any time
 * @author Mikko Hilpinen
 * @since 19.11.2026, v2.9
 */
class BufferingProcessErrorLogger(maxLines: Int) extends ProcessLogger
{
	// ATTRIBUTES   ---------------------
	
	private val buffer = new ShiftingBuffer[String](maxLines)
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return An iterator that yields the captured lines
	 */
	def linesIterator = buffer.iterator
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString: String = linesIterator.mkString("\n")
	
	override def out(s: => String): Unit = ()
	override def err(s: => String): Unit = buffer += s
	
	override def buffer[T](f: => T): T = f
	
	
	// OTHER    -------------------------
	
	/**
	 * Clears the buffer and returns the removed content as a string
	 * @return The content that was buffered so far
	 */
	def popAll() = {
		val result = toString
		clear()
		result
	}
	
	/**
	 * Clears the buffered content
	 */
	def clear(): Unit = buffer.clear()
}
