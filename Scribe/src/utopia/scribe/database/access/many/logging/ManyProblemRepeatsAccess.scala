package utopia.scribe.database.access.many.logging

import java.time.Instant
import utopia.flow.generic.ValueConversions._
import utopia.scribe.database.factory.logging.ProblemRepeatFactory
import utopia.scribe.database.model.logging.ProblemRepeatModel
import utopia.scribe.model.stored.logging.ProblemRepeat
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyProblemRepeatsAccess
{
	// NESTED	--------------------
	
	private class ManyProblemRepeatsSubView(override val parent: ManyRowModelAccess[ProblemRepeat], 
		override val filterCondition: Condition) 
		extends ManyProblemRepeatsAccess with SubView
}

/**
  * A common trait for access points which target multiple ProblemRepeats at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait ManyProblemRepeatsAccess extends ManyRowModelAccess[ProblemRepeat] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * caseIds of the accessible ProblemRepeats
	  */
	def caseIds(implicit connection: Connection) = pullColumn(model.caseIdColumn).map { v => v.getInt }
	
	/**
	  * creationTimes of the accessible ProblemRepeats
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemRepeatModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemRepeatFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyProblemRepeatsAccess = 
		new ManyProblemRepeatsAccess.ManyProblemRepeatsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the caseId of the targeted ProblemRepeat instance(s)
	  * @param newCaseId A new caseId to assign
	  * @return Whether any ProblemRepeat instance was affected
	  */
	def caseIds_=(newCaseId: Int)(implicit connection: Connection) = putColumn(model.caseIdColumn, newCaseId)
	
	/**
	  * Updates the created of the targeted ProblemRepeat instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ProblemRepeat instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

