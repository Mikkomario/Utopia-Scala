package utopia.logos.model.stored.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.model.partial.text.TextPlacementData
import utopia.vault.model.template.StoredFromModelFactory

object TextPlacement extends StoredFromModelFactory[TextPlacementData, TextPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = TextPlacementData
	
	override protected def complete(model: AnyModel, data: TextPlacementData) = 
		model("id").tryInt.map { apply(_, data) }
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new text placement
	  * @param id id of this text placement in the database
	  * @param data Wrapped text placement data
	  * @return text placement with the specified id and wrapped data
	  */
	def apply(id: Int, data: TextPlacementData): TextPlacement = _TextPlacement(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the text placement trait
	  * @param id id of this text placement in the database
	  * @param data Wrapped text placement data
	  * @author Mikko Hilpinen
	  * @since 27.08.2024
	  */
	private case class _TextPlacement(id: Int, data: TextPlacementData) extends TextPlacement
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: TextPlacementData) = copy(data = data)
	}
}

/**
  * Represents a text placement that has already been stored in the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacement extends StoredTextPlacementLike[TextPlacementData, TextPlacement] with TextPlacementData

