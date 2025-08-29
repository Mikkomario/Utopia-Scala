package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Table}

/**
  * Common trait for interfaces which implement [[AccessColumns]] by wrapping (and mapping) one
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait AccessColumnsWrapper[VO, VVO, +VR, +VVR] extends AccessColumns[VR, VVR]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The wrapped column data access point
	  */
	protected def wrapped: AccessColumns[VO, VVO]
	/**
	  * @param value A value to wrap
	  * @return A wrapped version of the specified value
	  */
	protected def wrapValue(value: VO): VR
	/**
	 * @param values A set of values to wrap
	 * @return A wrapped version of the specified values-set
	 */
	protected def wrapValues(values: VVO): VVR
	
	
	// IMPLEMENTED  ----------------------
	
	override def table: Table = wrapped.table
	
	override def apply(column: Column, distinct: Boolean)(implicit connection: Connection): VR =
		wrapValue(wrapped(column, distinct))
	override def apply(columns: Seq[Column])(implicit connection: Connection): VVR =
		wrapValues(wrapped(columns))
	override def update(column: Column, value: Value)(implicit connection: Connection): Boolean =
		wrapped(column) = value
	override def update(assignments: IterableOnce[(Column, Value)])(implicit connection: Connection): Boolean =
		wrapped.update(assignments)
}
