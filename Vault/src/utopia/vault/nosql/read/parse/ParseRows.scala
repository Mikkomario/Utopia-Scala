package utopia.vault.nosql.read.parse

import utopia.vault.model.immutable.{Result, Row}

import scala.language.implicitConversions

object ParseRows
{
	// IMPLICIT --------------------------
	
	implicit def apply[A](f: Seq[Row] => A): ParseRows[A] = new _ParseRows[A](f)
	
	
	// NESTED   --------------------------
	
	private class _ParseRows[+A](f: Seq[Row] => A) extends ParseRows[A]
	{
		override def apply(rows: Seq[Row]): A = f(rows)
	}
}

/**
 * Common trait for (factory) interfaces able to parse buffered sequences of database rows
 *
 * @author Mikko Hilpinen
 * @since 11.07.2025, v1.22
 */
trait ParseRows[+A] extends ParseResult[A]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @param rows Rows to parse
	 * @return Item parsed from the specified rows
	 */
	def apply(rows: Seq[Row]): A
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(result: Result): A = apply(result.rows)
}
