package utopia.logos.database.reader.url

import utopia.logos.model.combined.url.{DetailedLink, DetailedRequestPath}
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.nosql.read.linked.CombiningDbRowReader

/**
 * An interface for reading detailed link data
 *
 * @author Mikko Hilpinen
 * @since 11.07.2025, v0.6
 */
object DetailedLinkDbReader
	extends CombiningDbRowReader[StoredLink, DetailedRequestPath, DetailedLink](LinkDbReader, DetailedRequestPathDbReader)
{
	override protected def combine(left: StoredLink, right: DetailedRequestPath): DetailedLink =
		DetailedLink(left, right)
}
