package utopia.scribe.api.database.model.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing IssueModel instances and for inserting issues to the database
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueModel extends DataInserter[IssueModel, Issue, IssueData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains issue context
	  */
	val contextAttName = "context"
	
	/**
	  * Name of the property that contains issue severity
	  */
	val severityAttName = "severityLevel"
	
	/**
	  * Name of the property that contains issue created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains issue context
	  */
	def contextColumn = table(contextAttName)
	
	/**
	  * Column that contains issue severity
	  */
	def severityColumn = table(severityAttName)
	
	/**
	  * Column that contains issue created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = IssueFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: IssueData) = apply(None, data.context, Some(data.severity.level), 
		Some(data.created))
	
	override protected def complete(id: Value, data: IssueData) = Issue(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param context Program context where this issue occurred or was logged. Should be unique.
	  * @return A model containing only the specified context
	  */
	def withContext(context: String) = apply(context = context)
	
	/**
	  * @param created Time when this issue first occurred or was first recorded
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A issue id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param severity The estimated severity of this issue
	  * @return A model containing only the specified severity
	  */
	def withSeverity(severity: Severity) = apply(severity = Some(severity.level))
}

/**
  * Used for interacting with Issues in the database
  * @param id issue database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueModel(id: Option[Int] = None, context: String = "", severity: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Issue]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IssueModel.factory
	
	override def valueProperties = {
		import IssueModel._
		Vector("id" -> id, contextAttName -> context, severityAttName -> severity, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param context Program context where this issue occurred or was logged. Should be unique.
	  * @return A new copy of this model with the specified context
	  */
	def withContext(context: String) = copy(context = context)
	
	/**
	  * @param created Time when this issue first occurred or was first recorded
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param severity The estimated severity of this issue
	  * @return A new copy of this model with the specified severity
	  */
	def withSeverity(severity: Severity) = copy(severity = Some(severity.level))
}

