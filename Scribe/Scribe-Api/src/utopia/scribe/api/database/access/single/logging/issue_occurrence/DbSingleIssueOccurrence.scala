package utopia.scribe.api.database.access.single.logging.issue_occurrence

import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual issue occurrences, based on their id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class DbSingleIssueOccurrence(id: Int) 
	extends UniqueIssueOccurrenceAccess with SingleIntIdModelAccess[IssueOccurrence]

