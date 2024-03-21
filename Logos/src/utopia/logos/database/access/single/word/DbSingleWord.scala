package utopia.logos.database.access.single.word

import utopia.logos.model.stored.word.Word
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual words, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class DbSingleWord(id: Int) extends UniqueWordAccess with SingleIntIdModelAccess[Word]

