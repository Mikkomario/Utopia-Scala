package utopia.scribe.database.access.single.logging

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.scribe.database.factory.logging.ProblemFactory
import utopia.scribe.database.model.logging.ProblemModel
import utopia.scribe.model.enumeration.Severity
import utopia.scribe.model.stored.logging.Problem
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Problems.
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait UniqueProblemAccess 
	extends SingleRowModelAccess[Problem] with DistinctModelAccess[Problem, Option[Problem], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * 
		Program context where this problem occurred or was logged. Should be unique.. None if no instance (or value)
	  *  was found.
	  */
	def context(implicit connection: Connection) = pullColumn(model.contextColumn).string
	
	/**
	  * Severity of this problem. None if no instance (or value) was found.
	  */
	def severity(implicit connection: Connection) = 
		pullColumn(model.severityColumn).int.flatMap(Severity.findForId)
	
	/**
	  * Time when this problem first occurred. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the context of the targeted Problem instance(s)
	  * @param newContext A new context to assign
	  * @return Whether any Problem instance was affected
	  */
	def context_=(newContext: String)(implicit connection: Connection) = 
		putColumn(model.contextColumn, newContext)
	
	/**
	  * Updates the created of the targeted Problem instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Problem instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the severity of the targeted Problem instance(s)
	  * @param newSeverity A new severity to assign
	  * @return Whether any Problem instance was affected
	  */
	def severity_=(newSeverity: Severity)(implicit connection: Connection) = 
		putColumn(model.severityColumn, newSeverity.id)
}

