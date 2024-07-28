package utopia.logos.database.access.single.word.statement

import utopia.logos.model.stored.word.Statement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual statements, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DbSingleStatement(id: Int) extends UniqueStatementAccess with SingleIntIdModelAccess[Statement]

