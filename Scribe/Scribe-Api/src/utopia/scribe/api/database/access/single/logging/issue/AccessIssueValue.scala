package utopia.scribe.api.database.access.single.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * An access point to individual issue's values.
  * A proof of concept. Not intended for production use yet.
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.0.5
  */
@deprecated("Replaced with targeting access classes", "v1.2")
case class AccessIssueValue(access: AccessColumn) extends AccessValue
{
	val model = IssueModel
	
	lazy val id = apply(model.index) { _.getInt }
	lazy val context = apply(model.contextColumn) { _.getString }
	lazy val severity = apply(model.severityColumn)(Severity.fromValue) { _.toValue }
	lazy val created = apply(model.createdColumn) { _.getInstant }
}