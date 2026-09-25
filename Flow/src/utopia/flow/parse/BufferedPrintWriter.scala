package utopia.flow.parse

import utopia.flow.parse.BufferedPrintWriter.defaultBufferSize

import java.io.{BufferedWriter, Writer}

object BufferedPrintWriter
{
	val defaultBufferSize = 8192
}

/**
 * A writer implementation that wraps another writer and provides an interface similar to that of PrintWriter.
 * However, unlike PrintWriter, this writer throws write errors instead of silently ignoring them.
 * @param output The underlying writer to wrap
 * @param bufferSize Applied buffer size. Default = 8M.
 * @param autoFlush Whether to automatically flush this writer whenever println is called. Default = false.
 * @author Mikko Hilpinen
 * @since 25.09.2026, v2.9
 */
class BufferedPrintWriter(output: Writer, bufferSize: Int = defaultBufferSize, autoFlush: Boolean = false)
	extends Writer
{
	// ATTRIBUTES   ---------------------
	
	// Makes sure the delegate is buffered
	private val writer = output match {
		case buffered: BufferedWriter => buffered
		case writer => new BufferedWriter(writer, bufferSize)
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override def write(cbuf: Array[Char], off: Int, len: Int) = writer.write(cbuf, off, len)
	override def write(c: Int) = writer.write(c)
	override def write(str: String, off: Int, len: Int) = writer.write(str, off, len)
	
	override def flush() = writer.flush()
	override def close() = writer.close()
	
	
	// OTHER    ---------------------
	
	/**
	 * Writes a newline character
	 */
	def println() = {
		writer.newLine()
		if (autoFlush)
			writer.flush()
	}
	/**
	 * Writes the specified string, followed by a newline character
	 * @param str String to write
	 */
	def println(str: String): Unit = {
		writer.write(str)
		println()
	}
	
	/**
	 * Writes a string
	 * @param str String to write
	 */
	def print(str: String) = writer.write(str)
	/**
	 * Writes a single character
	 * @param char Character to write
	 */
	def print(char: Char) = writer.write(char)
}
