package utopia.logos.model.stored.text

import utopia.logos.database.access.single.text.word_placement.DbSingleWordPlacement
import utopia.logos.model.partial.text.WordPlacementData
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a word placement that has already been stored in the database
  * @param id id of this word placement in the database
  * @param data Wrapped word placement data
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class WordPlacement(id: Int, data: WordPlacementData)
	extends StoredModelConvertible[WordPlacementData] with StoredPlaced[WordPlacementData, Int]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this word placement in the database
	  */
	def access = DbSingleWordPlacement(id)
}

