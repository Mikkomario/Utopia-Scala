package utopia.vigil.model.partial.scope

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.vigil.model.factory.scope.ScopeRelationFactory

object ScopeRelationData extends FromModelFactoryWithSchema[ScopeRelationData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Pair(PropertyDeclaration("parentScopeId", IntType, Single("parent_scope_id")), 
			PropertyDeclaration("grantedScopeId", IntType, Single("granted_scope_id"))))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		ScopeRelationData(valid("parentScopeId").getInt, valid("grantedScopeId").getInt)
}

/**
  * Documents that possessing one scope grants another access to another scope
  * @param parentScopeId  ID of the scope that grants access to another scope
  * @param grantedScopeId ID of the scope granted by the linked parent scope
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
case class ScopeRelationData(parentScopeId: Int, grantedScopeId: Int) 
	extends ScopeRelationFactory[ScopeRelationData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Pair("parentScopeId" -> parentScopeId, "grantedScopeId" -> grantedScopeId))
	
	override def withGrantedScopeId(grantedScopeId: Int) = copy(grantedScopeId = grantedScopeId)
	
	override def withParentScopeId(parentScopeId: Int) = copy(parentScopeId = parentScopeId)
}

