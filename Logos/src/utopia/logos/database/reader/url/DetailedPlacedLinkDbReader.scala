package utopia.logos.database.reader.url

import utopia.logos.model.combined.url.{DetailedLink, DetailedPlacedLink}
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.read.linked.CombiningDbRowReader

/**
 * Combines detailed link information, as well as link placement information
 *
 * @author Mikko Hilpinen
 * @since 13.07.2025, v0.6
 */
object DetailedPlacedLinkDbReader
	extends CombiningDbRowReader[DetailedLink, LinkPlacement, DetailedPlacedLink](DetailedLinkDbReader, LinkPlacementDbReader)
{
	override protected def combine(left: DetailedLink, right: LinkPlacement): DetailedPlacedLink =
		DetailedPlacedLink(left, right)
}
