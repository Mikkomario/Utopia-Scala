package utopia.logos.model.combined.url

import utopia.flow.view.template.Extender
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement
import utopia.logos.model.template.Placed

/**
 * Attaches detailed link information to a link-placement entry
 * @author Mikko Hilpinen
 * @since 17.10.2023, Emissary Email Client v0.1, added to Logos v0.2 11.3.2024
 */
case class DetailedLinkPlacement(placement: LinkPlacement, link: DetailedLink)
	extends Extender[LinkPlacementData] with Placed
{
	// COMPUTED ------------------------
	
	/**
	 * @return Id of this link placement
	 */
	def id = placement.id
	
	
	// IMPLEMENTED  --------------------
	
	override def wrapped: LinkPlacementData = placement.data
	
	override def orderIndex: Int = wrapped.orderIndex
	
	override def toString = link.toString
}
