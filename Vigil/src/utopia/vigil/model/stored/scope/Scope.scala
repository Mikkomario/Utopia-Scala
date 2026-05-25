package utopia.vigil.model.stored.scope

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.factory.scope.ScopeFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeData

object Scope extends StandardStoredFactory[ScopeData, Scope]
{
	// ATTRIBUTES	--------------------
	
	/**
	 * Maximum length of a scope key
	 */
	val maxKeyLength = 128
	
	override val dataFactory = ScopeData
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new scope
	  * @param id   ID of this scope in the database
	  * @param data Wrapped scope data
	  * @return scope with the specified id and wrapped data
	  */
	def apply(id: Int, data: ScopeData): Scope = _Scope(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the scope trait
	  * @param id   ID of this scope in the database
	  * @param data Wrapped scope data
	  * @author Mikko Hilpinen
	  * @since 24.05.2026
	  */
	private case class _Scope(id: Int, data: ScopeData) extends Scope
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: ScopeData) = copy(data = data)
	}
}

/**
  * Represents a scope that has already been stored in the database. 
  * Used for limiting authorization to certain features or areas
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait Scope 
	extends StoredModelConvertible[ScopeData] with FromIdFactory[Int, Scope] 
		with ScopeFactoryWrapper[ScopeData, Scope] with ScopeTarget
{
	// ATTRIBUTES	--------------------
	
	override val isValid: Boolean = true
	
	
	// IMPLEMENTED	--------------------
	
	override def key: String = data.key
	
	override protected def wrappedFactory = data
}

