package utopia.vault.nosql.read

import utopia.vault.model.immutable.Row
import utopia.vault.model.mutable.ResultStream

import scala.util.Try

/**
 * Common trait for classes which implement [[DbRowReader]] by wrapping another such implementation.
 * @tparam A Type of the individual items pulled from the DB
 * @author Mikko Hilpinen
 * @since 30.11.2025, v2.1
 */
trait DbRowReaderWrapper[+A] extends DbRowReader[A] with DbReaderWrapper[Seq[A]]
{
	// ABSTRACT --------------------------
	
	override protected def wrapped: DbRowReader[A]
	
	
	// IMPLEMENTED  ----------------------
	
	override def shouldParse(row: Row): Boolean = wrapped.shouldParse(row)
	
	override def apply(row: Row): Try[A] = wrapped(row)
	override def apply(stream: ResultStream) = wrapped(stream)
}
