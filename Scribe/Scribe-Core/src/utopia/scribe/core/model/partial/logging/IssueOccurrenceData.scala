package utopia.scribe.core.model.partial.logging

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.VectorType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

import java.time.Instant

object IssueOccurrenceData extends FromModelFactoryWithSchema[IssueOccurrenceData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("caseId", IntType, Vector("case_id")), 
			PropertyDeclaration("errorMessages", VectorType, Vector("error_messages"), isOptional = true), 
			PropertyDeclaration("created", InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueOccurrenceData(valid("caseId").getInt, 
			valid("errorMessages").notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => v.getString }; case None => Vector.empty }, 
			valid("created").getInstant)
}

/**
  * Represents a specific occurrence of a recorded issue
  * @param caseId Id of the issue variant that occurred
  * @param errorMessages Error messages listed in the stack trace
  * @param created Time when the issue occurred or was recorded
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueOccurrenceData(caseId: Int, errorMessages: Vector[String] = Vector.empty, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("caseId" -> caseId, "errorMessages" -> errorMessages.map { v => v }, 
			"created" -> created))
}

