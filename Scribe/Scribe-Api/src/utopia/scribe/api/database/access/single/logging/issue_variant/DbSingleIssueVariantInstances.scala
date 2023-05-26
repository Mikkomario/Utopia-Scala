package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.core.model.combined.logging.IssueVariantInstances
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual issue variant instances, based on their variant id
  * @author Mikko Hilpinen
  * @since 25.05.2023, v0.1
  */
case class DbSingleIssueVariantInstances(id: Int) 
	extends UniqueIssueVariantInstancesAccess with SingleIntIdModelAccess[IssueVariantInstances]

