package utopia.vigil.model.cached.scope

/**
 * Common trait for [[ScopeTarget]] implementations that are based on wrapping another such item.
 * @author Mikko Hilpinen
 * @since 25.05.2026, v0.1
 */
trait ScopeTargetWrapper extends ScopeTarget
{
	// ABSTRACT -------------------------
	
	/**
	 * @return The wrapped scope target
	 */
	protected def wrapped: ScopeTarget
	
	
	// IMPLEMENTED  --------------------
	
	override def isValid: Boolean = wrapped.isValid
	override def key: String = wrapped.key
	override def id: Int = wrapped.id
}
