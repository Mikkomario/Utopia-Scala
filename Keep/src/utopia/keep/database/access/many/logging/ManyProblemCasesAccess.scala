package utopia.keep.database.access.many.logging

import java.time.Instant
import utopia.flow.generic.ValueConversions._
import utopia.keep.database.factory.logging.ProblemCaseFactory
import utopia.keep.database.model.logging.ProblemCaseModel
import utopia.keep.model.stored.logging.ProblemCase
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyProblemCasesAccess
{
	// NESTED	--------------------
	
	private class ManyProblemCasesSubView(override val parent: ManyRowModelAccess[ProblemCase], 
		override val filterCondition: Condition) 
		extends ManyProblemCasesAccess with SubView
}

/**
  * A common trait for access points which target multiple ProblemCases at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait ManyProblemCasesAccess extends ManyRowModelAccess[ProblemCase] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * problemIds of the accessible ProblemCases
	  */
	def problemIds(implicit connection: Connection) = pullColumn(model.problemIdColumn).map { v => v.getInt }
	
	/**
	  * detailss of the accessible ProblemCases
	  */
	def detailss(implicit connection: Connection) = pullColumn(model.detailsColumn).flatMap { _.string }
	
	/**
	  * stacks of the accessible ProblemCases
	  */
	def stacks(implicit connection: Connection) = pullColumn(model.stackColumn).flatMap { _.string }
	
	/**
	  * creationTimes of the accessible ProblemCases
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemCaseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemCaseFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyProblemCasesAccess = 
		new ManyProblemCasesAccess.ManyProblemCasesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ProblemCase instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the details of the targeted ProblemCase instance(s)
	  * @param newDetails A new details to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def detailss_=(newDetails: String)(implicit connection: Connection) = 
		putColumn(model.detailsColumn, newDetails)
	
	/**
	  * Updates the problemId of the targeted ProblemCase instance(s)
	  * @param newProblemId A new problemId to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def problemIds_=(newProblemId: Int)(implicit connection: Connection) = 
		putColumn(model.problemIdColumn, newProblemId)
	
	/**
	  * Updates the stack of the targeted ProblemCase instance(s)
	  * @param newStack A new stack to assign
	  * @return Whether any ProblemCase instance was affected
	  */
	def stacks_=(newStack: String)(implicit connection: Connection) = putColumn(model.stackColumn, newStack)
}

