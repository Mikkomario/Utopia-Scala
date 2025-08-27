package utopia.scribe.core.model.partial.management

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.scribe.core.model.factory.management.CommentFactory

import java.time.Instant

object CommentData extends FromModelFactoryWithSchema[CommentData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("issueVariantId", IntType, Single("issue_variant_id")), 
			PropertyDeclaration("text", StringType), PropertyDeclaration("created", InstantType, 
			isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		CommentData(valid("issueVariantId").getInt, valid("text").getString, valid("created").getInstant)
}

/**
  * Comments an issue
  * @param issueVariantId ID of the commented issue variant
  * @param text           The text contents of this comment
  * @param created        Time when this comment was recorded
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class CommentData(issueVariantId: Int, text: String, created: Instant = Now) 
	extends CommentFactory[CommentData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("issueVariantId" -> issueVariantId, "text" -> text, 
		"created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withIssueVariantId(issueVariantId: Int) = copy(issueVariantId = issueVariantId)
	
	override def withText(text: String) = copy(text = text)
}

