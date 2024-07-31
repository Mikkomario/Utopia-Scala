package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.api.database.factory.logging.IssueInstancesFactory
import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.Condition

object UniqueIssueInstancesAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition A search condition that returns a distinct issue
	  * @return Access to the issue that fulfills the specified condition
	  */
	def apply(condition: Condition): UniqueIssueInstancesAccess =
		 new _UniqueIssueInstancesAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private class _UniqueIssueInstancesAccess(override val accessCondition: Option[Condition]) 
		extends UniqueIssueInstancesAccess
}

/**
  * Common trait for access point that target the instances (occurrences) of a single distinct issue.
  * @author Mikko Hilpinen
  * @since 03.08.2023, v1.0
  */
trait UniqueIssueInstancesAccess extends UniqueIssueAccessLike[IssueInstances]
{
	// IMPLEMENTED	--------------------
	
	override def factory: FromResultFactory[IssueInstances] = IssueInstancesFactory
}

