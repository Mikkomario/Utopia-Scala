package utopia.scribe.database.model.logging

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.database.factory.logging.ProblemFactory
import utopia.scribe.model.enumeration.Severity
import utopia.scribe.model.partial.logging.ProblemData
import utopia.scribe.model.stored.logging.Problem
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ProblemModel instances and for inserting Problems to the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object ProblemModel extends DataInserter[ProblemModel, Problem, ProblemData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Problem context
	  */
	val contextAttName = "context"
	
	/**
	  * Name of the property that contains Problem severity
	  */
	val severityAttName = "severity"
	
	/**
	  * Name of the property that contains Problem created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Problem context
	  */
	def contextColumn = table(contextAttName)
	
	/**
	  * Column that contains Problem severity
	  */
	def severityColumn = table(severityAttName)
	
	/**
	  * Column that contains Problem created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ProblemFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ProblemData) = 
		apply(None, Some(data.context), Some(data.severity), Some(data.created))
	
	override def complete(id: Value, data: ProblemData) = Problem(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param context Program context where this problem occurred or was logged. Should be unique.
	  * @return A model containing only the specified context
	  */
	def withContext(context: String) = apply(context = Some(context))
	
	/**
	  * @param created Time when this problem first occurred
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A Problem id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param severity Severity of this problem
	  * @return A model containing only the specified severity
	  */
	def withSeverity(severity: Severity) = apply(severity = Some(severity))
}

/**
  * Used for interacting with Problems in the database
  * @param id Problem database id
  * @param context Program context where this problem occurred or was logged. Should be unique.
  * @param severity Severity of this problem
  * @param created Time when this problem first occurred
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class ProblemModel(id: Option[Int] = None, context: Option[String] = None, 
	severity: Option[Severity] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[Problem]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ProblemModel.factory
	
	override def valueProperties = {
		import ProblemModel._
		Vector("id" -> id, contextAttName -> context, severityAttName -> severity.map { _.id }, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param context A new context
	  * @return A new copy of this model with the specified context
	  */
	def withContext(context: String) = copy(context = Some(context))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param severity A new severity
	  * @return A new copy of this model with the specified severity
	  */
	def withSeverity(severity: Severity) = copy(severity = Some(severity))
}

