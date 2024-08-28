package utopia.logos.database.access.single.text.word.placement

import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual word placements, based on their id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleWordPlacement(id: Int) 
	extends UniqueWordPlacementAccess with SingleIntIdModelAccess[WordPlacement]

