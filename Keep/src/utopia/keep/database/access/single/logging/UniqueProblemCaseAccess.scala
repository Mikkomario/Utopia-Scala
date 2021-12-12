package utopia.keep.database.access.single.logging

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.keep.database.factory.logging.ProblemCaseFactory
import utopia.keep.database.model.logging.ProblemCaseModel
import utopia.keep.model.stored.logging.ProblemCase
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct ProblemCases.
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait UniqueProblemCaseAccess 
	extends SingleRowModelAccess[ProblemCase] 
		with DistinctModelAccess[ProblemCase, Option[ProblemCase], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the problem that occurred. None if no instance (or value) was found.
	  */
	def problemId(implicit connection: Connection) = pullColumn(model.problemIdColumn).int
	
	/**
	  * Details about this problem case, like the error message, 
	  * for example. None if no instance (or value) was found.
	  */
	def details(implicit connection: Connection) = pullColumn(model.detailsColumn).string
	
	/**
	  * The stack of this instance. None if no instance (or value) was found.
	  */
	def stack(implicit connection: Connection) = pullColumn(model.stackColumn).string
	
	/**
	  * Time when this case first occurred. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemCaseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemCaseFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ProblemCase instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the details of the targeted ProblemCase instance(s)
	  * @param newDetails A new details to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def details_=(newDetails: String)(implicit connection: Connection) = 
		putColumn(model.detailsColumn, newDetails)
	
	/**
	  * Updates the problemId of the targeted ProblemCase instance(s)
	  * @param newProblemId A new problemId to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def problemId_=(newProblemId: Int)(implicit connection: Connection) = 
		putColumn(model.problemIdColumn, newProblemId)
	
	/**
	  * Updates the stack of the targeted ProblemCase instance(s)
	  * @param newStack A new stack to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def stack_=(newStack: String)(implicit connection: Connection) = putColumn(model.stackColumn, newStack)
}

