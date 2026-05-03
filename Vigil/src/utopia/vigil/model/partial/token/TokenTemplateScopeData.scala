package utopia.vigil.model.partial.token

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.time.Now
import utopia.vigil.model.factory.token.TokenTemplateScopeFactory
import utopia.vigil.model.partial.scope.{ScopeRightData, ScopeRightDataLike}

import java.time.Instant

object TokenTemplateScopeData extends FromModelFactoryWithSchema[TokenTemplateScopeData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("scopeId", IntType, Single("scope_id")), 
			PropertyDeclaration("templateId", IntType, Single("template_id")), PropertyDeclaration("created", 
			InstantType, isOptional = true), PropertyDeclaration("usable", BooleanType, Empty, false)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		TokenTemplateScopeData(valid("scopeId").getInt, valid("templateId").getInt, 
			valid("created").getInstant, valid("usable").getBoolean)
}

/**
  * Links a (granted) scope to a token template
  * @param scopeId    ID of the granted or accessible scope
  * @param templateId ID of the template that grants this scope
  * @param created    Time when this scope right was added to the database
  * @param usable     Whether the linked scope is directly accessible. 
  *                   False if the scope is only applied when granting access for other 
  *                   authentication methods.
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenTemplateScopeData(scopeId: Int, templateId: Int, created: Instant = Now, 
	usable: Boolean = false) 
	extends TokenTemplateScopeFactory[TokenTemplateScopeData] with ScopeRightData 
		with ScopeRightDataLike[TokenTemplateScopeData]
{
	// IMPLEMENTED	--------------------
	
	override def toModel = super[ScopeRightData].toModel ++ Model(Single("templateId" -> templateId))
	
	override def copyScopeRight(scopeId: Int, created: Instant, usable: Boolean) = 
		copy(scopeId = scopeId, created = created, usable = usable)
	
	override def withTemplateId(templateId: Int) = copy(templateId = templateId)
}

