package utopia.scribe.core.model.partial.logging

import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType, PairType, VectorType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

import java.time.Instant

object IssueOccurrenceData extends FromModelFactoryWithSchema[IssueOccurrenceData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = ModelDeclaration(Vector(
		PropertyDeclaration("caseId", IntType, Vector("case_id")),
		PropertyDeclaration("errorMessages", VectorType, Vector("error_messages"), isOptional = true),
		PropertyDeclaration("details", ModelType, isOptional = true),
		PropertyDeclaration("count", IntType, Vector(), 1),
		PropertyDeclaration("occurrencePeriod", PairType, Vector("earliest", "latest", "occurrence_period"),
			Span.singleValue[Instant](Now).toPair.map[Value] { v => v })
	))
	
	
	/**
	  * Ordering that lists issue occurrences by their latest occurrence time (ascending)
	  */
	implicit val chronoOrdering: Ordering[IssueOccurrenceData] = Ordering.by { _.lastOccurrence }
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) =
		IssueOccurrenceData(valid("caseId").getInt, valid("errorMessages").getVector.map { v => v.getString },
			valid("details").getModel, valid("count").getInt,
			Span(valid("occurrencePeriod").getPair.map { v => v.getInstant }))
	
}

/**
  * Represents one or more specific occurrences of a recorded issue
  * @param caseId Id of the issue variant that occurred
  * @param errorMessages Error messages listed in the stack trace. 
  * If multiple occurrences are represented, contains data from the latest occurrence.
  * @param details Additional details concerning these issue occurrences.
  * In case of multiple occurrences, contains only the latest entry for each detail.
  * @param count Number of issue occurrences represented by this entry
  * @param occurrencePeriod The first and last time this set of issues occurred
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueOccurrenceData(caseId: Int, errorMessages: Vector[String] = Vector.empty, 
	details: Model = Model.empty, count: Int = 1, 
	occurrencePeriod: Span[Instant] = Span.singleValue[Instant](Now)) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * The first issue occurrence time covered by this instance
	  */
	def firstOccurrence = occurrencePeriod.start
	
	/**
	  * The last issue occurrence time covered by this instance
	  */
	def lastOccurrence = occurrencePeriod.end
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("caseId" -> caseId, "errorMessages" -> errorMessages.map[Value] { v => v }, 
			"details" -> details, "count" -> count, 
			"occurrencePeriod" -> occurrencePeriod.toPair.map[Value] { v => v }))
}

