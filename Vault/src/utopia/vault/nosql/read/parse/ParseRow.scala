package utopia.vault.nosql.read.parse

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.vault.model.immutable.{Result, Row}
import utopia.vault.model.mutable.ResultStream
import utopia.vault.util.ErrorHandling

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ParseRow
{
	// IMPLICIT    -------------------------
	
	implicit def apply[A](f: Row => Try[A]): ParseRow[A] = new _ParseRow[A](f)
	
	
	// NESTED   ----------------------------
	
	private class _ParseRow[+A](f: Row => Try[A]) extends ParseRow[A]
	{
		override def apply(row: Row): Try[A] = f(row)
		override def shouldParse(row: Row): Boolean = true
	}
}

/**
 * Common trait for (factory) interfaces able to parse database rows
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait ParseRow[+A] extends ParseResultStream[Seq[A]] with ParseResult[Seq[A]]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @param row Row to parse
	 * @return Item parsed from the row. Failure if parsing failed.
	 */
	def apply(row: Row): Try[A]
	/**
	 * @param row A row
	 * @return Whether that row should be parsed using [[apply]]
	 */
	def shouldParse(row: Row): Boolean
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(stream: ResultStream) =
		OptimizedIndexedSeq.from(stream.rowsIterator.flatMap(tryParse))
	
	override def apply(result: Result): Seq[A] = result.rows.flatMap(tryParse)
	
	
	// OTHER    ---------------------------
	
	/**
	 * Attempts to parse a row, if appropriate
	 * @param row Row to parse
	 * @return Item parsed from the row.
	 *         None if the row was skipped or if parsing failed.
	 */
	def tryParse(row: Row): Option[A] = {
		// Case: Should attempt parsing
		if (shouldParse(row))
			apply(row) match {
				// Case: Parsing succeeded
				case Success(result) => Some(result)
				// Case: Parsing failed => Delegates the error to the main error handling interface
				case Failure(error) =>
					ErrorHandling.modelParsePrinciple.handle(error)
					None
			}
		// Case: Should skip
		else
			None
	}
}
