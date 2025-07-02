package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.model.logging.IssueModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.model.template.HasTable
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.columns.AccessValues

/**
  * Common trait for interfaces that provide access to multiple issue-specific values at a time.
  * NB: At this time (v1.0.5) this is just drafting for new concepts
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.0.5
  */
case class AccessIssueValues(override protected val access: AccessManyColumns with HasTable) extends AccessValues
{
	// ATTRIBUTES   ------------------
	
	private val model = IssueModel
	
	lazy val ids = apply(model.index) { _.getInt }
	lazy val contexts = apply(model.contextColumn) { _.getString }
	lazy val severities = apply(model.severityColumn)(Severity.fromValue)
	lazy val creationTimes = apply(model.createdColumn) { _.getInstant }
}
