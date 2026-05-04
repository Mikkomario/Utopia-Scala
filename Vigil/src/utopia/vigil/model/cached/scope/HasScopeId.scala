package utopia.vigil.model.cached.scope

/**
 * Common trait for classes that make reference to scopes via scope IDs
 * @author Mikko Hilpinen
 * @since 04.05.2026, v0.1
 */
trait HasScopeId
{
	// ABSTRACT --------------------
	
	/**
	 * @return ID of the referenced scope
	 */
	def scopeId: Int
	
	
	// COMPUTED -------------------
	
	/**
	 * @return The referenced scope
	 */
	def scope = ScopeTarget.id(scopeId)
}
