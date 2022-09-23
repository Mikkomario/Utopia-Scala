package utopia.scribe.database.model.logging

import utopia.flow.collection.value.typeless.Value

import java.time.Instant
import utopia.flow.generic.ValueConversions._
import utopia.scribe.database.factory.logging.ProblemCaseFactory
import utopia.scribe.model.partial.logging.ProblemCaseData
import utopia.scribe.model.stored.logging.ProblemCase
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ProblemCaseModel instances and for inserting ProblemCases to the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object ProblemCaseModel extends DataInserter[ProblemCaseModel, ProblemCase, ProblemCaseData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains ProblemCase problemId
	  */
	val problemIdAttName = "problemId"
	
	/**
	  * Name of the property that contains ProblemCase details
	  */
	val detailsAttName = "details"
	
	/**
	  * Name of the property that contains ProblemCase stack
	  */
	val stackAttName = "stack"
	
	/**
	  * Name of the property that contains ProblemCase created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains ProblemCase problemId
	  */
	def problemIdColumn = table(problemIdAttName)
	
	/**
	  * Column that contains ProblemCase details
	  */
	def detailsColumn = table(detailsAttName)
	
	/**
	  * Column that contains ProblemCase stack
	  */
	def stackColumn = table(stackAttName)
	
	/**
	  * Column that contains ProblemCase created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ProblemCaseFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ProblemCaseData) = 
		apply(None, Some(data.problemId), data.details, data.stack, Some(data.created))
	
	override def complete(id: Value, data: ProblemCaseData) = ProblemCase(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this case first occurred
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param details Details about this problem case, like the error message, for example
	  * @return A model containing only the specified details
	  */
	def withDetails(details: String) = apply(details = Some(details))
	
	/**
	  * @param id A ProblemCase id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param problemId Id of the problem that occurred
	  * @return A model containing only the specified problemId
	  */
	def withProblemId(problemId: Int) = apply(problemId = Some(problemId))
	
	/**
	  * @return A model containing only the specified stack
	  */
	def withStack(stack: String) = apply(stack = Some(stack))
}

/**
  * Used for interacting with ProblemCases in the database
  * @param id ProblemCase database id
  * @param problemId Id of the problem that occurred
  * @param details Details about this problem case, like the error message, for example
  * @param created Time when this case first occurred
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemCaseModel(id: Option[Int] = None, problemId: Option[Int] = None, 
	details: Option[String] = None, stack: Option[String] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[ProblemCase]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemCaseModel.factory
	
	override def valueProperties = {
		import ProblemCaseModel._
		Vector("id" -> id, problemIdAttName -> problemId, detailsAttName -> details, stackAttName -> stack, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param details A new details
	  * @return A new copy of this model with the specified details
	  */
	def withDetails(details: String) = copy(details = Some(details))
	
	/**
	  * @param problemId A new problemId
	  * @return A new copy of this model with the specified problemId
	  */
	def withProblemId(problemId: Int) = copy(problemId = Some(problemId))
	
	/**
	  * @param stack A new stack
	  * @return A new copy of this model with the specified stack
	  */
	def withStack(stack: String) = copy(stack = Some(stack))
}

