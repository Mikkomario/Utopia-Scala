package utopia.scribe.api.database.model.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
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
	  * Name of the property that contains issue occurrence created
	  */
	val createdAttName = "created"
	
	
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
	  * Column that contains issue occurrence created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = IssueOccurrenceFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: IssueOccurrenceData) = 
		apply(None, Some(data.caseId), data.errorMessages, Some(data.created))
	
	override protected def complete(id: Value, data: IssueOccurrenceData) = IssueOccurrence(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId Id of the issue variant that occurred
	  * @return A model containing only the specified case id
	  */
	def withCaseId(caseId: Int) = apply(caseId = Some(caseId))
	
	/**
	  * @param created Time when the issue occurred or was recorded
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param errorMessages Error messages listed in the stack trace
	  * @return A model containing only the specified error messages
	  */
	def withErrorMessages(errorMessages: Vector[String]) = apply(errorMessages = errorMessages)
	
	/**
	  * @param id A issue occurrence id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with IssueOccurrences in the database
  * @param id issue occurrence database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueOccurrenceModel(id: Option[Int] = None, caseId: Option[Int] = None, 
	errorMessages: Vector[String] = Vector.empty, created: Option[Instant] = None) 
	extends StorableWithFactory[IssueOccurrence]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IssueOccurrenceModel.factory
	
	override def valueProperties = {
		import IssueOccurrenceModel._
		Vector("id" -> id, caseIdAttName -> caseId, 
			errorMessagesAttName -> (errorMessages.map { v => v }: Value).toJson, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId Id of the issue variant that occurred
	  * @return A new copy of this model with the specified case id
	  */
	def withCaseId(caseId: Int) = copy(caseId = Some(caseId))
	
	/**
	  * @param created Time when the issue occurred or was recorded
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param errorMessages Error messages listed in the stack trace
	  * @return A new copy of this model with the specified error messages
	  */
	def withErrorMessages(errorMessages: Vector[String]) = copy(errorMessages = errorMessages)
}

