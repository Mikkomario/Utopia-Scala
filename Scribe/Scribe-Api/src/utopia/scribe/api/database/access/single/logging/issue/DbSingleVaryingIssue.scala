package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.core.model.combined.logging.VaryingIssue
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual varying issues, based on their issue id
  * @author Mikko Hilpinen
  * @since 26.05.2023, v0.1
  */
case class DbSingleVaryingIssue(id: Int) 
	extends UniqueVaryingIssueAccess with SingleIntIdModelAccess[VaryingIssue]

