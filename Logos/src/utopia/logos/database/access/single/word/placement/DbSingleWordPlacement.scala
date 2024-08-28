package utopia.logos.database.access.single.word.placement

import utopia.logos.model.stored.word.WordPlacement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual word placements, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class DbSingleWordPlacement(id: Int) 
	extends UniqueWordPlacementAccess with SingleIntIdModelAccess[WordPlacement]

