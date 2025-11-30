package utopia.vault.nosql.read

import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.model.mutable.ResultStream
import utopia.vault.sql.SqlTarget

/**
 * Common trait for implementations of [[DbReader]] that wrap another such instance
 * @tparam A Type of the items read from the DB, in the form they are read (i.e. as a full result group)
 * @author Mikko Hilpinen
 * @since 30.11.2025, v2.1
 */
trait DbReaderWrapper[+A] extends DbReader[A]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The wrapped [[DbReader]] implementation
	 */
	protected def wrapped: DbReader[A]
	
	
	// IMPLEMENTED  ----------------------
	
	override def target: SqlTarget = wrapped.target
	override def table: Table = wrapped.table
	override def tables: Seq[Table] = wrapped.tables
	override def selectTarget: SelectTarget = wrapped.selectTarget
	
	override def apply(stream: ResultStream): A = wrapped(stream)
}
