package utopia.scribe.core.model.combined.management

import utopia.scribe.core.model.stored.management.Resolution

object ResolutionWithText
{
	// OTHER    ------------------------------
	
	/**
	 * @param resolution Resolution to wrap
	 * @param text Text to attach
	 * @return A resolution with the specified text included
	 */
	def apply(resolution: Resolution, text: String): ResolutionWithText =
		_ResolutionWithText(resolution, text)
	
	
	// NESTED   ------------------------------
	
	private case class _ResolutionWithText(resolution: Resolution, text: String) extends ResolutionWithText
	{
		override protected def wrap(factory: Resolution): ResolutionWithText = copy(resolution = factory)
	}
}

/**
 * Includes comment text to a Resolution instance
 *
 * @author Mikko Hilpinen
 * @since 26.08.2025, v1.2
 */
trait ResolutionWithText extends CombinedResolution[ResolutionWithText]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return The comment written about this resolution. May be empty.
	 */
	def text: String
}