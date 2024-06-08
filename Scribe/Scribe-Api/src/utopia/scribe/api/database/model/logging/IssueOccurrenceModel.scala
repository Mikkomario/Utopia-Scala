package utopia.scribe.api.database.model.logging

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.End
import utopia.flow.util.NotEmpty
import utopia.scribe.api.database.factory.logging.IssueOccurrenceFactory
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing IssueOccurrenceModel instances and for inserting issue occurrences to the database
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueOccurrenceModel extends DataInserter[IssueOccurrenceModel, IssueOccurrence, IssueOccurrenceData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains issue occurrence case id
	  */
	val caseIdAttName = "caseId"
	/**
	  * Name of the property that contains issue occurrence error messages
	  */
	val errorMessagesAttName = "errorMessages"
	/**
	  * Name of the property that contains issue occurrence details
	  */
	val detailsAttName = "details"
	/**
	  * Name of the property that contains issue occurrence count
	  */
	val countAttName = "count"
	/**
	  * Name of the property that contains issue occurrence earliest
	  */
	val earliestAttName = "firstOccurrence"
	/**
	  * Name of the property that contains issue occurrence latest
	  */
	val latestAttName = "lastOccurrence"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains issue occurrence case id
	  */
	def caseIdColumn = table(caseIdAttName)
	/**
	  * Column that contains issue occurrence error messages
	  */
	def errorMessagesColumn = table(errorMessagesAttName)
	/**
	  * Column that contains issue occurrence details
	  */
	def detailsColumn = table(detailsAttName)
	/**
	  * Column that contains issue occurrence count
	  */
	def countColumn = table(countAttName)
	/**
	  * Column that contains issue occurrence earliest
	  */
	def earliestColumn = table(earliestAttName)
	/**
	  * Column that contains issue occurrence latest
	  */
	def latestColumn = table(latestAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = IssueOccurrenceFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: IssueOccurrenceData) = 
		apply(None, Some(data.caseId), data.errorMessages, data.details, Some(data.count), 
			Some(data.occurrencePeriod.start), Some(data.occurrencePeriod.end))
	
	override protected def complete(id: Value, data: IssueOccurrenceData) = IssueOccurrence(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param end Targeted end (first or last)
	  * @return Column that matches the earliest or the latest occurrence, based on the specified end
	  */
	def timeColumn(end: End) = end match {
		case First => earliestColumn
		case Last => latestColumn
	}
	
	/**
	  * @param caseId Id of the issue variant that occurred
	  * @return A model containing only the specified case id
	  */
	def withCaseId(caseId: Int) = apply(caseId = Some(caseId))
	
	/**
	  * @param count Number of issue occurrences represented by this entry
	  * @return A model containing only the specified count
	  */
	def withCount(count: Int) = apply(count = Some(count))
	
	/**
	  * @param details Additional details concerning these issue occurrences.
	  * In case of multiple occurrences, contains only the latest entry for each detail.
	  * @return A model containing only the specified details
	  */
	def withDetails(details: Model) = apply(details = details)
	
	/**
	  * @param errorMessages Error messages listed in the stack trace. 
	  * If multiple occurrences are represented, contains data from the latest occurrence.
	  * @return A model containing only the specified error messages
	  */
	def withErrorMessages(errorMessages: Vector[String]) = apply(errorMessages = errorMessages)
	
	/**
	  * @param id A issue occurrence id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param occurrencePeriod The first and last time this set of issues occurred
	  * @return A model containing only the specified occurrence period (sets all 2 values)
	  */
	def withOccurrencePeriod(occurrencePeriod: Span[Instant]) = 
		apply(earliest = Some(occurrencePeriod.start), latest = Some(occurrencePeriod.end))
}

/**
  * Used for interacting with IssueOccurrences in the database
  * @param id issue occurrence database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueOccurrenceModel(id: Option[Int] = None, caseId: Option[Int] = None,
                                errorMessages: Seq[String] = Empty, details: Model = Model.empty,
                                count: Option[Int] = None,
                                earliest: Option[Instant] = None, latest: Option[Instant] = None)
	extends StorableWithFactory[IssueOccurrence]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IssueOccurrenceModel.factory
	
	override def valueProperties = {
		import IssueOccurrenceModel._
		Vector("id" -> id, caseIdAttName -> caseId, 
			errorMessagesAttName ->
				(NotEmpty(errorMessages) match {
					case Some(v) => (v.map[Value] { v => v }.toVector: Value).toJson: Value
					case None => Value.empty
				}),
			detailsAttName -> details.notEmpty.map { _.toJson }, countAttName -> count, 
			earliestAttName -> earliest, latestAttName -> latest)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId Id of the issue variant that occurred
	  * @return A new copy of this model with the specified case id
	  */
	def withCaseId(caseId: Int) = copy(caseId = Some(caseId))
	
	/**
	  * @param count Number of issue occurrences represented by this entry
	  * @return A new copy of this model with the specified count
	  */
	def withCount(count: Int) = copy(count = Some(count))
	
	/**
	  * @param details Additional details concerning these issue occurrences.
	  * In case of multiple occurrences, contains only the latest entry for each detail.
	  * @return A new copy of this model with the specified details
	  */
	def withDetails(details: Model) = copy(details = details)
	
	/**
	  * @param errorMessages Error messages listed in the stack trace. 
	  * If multiple occurrences are represented, contains data from the latest occurrence.
	  * @return A new copy of this model with the specified error messages
	  */
	def withErrorMessages(errorMessages: Vector[String]) = copy(errorMessages = errorMessages)
	
	/**
	  * @param occurrencePeriod The first and last time this set of issues occurred
	  * @return A new copy of this model with the specified occurrence period (sets all 2 values)
	  */
	def withOccurrencePeriod(occurrencePeriod: Span[Instant]) = 
		copy(earliest = Some(occurrencePeriod.start), latest = Some(occurrencePeriod.end))
}

