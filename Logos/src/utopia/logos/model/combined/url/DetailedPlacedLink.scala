package utopia.logos.model.combined.url

import utopia.logos.model.stored.url.{LinkPlacement, StoredLink}
import utopia.logos.model.template.{Placed, PlacedFactory}

/**
 * Represents a link placed in a specific position in a statement.
 * Includes request path and domain information.
 *
 * @author Mikko Hilpinen
 * @since 13.07.2025, v0.6
 */
case class DetailedPlacedLink(link: DetailedLink, placement: LinkPlacement)
	extends DetailedLink with CombinedLink[DetailedPlacedLink] with Placed with PlacedFactory[DetailedPlacedLink]
{
	override def stored: StoredLink = link.stored
	override def path: DetailedRequestPath = link.path
	override def orderIndex: Int = placement.orderIndex
	
	override def at(orderIndex: Int): DetailedPlacedLink = copy(placement = placement.at(orderIndex))
	
	override protected def wrap(factory: StoredLink): DetailedPlacedLink = copy(link = link.withStored(factory))
}