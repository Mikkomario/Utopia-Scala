package utopia.vault.nosql.read

import utopia.vault.model.template.{HasSelectTarget, HasTable, HasTables}
import utopia.vault.nosql.read.parse.ParseResultStream

/**
  * Common trait for interfaces which target and parse specific database data
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait DbReader[+A] extends ParseResultStream[A] with HasSelectTarget with HasTable with HasTables