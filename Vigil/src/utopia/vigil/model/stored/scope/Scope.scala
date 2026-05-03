package utopia.vigil.model.stored.scope

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.scope.AccessScope
import utopia.vigil.model.factory.scope.ScopeFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeData

object Scope extends StandardStoredFactory[ScopeData, Scope]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = ScopeData
}

/**
  * Represents a scope that has already been stored in the database. 
  * Used for limiting authorization to certain features or areas
  * @param id   ID of this scope in the database
  * @param data Wrapped scope data
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class Scope(id: Int, data: ScopeData) 
	extends StoredModelConvertible[ScopeData] with FromIdFactory[Int, Scope] 
		with ScopeFactoryWrapper[ScopeData, Scope]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this scope in the database
	  */
	def access = AccessScope(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: ScopeData) = copy(data = data)
}

