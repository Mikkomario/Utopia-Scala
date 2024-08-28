package utopia.logos.model.combined.text

import utopia.flow.view.template.Extender
import utopia.logos.model.partial.text.TextPlacementDataLike
import utopia.logos.model.stored.text.StoredTextPlacementLike
import utopia.logos.model.template.Placed
import utopia.vault.model.template.{HasId, Stored}

/**
  * Common trait for combinations that combine some text with its placement
  * @tparam Repr Type of this combination
  * @tparam Text Type of the placed text instance
  * @tparam Placement Type of the placement used
  * @tparam PlacementData Type of data portion used in the linked text placement instance
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait PlacedTextLike[+Repr, +Text <: Stored[TextData, Int], +TextData,
	+Placement <: StoredTextPlacementLike[PlacementData, Placement],
	PlacementData <: TextPlacementDataLike[PlacementData]]
	extends Extender[TextData] with HasId[Int] with Placed
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Placed text instance
	  */
	def placedText: Text
	/**
	  * @return Placement instance, linking this text to the text in which this one appears
	  */
	def placement: Placement
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Id of the text in which this text appears
	  */
	def parentId = placement.parentId
	
	
	// IMPLEMENTED  ---------------------
	
	override def id: Int = placedText.id
	override def orderIndex: Int = placement.orderIndex
	
	override def wrapped: TextData = placedText.data
}
