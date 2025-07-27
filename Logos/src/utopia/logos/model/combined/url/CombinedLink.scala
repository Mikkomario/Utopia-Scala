package utopia.logos.model.combined.url

import utopia.flow.view.template.Extender
import utopia.logos.model.factory.url.LinkFactoryWrapper
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.store.HasId

/**
 * Common trait for classes which attach data to stored links
 *
 * @author Mikko Hilpinen
 * @since 13.07.2025, v0.6
 */
trait CombinedLink[+Repr] extends Extender[LinkData] with HasId[Int] with LinkFactoryWrapper[StoredLink, Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The wrapped stored link instance
	 */
	def stored: StoredLink
	
	
	// IMPLEMENTED  --------------------
	
	override def id: Int = stored.id
	
	override def wrapped: LinkData = stored.data
	override protected def wrappedFactory: StoredLink = stored
	
	
	// OTHER    -------------------------
	
	def withStored(link: StoredLink) = wrap(link)
}
