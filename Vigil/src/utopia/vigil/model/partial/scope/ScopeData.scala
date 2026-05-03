package utopia.vigil.model.partial.scope

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.vigil.model.factory.scope.ScopeFactory

object ScopeData extends FromModelFactoryWithSchema[ScopeData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Pair(PropertyDeclaration("key", StringType), PropertyDeclaration("parentId", 
			IntType, Single("parent_id"), isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		ScopeData(valid("key").getString, valid("parentId").int)
}

/**
  * Used for limiting authorization to certain features or areas
  * @param key      A key used for identifying this scope
  * @param parentId ID of the scope that contains this scope. None if this is a root-level scope.
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class ScopeData(key: String, parentId: Option[Int] = None) 
	extends ScopeFactory[ScopeData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Pair("key" -> key, "parentId" -> parentId))
	
	override def withKey(key: String) = copy(key = key)
	
	override def withParentId(parentId: Int) = copy(parentId = Some(parentId))
}

