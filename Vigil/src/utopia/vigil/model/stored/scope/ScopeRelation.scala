package utopia.vigil.model.stored.scope

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.scope.relation.AccessScopeRelation
import utopia.vigil.model.factory.scope.ScopeRelationFactoryWrapper
import utopia.vigil.model.partial.scope.ScopeRelationData

object ScopeRelation extends StandardStoredFactory[ScopeRelationData, ScopeRelation]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = ScopeRelationData
}

/**
  * Represents a scope relation that has already been stored in the database. 
  * Documents that possessing one scope grants another access to another scope
  * @param id   ID of this scope relation in the database
  * @param data Wrapped scope relation data
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class ScopeRelation(id: Int, data: ScopeRelationData) 
	extends StoredModelConvertible[ScopeRelationData] with FromIdFactory[Int, ScopeRelation] 
		with ScopeRelationFactoryWrapper[ScopeRelationData, ScopeRelation]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this scope relation in the database
	  */
	def access = AccessScopeRelation(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: ScopeRelationData) = copy(data = data)
}

