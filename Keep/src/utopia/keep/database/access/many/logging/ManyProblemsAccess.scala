package utopia.keep.database.access.many.logging

import java.time.Instant
import utopia.flow.generic.ValueConversions._
import utopia.keep.database.factory.logging.ProblemFactory
import utopia.keep.database.model.logging.ProblemModel
import utopia.keep.model.enumeration.Severity
import utopia.keep.model.stored.logging.Problem
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyProblemsAccess
{
	// NESTED	--------------------
	
	private class ManyProblemsSubView(override val parent: ManyRowModelAccess[Problem], 
		override val filterCondition: Condition) 
		extends ManyProblemsAccess with SubView
}

/**
  * A common trait for access points which target multiple Problems at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait ManyProblemsAccess extends ManyRowModelAccess[Problem] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * contexts of the accessible Problems
	  */
	def contexts(implicit connection: Connection) = pullColumn(model.contextColumn).map { v => v.getString }
	
	/**
	  * severities of the accessible Problems
	  */
	def severities(implicit connection: Connection) = 
		pullColumn(model.severityColumn).flatMap { _.int }.flatMap(Severity.findForId)
	
	/**
	  * creationTimes of the accessible Problems
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ProblemModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyProblemsAccess = 
		new ManyProblemsAccess.ManyProblemsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the context of the targeted Problem instance(s)
	  * @param newContext A new context to assign
	  * @return Whether any Problem instance was affected
	  */
	def contexts_=(newContext: String)(implicit connection: Connection) = 
		putColumn(model.contextColumn, newContext)
	
	/**
	  * Updates the created of the targeted Problem instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Problem instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the severity of the targeted Problem instance(s)
	  * @param newSeverity A new severity to assign
	  * @return Whether any Problem instance was affected
	  */
	def severities_=(newSeverity: Severity)(implicit connection: Connection) = 
		putColumn(model.severityColumn, newSeverity.id)
}

