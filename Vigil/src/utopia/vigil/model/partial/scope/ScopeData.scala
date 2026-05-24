package utopia.vigil.model.partial.scope

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.vigil.model.factory.scope.ScopeFactory

object ScopeData extends FromModelFactoryWithSchema[ScopeData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = ModelDeclaration(Single(PropertyDeclaration("key", StringType)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = ScopeData(valid("key").getString)
}

/**
  * Used for limiting authorization to certain features or areas
  * @param key A key used for identifying this scope
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class ScopeData(key: String) extends ScopeFactory[ScopeData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Single("key" -> key))
	
	override def withKey(key: String) = copy(key = key)
}

