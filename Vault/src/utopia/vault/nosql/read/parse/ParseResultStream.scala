package utopia.vault.nosql.read.parse

import utopia.vault.model.mutable.ResultStream

import scala.language.implicitConversions

object ParseResultStream
{
	// IMPLICIT --------------------------
	
	implicit def apply[A](f: ResultStream => A): ParseResultStream[A] = new _ParseResultStream[A](f)
	
	
	// NESTED   --------------------------
	
	private class _ParseResultStream[+A](f: ResultStream => A) extends ParseResultStream[A]
	{
		override def apply(stream: ResultStream): A = f(stream)
	}
}

/**
 * Common trait for (factory) functions used for parsing result stream data
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait ParseResultStream[+A]
{
	/**
	 * @param stream A result stream
	 * @return Parsed results
	 */
	def apply(stream: ResultStream): A
}
