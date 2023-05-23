package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual issue variants, based on their id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class DbSingleIssueVariant(id: Int) 
	extends UniqueIssueVariantAccess with SingleIntIdModelAccess[IssueVariant]

