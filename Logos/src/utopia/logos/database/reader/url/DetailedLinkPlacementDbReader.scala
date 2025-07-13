package utopia.logos.database.reader.url

import utopia.logos.model.combined.url.{DetailedLink, DetailedLinkPlacement}
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.read.linked.CombiningDbRowReader

/**
 * Used for pulling link placement data, including full link information
 *
 * @author Mikko Hilpinen
 * @since 13.07.2025, v0.6
 */
object DetailedLinkPlacementDbReader
	extends CombiningDbRowReader[LinkPlacement, DetailedLink, DetailedLinkPlacement](LinkPlacementDbReader, DetailedLinkDbReader)
{
	override protected def combine(left: LinkPlacement, right: DetailedLink): DetailedLinkPlacement =
		DetailedLinkPlacement(left, right)
}
