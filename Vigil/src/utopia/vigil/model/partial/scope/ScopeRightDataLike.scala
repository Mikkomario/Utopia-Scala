package utopia.vigil.model.partial.scope

import utopia.vigil.model.factory.scope.ScopeRightFactory

import java.time.Instant

/**
  * Common trait for classes which provide read and copy access to scope right properties
  * @tparam Repr Implementing data class or data wrapper class
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDataLike[+Repr] extends HasScopeRightProps with ScopeRightFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Builds a modified copy of this scope right
	  * @param scopeId New scope id to assign. Default = current value.
	  * @param created New created to assign. Default = current value.
	  * @param usable  New usable to assign. Default = current value.
	  * @return A copy of this scope right with the specified properties
	  */
	def copyScopeRight(scopeId: Int = scopeId, created: Instant = created, usable: Boolean = usable): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def withCreated(created: Instant) = copyScopeRight(created = created)
	
	override def withScopeId(scopeId: Int) = copyScopeRight(scopeId = scopeId)
	
	override def withUsable(usable: Boolean) = copyScopeRight(usable = usable)
}

