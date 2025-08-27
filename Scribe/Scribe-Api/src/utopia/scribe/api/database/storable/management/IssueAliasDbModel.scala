package utopia.scribe.api.database.storable.management

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.management.IssueAliasFactory
import utopia.scribe.core.model.partial.management.IssueAliasData
import utopia.scribe.core.model.stored.management.IssueAlias
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing IssueAliasDbModel instances and for inserting issue aliases to the 
  * database
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
object IssueAliasDbModel 
	extends StorableFactory[IssueAliasDbModel, IssueAlias, IssueAliasData] 
		with FromIdFactory[Int, IssueAliasDbModel] with HasIdProperty 
		with IssueAliasFactory[IssueAliasDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with issue ids
	  */
	lazy val issueId = property("issueId")
	
	/**
	  * Database property used for interacting with aliases
	  */
	lazy val alias = property("alias")
	
	/**
	  * Database property used for interacting with new severities
	  */
	lazy val newSeverity = property("newSeverity")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.issueAlias
	
	override def apply(data: IssueAliasData): IssueAliasDbModel = 
		apply(None, Some(data.issueId), data.alias, Some(data.newSeverity), Some(data.created))
	
	override def withAlias(alias: String) = apply(alias = alias)
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withIssueId(issueId: Int) = apply(issueId = Some(issueId))
	
	override def withNewSeverity(newSeverity: Int) = apply(newSeverity = Some(newSeverity))
	
	override protected def complete(id: Value, data: IssueAliasData) = IssueAlias(id.getInt, data)
}

/**
  * Used for interacting with IssueAliases in the database
  * @param id issue alias database id
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class IssueAliasDbModel(id: Option[Int] = None, issueId: Option[Int] = None, alias: String = "", 
	newSeverity: Option[Int] = None, created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, IssueAliasDbModel] 
		with IssueAliasFactory[IssueAliasDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(IssueAliasDbModel.id.name -> id, IssueAliasDbModel.issueId.name -> issueId, 
			IssueAliasDbModel.alias.name -> alias, IssueAliasDbModel.newSeverity.name -> newSeverity, 
			IssueAliasDbModel.created.name -> created)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = IssueAliasDbModel.table
	
	override def withAlias(alias: String) = copy(alias = alias)
	
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	override def withIssueId(issueId: Int) = copy(issueId = Some(issueId))
	
	override def withNewSeverity(newSeverity: Int) = copy(newSeverity = Some(newSeverity))
}

