package utopia.vigil.model.partial.token

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.vigil.model.factory.token.TokenGrantRightFactory

object TokenGrantRightData extends FromModelFactoryWithSchema[TokenGrantRightData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("ownerTemplateId", IntType, Single("owner_template_id")), 
			PropertyDeclaration("grantedTemplateId", IntType, Single("granted_template_id")), 
			PropertyDeclaration("revokes", BooleanType, Empty, false)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		TokenGrantRightData(valid("ownerTemplateId").getInt, valid("grantedTemplateId").getInt, 
			valid("revokes").getBoolean)
}

/**
  * Used for allowing certain token types (templates) to generate new tokens of other types
  * @param ownerTemplateId   ID of the token template that has been given the right to generate 
  *                          new tokens
  * @param grantedTemplateId ID of the template applied to the generated tokens
  * @param revokes           Whether generating a new token revokes the token used for 
  *                          authorizing that action
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenGrantRightData(ownerTemplateId: Int, grantedTemplateId: Int, revokes: Boolean = false) 
	extends TokenGrantRightFactory[TokenGrantRightData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("ownerTemplateId" -> ownerTemplateId, "grantedTemplateId" -> grantedTemplateId, 
			"revokes" -> revokes))
	
	override def withGrantedTemplateId(grantedTemplateId: Int) = copy(grantedTemplateId = grantedTemplateId)
	
	override def withOwnerTemplateId(ownerTemplateId: Int) = copy(ownerTemplateId = ownerTemplateId)
	
	override def withRevokes(revokes: Boolean) = copy(revokes = revokes)
}

