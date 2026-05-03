package utopia.vigil.model.stored.scope

import utopia.vault.store.StandardStoredFactory
import utopia.vigil.model.partial.scope.ScopeRightData

object ScopeRight extends StandardStoredFactory[ScopeRightData, ScopeRight]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = ScopeRightData
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new scope right
	  * @param id   ID of this scope right in the database
	  * @param data Wrapped scope right data
	  * @return scope right with the specified id and wrapped data
	  */
	def apply(id: Int, data: ScopeRightData): ScopeRight = _ScopeRight(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the scope right trait
	  * @param id   ID of this scope right in the database
	  * @param data Wrapped scope right data
	  * @author Mikko Hilpinen
	  * @since 01.05.2026
	  */
	private case class _ScopeRight(id: Int, data: ScopeRightData) extends ScopeRight
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: ScopeRightData) = copy(data = data)
	}
}

/**
  * Represents a scope right that has already been stored in the database. 
  * Links a scope to an authentication method that grants that scope
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRight extends StoredScopeRightLike[ScopeRightData, ScopeRight] with ScopeRightData

