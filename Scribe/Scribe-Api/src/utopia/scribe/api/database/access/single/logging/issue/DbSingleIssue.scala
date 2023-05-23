package utopia.scribe.api.database.access.single.logging.issue

import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual issues, based on their id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class DbSingleIssue(id: Int) extends UniqueIssueAccess with SingleIntIdModelAccess[Issue]

