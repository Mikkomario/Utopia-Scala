package utopia.scribe.api.database.storable.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.factory.logging.IssueFactory
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing IssueDbModel instances and for inserting issues to the database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueDbModel 
	extends StorableFactory[IssueDbModel, Issue, IssueData] with FromIdFactory[Int, IssueDbModel] 
		with HasIdProperty with IssueFactory[IssueDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with contexts
	  */
	lazy val context = property("context")
	
	/**
	  * Database property used for interacting with severities
	  */
	lazy val severity = property("severityLevel")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.issue
	
	override def apply(data: IssueData): IssueDbModel = 
		apply(None, data.context, Some(data.severity), Some(data.created))
	
	/**
	  * @param context Program context where this issue occurred or was logged. Should be unique.
	  * @return A model containing only the specified context
	  */
	override def withContext(context: String) = apply(context = context)
	
	/**
	  * @param created Time when this issue first occurred or was first recorded
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param severity The estimated severity of this issue
	  * @return A model containing only the specified severity
	  */
	override def withSeverity(severity: Severity) = apply(severity = Some(severity))
	
	override protected def complete(id: Value, data: IssueData) = Issue(id.getInt, data)
}

/**
  * Used for interacting with Issues in the database
  * @param id issue database id
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
case class IssueDbModel(id: Option[Int] = None, context: String = "", severity: Option[Severity] = None, 
	created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, IssueDbModel] 
		with IssueFactory[IssueDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(IssueDbModel.id.name -> id, IssueDbModel.context.name -> context, 
			IssueDbModel.severity.name -> severity.map[Value] { e => e.level }.getOrElse(Value.empty), 
			IssueDbModel.created.name -> created)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = IssueDbModel.table
	
	/**
	  * @param context Program context where this issue occurred or was logged. Should be unique.
	  * @return A new copy of this model with the specified context
	  */
	override def withContext(context: String) = copy(context = context)
	
	/**
	  * @param created Time when this issue first occurred or was first recorded
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param severity The estimated severity of this issue
	  * @return A new copy of this model with the specified severity
	  */
	override def withSeverity(severity: Severity) = copy(severity = Some(severity))
}

