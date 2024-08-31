package utopia.logos.database.access.single.text.word

import utopia.logos.model.stored.text.StoredWord
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual words, based on their id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleWord(id: Int) extends UniqueWordAccess with SingleIntIdModelAccess[StoredWord]

