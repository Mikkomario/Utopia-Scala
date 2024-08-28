package utopia.logos.database.access.single.text.delimiter

import utopia.logos.model.stored.text.Delimiter
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual delimiters, based on their id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleDelimiter(id: Int) extends UniqueDelimiterAccess with SingleIntIdModelAccess[Delimiter]

