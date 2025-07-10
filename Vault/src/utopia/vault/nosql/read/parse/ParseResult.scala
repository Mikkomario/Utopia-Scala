package utopia.vault.nosql.read.parse

import utopia.vault.model.immutable.Result
import utopia.vault.model.mutable.ResultStream

import scala.language.implicitConversions

object ParseResult
{
	// IMPLICIT --------------------------
	
	implicit def apply[A](f: Result => A): ParseResult[A] = new _ParseResult[A](f)
	
	
	// NESTED   --------------------------
	
	private class _ParseResult[+A](f: Result => A) extends ParseResult[A]
	{
		override def apply(result: Result): A = f(result)
	}
}

/**
 * Common trait for (factory) interfaces able to parse buffered database query results
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait ParseResult[+A] extends ParseResultStream[A]
{
	// ABSTRACT -------------------------
	
	/**
	 * @param result A buffered database query result
	 * @return Parsed results
	 */
	def apply(result: Result): A
	
	
	// IMPLEMENTED  --------------------
	
	override def apply(stream: ResultStream): A = apply(stream.buffer)
}
