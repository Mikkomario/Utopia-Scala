package utopia.scribe.core.model.partial.management

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.scribe.core.model.factory.management.IssueAliasFactory

import java.time.Instant

object IssueAliasData extends FromModelFactoryWithSchema[IssueAliasData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("issueId", IntType, Single("issue_id")), 
			PropertyDeclaration("alias", StringType, isOptional = true), PropertyDeclaration("newSeverity", 
			IntType, Single("new_severity")), PropertyDeclaration("created", InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueAliasData(valid("issueId").getInt, valid("alias").getString, valid("newSeverity").getInt, 
			valid("created").getInstant)
}

/**
  * Assigns a more human-readable name to an issue. May also be used to adjust issue severity.
  * @param issueId     ID of the described issue
  * @param alias       Alias given to the issue. Empty if no alias is given.
  * @param newSeverity New severity level assigned for the issue. None if severity is not 
  *                    modified.
  * @param created     Time when this alias was given
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class IssueAliasData(issueId: Int, alias: String, newSeverity: Int, created: Instant = Now) 
	extends IssueAliasFactory[IssueAliasData] with ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("issueId" -> issueId, "alias" -> alias, "newSeverity" -> newSeverity, 
			"created" -> created))
	
	override def withAlias(alias: String) = copy(alias = alias)
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withIssueId(issueId: Int) = copy(issueId = issueId)
	
	override def withNewSeverity(newSeverity: Int) = copy(newSeverity = newSeverity)
}

