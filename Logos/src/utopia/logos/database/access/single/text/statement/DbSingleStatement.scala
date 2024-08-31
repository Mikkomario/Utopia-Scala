package utopia.logos.database.access.single.text.statement

import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual statements, based on their id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleStatement(id: Int) extends UniqueStatementAccess with SingleIntIdModelAccess[StoredStatement]

