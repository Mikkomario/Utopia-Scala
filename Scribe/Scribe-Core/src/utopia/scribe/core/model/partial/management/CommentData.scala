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
		ModelDeclaration(Vector(PropertyDeclaration("issueId", IntType, Single("issue_id")), 
			PropertyDeclaration("text", StringType), PropertyDeclaration("created", InstantType, 
			isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		CommentData(valid("issueId").getInt, valid("text").getString, valid("created").getInstant)
}

/**
  * Comments an issue
  * @param issueId ID of the commented issue
  * @param text    The text contents of this comment
  * @param created Time when this comment was recorded
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class CommentData(issueId: Int, text: String, created: Instant = Now) 
	extends CommentFactory[CommentData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("issueId" -> issueId, "text" -> text, "created" -> created))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withIssueId(issueId: Int) = copy(issueId = issueId)
	
	override def withText(text: String) = copy(text = text)
}

