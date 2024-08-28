package utopia.logos.model.combined.text

import utopia.flow.view.template.Extender
import utopia.logos.model.factory.text.TextPlacementFactoryWrapper
import utopia.logos.model.partial.text.TextPlacementDataLike
import utopia.logos.model.stored.text.StoredTextPlacementLike
import utopia.vault.model.template.HasId

/**
  * Common trait for combinations that add information to text placements
  * @tparam Repr Type of this combination
  * @tparam Stored Type of stored text placement used
  * @tparam Data Type of text placement data used
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait CombinedTextPlacement[+Repr, Stored <: StoredTextPlacementLike[Data, Stored], Data <: TextPlacementDataLike[Data]]
	extends Extender[Data] with HasId[Int] with TextPlacementFactoryWrapper[Stored, Repr]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Wrapped text placement
	  */
	def placement: Stored
	
	
	// IMPLEMENTED  -----------------
	
	override def id: Int = placement.id
	
	override def wrapped: Data = placement.data
	override protected def wrappedFactory: Stored = placement
}