package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Table}

/**
  * Common trait for interfaces which implement [[AccessColumns]] by wrapping (and mapping) one
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait AccessColumnsWrapper[VO, +VR] extends AccessColumns[VR]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The wrapped column data access point
	  */
	protected def wrapped: AccessColumns[VO]
	/**
	  * @param value A value to wrap
	  * @return A wrapped version of the specified value
	  */
	protected def wrapValue(value: VO): VR
	
	
	// IMPLEMENTED  ----------------------
	
	override def table: Table = wrapped.table
	
	override def apply(column: Column, distinct: Boolean)(implicit connection: Connection): VR =
		wrapValue(wrapped(column, distinct))
	override def apply(columns: Seq[Column])(implicit connection: Connection): Seq[VR] = wrapped(columns).map(wrapValue)
	override def update(column: Column, value: Value)(implicit connection: Connection): Boolean =
		wrapped(column) = value
	override def update(assignments: IterableOnce[(Column, Value)])(implicit connection: Connection): Boolean =
		wrapped.update(assignments)
}
