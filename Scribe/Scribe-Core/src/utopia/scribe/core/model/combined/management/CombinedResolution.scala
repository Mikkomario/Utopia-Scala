package utopia.scribe.core.model.combined.management

import utopia.flow.view.template.Extender
import utopia.scribe.core.model.factory.management.ResolutionFactoryWrapper
import utopia.scribe.core.model.partial.management.ResolutionData
import utopia.scribe.core.model.stored.management.Resolution
import utopia.vault.store.HasId

/**
 * Common trait for models which attach other data to Resolutions
 *
 * @author Mikko Hilpinen
 * @since 26.08.2025, v1.2
 */
trait CombinedResolution[+Repr]
	extends Extender[ResolutionData] with HasId[Int] with ResolutionFactoryWrapper[Resolution, Repr]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return The wrapped resolution instance
	 */
	def resolution: Resolution
	
	
	// IMPLEMENTED  -------------------------
	
	override def id: Int = resolution.id
	
	override def wrapped: ResolutionData = resolution.data
	override protected def wrappedFactory: Resolution = resolution
}
