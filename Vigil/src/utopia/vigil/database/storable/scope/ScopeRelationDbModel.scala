package utopia.vigil.database.storable.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}
import utopia.vigil.database.VigilTables
import utopia.vigil.model.factory.scope.ScopeRelationFactory
import utopia.vigil.model.partial.scope.ScopeRelationData
import utopia.vigil.model.stored.scope.ScopeRelation

/**
  * Used for constructing ScopeRelationDbModel instances and for inserting scope relations to the 
  * database
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
object ScopeRelationDbModel 
	extends StorableFactory[ScopeRelationDbModel, ScopeRelation, ScopeRelationData] 
		with FromIdFactory[Int, ScopeRelationDbModel] with HasIdProperty 
		with ScopeRelationFactory[ScopeRelationDbModel]
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with parent scope ids
	  */
	lazy val parentScopeId = property("parentScopeId")
	
	/**
	  * Database property used for interacting with granted scope ids
	  */
	lazy val grantedScopeId = property("grantedScopeId")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.scopeRelation
	
	override def apply(data: ScopeRelationData): ScopeRelationDbModel = 
		apply(None, Some(data.parentScopeId), Some(data.grantedScopeId))
	
	override def withGrantedScopeId(grantedScopeId: Int) = apply(grantedScopeId = Some(grantedScopeId))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withParentScopeId(parentScopeId: Int) = apply(parentScopeId = Some(parentScopeId))
	
	override protected def complete(id: Value, data: ScopeRelationData) = ScopeRelation(id.getInt, data)
}

/**
  * Used for interacting with ScopeRelations in the database
  * @param id scope relation database id
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class ScopeRelationDbModel(id: Option[Int] = None, parentScopeId: Option[Int] = None, 
	grantedScopeId: Option[Int] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, ScopeRelationDbModel] 
		with ScopeRelationFactory[ScopeRelationDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(ScopeRelationDbModel.id.name -> id, ScopeRelationDbModel.parentScopeId.name -> parentScopeId, 
			ScopeRelationDbModel.grantedScopeId.name -> grantedScopeId)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScopeRelationDbModel.table
	
	override def withGrantedScopeId(grantedScopeId: Int) = copy(grantedScopeId = Some(grantedScopeId))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	override def withParentScopeId(parentScopeId: Int) = copy(parentScopeId = Some(parentScopeId))
}

