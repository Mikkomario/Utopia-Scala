package utopia.logos.model.stored.word

import utopia.logos.database.access.single.word.placement.DbSingleWordPlacement
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.factory.word.WordPlacementFactory
import utopia.logos.model.partial.word.WordPlacementData
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

/**
  * Represents a word placement that has already been stored in the database
  * @param id id of this word placement in the database
  * @param data Wrapped word placement data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class WordPlacement(id: Int, data: WordPlacementData) 
	extends StoredModelConvertible[WordPlacementData] with WordPlacementFactory[WordPlacement] 
		with FromIdFactory[Int, WordPlacement] with StoredPlaced[WordPlacementData, Int]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this word placement in the database
	  */
	def access = DbSingleWordPlacement(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withId(id: Int) = copy(id = id)
	
	override def withOrderIndex(orderIndex: Int) = copy(data = data.withOrderIndex(orderIndex))
	
	override def withStatementId(statementId: Int) = copy(data = data.withStatementId(statementId))
	
	override def withStyle(style: DisplayStyle) = copy(data = data.withStyle(style))
	
	override def withWordId(wordId: Int) = copy(data = data.withWordId(wordId))
}

