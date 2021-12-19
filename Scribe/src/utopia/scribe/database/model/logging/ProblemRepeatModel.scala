package utopia.scribe.database.model.logging

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.scribe.database.factory.logging.ProblemRepeatFactory
import utopia.scribe.model.partial.logging.ProblemRepeatData
import utopia.scribe.model.stored.logging.ProblemRepeat
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ProblemRepeatModel instances and for inserting ProblemRepeats to the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object ProblemRepeatModel extends DataInserter[ProblemRepeatModel, ProblemRepeat, ProblemRepeatData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains ProblemRepeat caseId
	  */
	val caseIdAttName = "caseId"
	
	/**
	  * Name of the property that contains ProblemRepeat created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains ProblemRepeat caseId
	  */
	def caseIdColumn = table(caseIdAttName)
	
	/**
	  * Column that contains ProblemRepeat created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ProblemRepeatFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ProblemRepeatData) = apply(None, Some(data.caseId), Some(data.created))
	
	override def complete(id: Value, data: ProblemRepeatData) = ProblemRepeat(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId Id of the problem case that repeated
	  * @return A model containing only the specified caseId
	  */
	def withCaseId(caseId: Int) = apply(caseId = Some(caseId))
	
	/**
	  * @param created Time when that case repeated itself
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A ProblemRepeat id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with ProblemRepeats in the database
  * @param id ProblemRepeat database id
  * @param caseId Id of the problem case that repeated
  * @param created Time when that case repeated itself
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemRepeatModel(id: Option[Int] = None, caseId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[ProblemRepeat]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemRepeatModel.factory
	
	override def valueProperties = {
		import ProblemRepeatModel._
		Vector("id" -> id, caseIdAttName -> caseId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId A new caseId
	  * @return A new copy of this model with the specified caseId
	  */
	def withCaseId(caseId: Int) = copy(caseId = Some(caseId))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
}

