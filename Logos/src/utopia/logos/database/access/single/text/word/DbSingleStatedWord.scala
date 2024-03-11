package utopia.logos.database.access.single.text.word

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.logos.model.combined.text.StatedWord

/**
  * An access point to individual stated words, based on their word id
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class DbSingleStatedWord(id: Int) extends UniqueStatedWordAccess with SingleIntIdModelAccess[StatedWord]

