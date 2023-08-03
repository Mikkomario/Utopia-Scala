package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.factory.logging.IssueInstancesFactory
import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.{Condition, SqlTarget}

/**
  * The root access point to individual issues where their occurrences are included
  * @author Mikko Hilpinen
  * @since 3.8.2023, v1.0
  */
object DbIssueInstances extends SingleModelAccess[IssueInstances] with UnconditionalView with Indexed
{
	// IMPLEMENTED  ----------------------------
	
	override def factory: FromResultFactory[IssueInstances] = IssueInstancesFactory
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param condition A search condition that yields unique issues
	  * @return Access to the issue that fulfills the specified search condition
	  */
	def filterDistinct(condition: Condition) = UniqueIssueInstancesAccess(condition)
}
