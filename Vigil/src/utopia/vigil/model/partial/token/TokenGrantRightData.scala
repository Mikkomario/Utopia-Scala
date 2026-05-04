package utopia.vigil.model.partial.token

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.util.UncertainBoolean
import utopia.vigil.model.factory.token.TokenGrantRightFactory

object TokenGrantRightData extends FromModelFactoryWithSchema[TokenGrantRightData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("ownerTemplateId", IntType, Single("owner_template_id")), 
			PropertyDeclaration("grantedTemplateId", IntType, Single("granted_template_id")), 
			PropertyDeclaration("revokesOriginal", BooleanType, Single("revokes_original"), false), 
			PropertyDeclaration("revokesEarlier", BooleanType, Single("revokes_earlier"), isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		TokenGrantRightData(valid("ownerTemplateId").getInt, valid("grantedTemplateId").getInt, 
			valid("revokesOriginal").getBoolean, UncertainBoolean(valid("revokesEarlier").boolean))
}

/**
  * Used for allowing certain token types (templates) to generate new tokens of other types
  * @param ownerTemplateId   ID of the token template that has been given the right to generate 
  *                          new tokens
  * @param grantedTemplateId ID of the template applied to the generated tokens
  * @param revokesOriginal   Whether generating a new token revokes the token used for 
  *                          authorizing that action
  * @param revokesEarlier    Whether earlier generated tokens should all be revoked when 
  *                          generating new tokens. 
  *                          Uncertain if this may be controlled manually.
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
case class TokenGrantRightData(ownerTemplateId: Int, grantedTemplateId: Int, 
	revokesOriginal: Boolean = false, revokesEarlier: UncertainBoolean = UncertainBoolean) 
	extends TokenGrantRightFactory[TokenGrantRightData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("ownerTemplateId" -> ownerTemplateId, "grantedTemplateId" -> grantedTemplateId, 
			"revokesOriginal" -> revokesOriginal, "revokesEarlier" -> revokesEarlier.exact))
	
	override def withGrantedTemplateId(grantedTemplateId: Int) = copy(grantedTemplateId = grantedTemplateId)
	
	override def withOwnerTemplateId(ownerTemplateId: Int) = copy(ownerTemplateId = ownerTemplateId)
	
	override def withRevokesEarlier(revokesEarlier: UncertainBoolean) = copy(revokesEarlier = revokesEarlier)
	
	override def withRevokesOriginal(revokesOriginal: Boolean) = copy(revokesOriginal = revokesOriginal)
}

