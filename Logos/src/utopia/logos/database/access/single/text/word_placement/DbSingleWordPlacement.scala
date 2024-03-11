package utopia.logos.database.access.single.text.word_placement

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.logos.model.stored.text.WordPlacement

/**
  * An access point to individual word placements, based on their id
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class DbSingleWordPlacement(id: Int) 
	extends UniqueWordPlacementAccess with SingleIntIdModelAccess[WordPlacement]

