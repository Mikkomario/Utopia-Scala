package utopia.keep.database.access.single.logging

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.keep.database.factory.logging.ProblemRepeatFactory
import utopia.keep.database.model.logging.ProblemRepeatModel
import utopia.keep.model.stored.logging.ProblemRepeat
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct ProblemRepeats.
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait UniqueProblemRepeatAccess 
	extends SingleRowModelAccess[ProblemRepeat] 
		with DistinctModelAccess[ProblemRepeat, Option[ProblemRepeat], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the problem case that repeated. None if no instance (or value) was found.
	  */
	def caseId(implicit connection: Connection) = pullColumn(model.caseIdColumn).int
	
	/**
	  * Time when that case repeated itself. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemRepeatModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemRepeatFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the caseId of the targeted ProblemRepeat instance(s)
	  * @param newCaseId A new caseId to assign
	  * @return Whether any ProblemRepeat instance was affected
	  */
	def caseId_=(newCaseId: Int)(implicit connection: Connection) = putColumn(model.caseIdColumn, newCaseId)
	
	/**
	  * Updates the created of the targeted ProblemRepeat instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ProblemRepeat instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

