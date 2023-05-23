package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.scribe.core.model.combined.logging.ContextualIssueVariant
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual contextual issue variants, based on their variant id
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
case class DbSingleContextualIssueVariant(id: Int) 
	extends UniqueContextualIssueVariantAccess with SingleIntIdModelAccess[ContextualIssueVariant]

