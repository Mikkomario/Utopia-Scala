package utopia.scribe.api.database.storable.logging

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.util.NotEmpty
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.logging.IssueOccurrenceFactory
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing IssueOccurrenceDbModel instances and for inserting issue occurrences to 
  * the database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueOccurrenceDbModel 
	extends StorableFactory[IssueOccurrenceDbModel, IssueOccurrence, IssueOccurrenceData] 
		with FromIdFactory[Int, IssueOccurrenceDbModel] with HasIdProperty 
		with IssueOccurrenceFactory[IssueOccurrenceDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	/**
	  * Database property used for interacting with case ids
	  */
	lazy val caseId = property("caseId")
	/**
	  * Database property used for interacting with error messages
	  */
	lazy val errorMessages = property("errorMessages")
	/**
	  * Database property used for interacting with details
	  */
	lazy val details = property("details")
	/**
	  * Database property used for interacting with counts
	  */
	lazy val count = property("count")
	/**
	  * Database property used for interacting with earliests
	  */
	lazy val earliest = property("firstOccurrence")
	/**
	  * Database property used for interacting with latests
	  */
	lazy val latest = property("lastOccurrence")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.issueOccurrence
	
	override def apply(data: IssueOccurrenceData): IssueOccurrenceDbModel = 
		apply(None, Some(data.caseId), data.errorMessages, data.details, Some(data.count), 
			Some(data.occurrencePeriod.start), Some(data.occurrencePeriod.end))
	
	/**
	  * @param caseId Id of the issue variant that occurred
	  * @return A model containing only the specified case id
	  */
	override def withCaseId(caseId: Int) = apply(caseId = Some(caseId))
	
	/**
	  * @param count Number of issue occurrences represented by this entry
	  * @return A model containing only the specified count
	  */
	override def withCount(count: Int) = apply(count = Some(count))
	/**
	  * @param details Additional details concerning these issue occurrences.
	  *                In case of multiple occurrences, contains only the latest entry for each 
	  *                detail.
	  * @return A model containing only the specified details
	  */
	override def withDetails(details: Model) = apply(details = details)
	/**
	  * @param errorMessages Error messages listed in the stack trace. 
	  *                      If multiple occurrences are represented, contains data from the latest 
	  *                      occurrence.
	  * @return A model containing only the specified error messages
	  */
	override def withErrorMessages(errorMessages: Seq[String]) =
		apply(errorMessages = errorMessages)
	override def withId(id: Int) = apply(id = Some(id))
	/**
	  * @param occurrencePeriod The first and last time this set of issues occurred
	  * @return A model containing only the specified occurrence period (sets all 2 values)
	  */
	override def withOccurrencePeriod(occurrencePeriod: Span[Instant]) = 
		apply(earliest = Some(occurrencePeriod.start), latest = Some(occurrencePeriod.end))
	
	override protected def complete(id: Value, data: IssueOccurrenceData) = IssueOccurrence(id.getInt, data)
}

/**
  * Used for interacting with IssueOccurrences in the database
  * @param id issue occurrence database id
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
case class IssueOccurrenceDbModel(id: Option[Int] = None, caseId: Option[Int] = None,
                                  errorMessages: Seq[String] = Empty, details: Model = Model.empty,
                                  count: Option[Int] = None, earliest: Option[Instant] = None,
                                  latest: Option[Instant] = None)
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, IssueOccurrenceDbModel] 
		with IssueOccurrenceFactory[IssueOccurrenceDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = Vector(
		IssueOccurrenceDbModel.id.name -> id,
		IssueOccurrenceDbModel.caseId.name -> caseId,
		IssueOccurrenceDbModel.errorMessages.name ->
			(NotEmpty(errorMessages) match {
				case Some(v) => (v.map[Value] { v => v }: Value).toJson: Value
				case None => Value.empty
			}),
		IssueOccurrenceDbModel.details.name -> details.notEmpty.map { _.toJson },
		IssueOccurrenceDbModel.count.name -> count,
		IssueOccurrenceDbModel.earliest.name -> earliest,
		IssueOccurrenceDbModel.latest.name -> latest)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = IssueOccurrenceDbModel.table
	
	/**
	  * @param caseId Id of the issue variant that occurred
	  * @return A new copy of this model with the specified case id
	  */
	override def withCaseId(caseId: Int) = copy(caseId = Some(caseId))
	/**
	  * @param count Number of issue occurrences represented by this entry
	  * @return A new copy of this model with the specified count
	  */
	override def withCount(count: Int) = copy(count = Some(count))
	/**
	  * @param details Additional details concerning these issue occurrences.
	  *                In case of multiple occurrences, contains only the latest entry for each 
	  *                detail.
	  * @return A new copy of this model with the specified details
	  */
	override def withDetails(details: Model) = copy(details = details)
	/**
	  * @param errorMessages Error messages listed in the stack trace. 
	  *                      If multiple occurrences are represented, contains data from the latest 
	  *                      occurrence.
	  * @return A new copy of this model with the specified error messages
	  */
	override def withErrorMessages(errorMessages: Seq[String]) = copy(errorMessages = errorMessages)
	override def withId(id: Int) = copy(id = Some(id))
	/**
	  * @param occurrencePeriod The first and last time this set of issues occurred
	  * @return A new copy of this model with the specified occurrence period (sets all 2 values)
	  */
	override def withOccurrencePeriod(occurrencePeriod: Span[Instant]) = 
		copy(earliest = Some(occurrencePeriod.start), latest = Some(occurrencePeriod.end))
}

