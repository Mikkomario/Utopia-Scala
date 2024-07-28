package utopia.logos.database.access.single.word.delimiter

import utopia.logos.model.stored.word.Delimiter
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual delimiters, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DbSingleDelimiter(id: Int) extends UniqueDelimiterAccess with SingleIntIdModelAccess[Delimiter]

