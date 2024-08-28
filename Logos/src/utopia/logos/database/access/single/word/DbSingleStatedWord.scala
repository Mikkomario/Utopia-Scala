package utopia.logos.database.access.single.word

import utopia.logos.model.combined.word.StatedWord
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual stated words, based on their word id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class DbSingleStatedWord(id: Int) extends UniqueStatedWordAccess with SingleIntIdModelAccess[StatedWord]

