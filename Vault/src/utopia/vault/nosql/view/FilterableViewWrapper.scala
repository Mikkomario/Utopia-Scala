package utopia.vault.nosql.view

import utopia.vault.sql.{Condition, SqlTarget}

/**
 * Common trait for instances which implement [[FilterableView]] by wrapping such a view.
 *
 * @author Mikko Hilpinen
 * @since 02.06.2025, v1.21.1
 * @tparam A Type of wrapped views
 */
trait FilterableViewWrapper[+A <: FilterableView[A]] extends FilterableView[A]
{
	// COMPUTED ----------------------
	
	/**
	 * @return The wrapped view
	 */
	protected def wrapped: A
	
	
	// IMPLEMENTED  ------------------
	
	override protected def self: A = wrapped
	override def target: SqlTarget = wrapped.target
	override def accessCondition: Option[Condition] = wrapped.accessCondition
	override def apply(condition: Condition): A = wrapped(condition)
}
