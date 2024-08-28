package utopia.logos.database.access.single.text.word

import utopia.logos.model.combined.text.StatedWord
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual stated words, based on their word id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleStatedWord(id: Int) extends UniqueStatedWordAccess with SingleIntIdModelAccess[StatedWord]

