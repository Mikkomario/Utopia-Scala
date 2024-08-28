package utopia.logos.model.stored.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.access.single.text.word.placement.DbSingleWordPlacement
import utopia.logos.model.factory.text.WordPlacementFactoryWrapper
import utopia.logos.model.partial.text.{TextPlacementData, WordPlacementData}
import utopia.vault.model.template.StoredFromModelFactory

object WordPlacement extends StoredFromModelFactory[WordPlacementData, WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = WordPlacementData
	
	override protected def complete(model: AnyModel, data: WordPlacementData) = 
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a word placement that has already been stored in the database
  * @param id id of this word placement in the database
  * @param data Wrapped word placement data
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class WordPlacement(id: Int, data: WordPlacementData) 
	extends WordPlacementFactoryWrapper[WordPlacementData, WordPlacement] with TextPlacementData 
		with StoredTextPlacementLike[WordPlacementData, WordPlacement]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this word placement in the database
	  */
	def access = DbSingleWordPlacement(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: WordPlacementData) = copy(data = data)
}

