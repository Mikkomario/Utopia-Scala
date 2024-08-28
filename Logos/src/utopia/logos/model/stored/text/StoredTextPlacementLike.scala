package utopia.logos.model.stored.text

import utopia.logos.model.factory.text.TextPlacementFactoryWrapper
import utopia.logos.model.partial.text.TextPlacementDataLike
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.{FromIdFactory, Stored}

/**
  * Common trait for text placements which have been stored in the database
  * @tparam Data Type of the wrapped data
  * @tparam Repr Implementing type
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait StoredTextPlacementLike[Data <: TextPlacementDataLike[Data], +Repr] 
	extends Stored[Data, Int] with FromIdFactory[Int, Repr] with TextPlacementFactoryWrapper[Data, Repr] 
		with TextPlacementDataLike[Repr] with StoredPlaced[Data, Int]
{
	// IMPLEMENTED	--------------------
	
	override def parentId = data.parentId
	override def placedId = data.placedId
	
	override protected def wrappedFactory = data
	
	override def copyTextPlacement(parentId: Int, placedId: Int, orderIndex: Int) = 
		wrap(data.copyTextPlacement(parentId, placedId, orderIndex))
}

