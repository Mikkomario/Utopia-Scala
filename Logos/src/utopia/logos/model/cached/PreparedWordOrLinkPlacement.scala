package utopia.logos.model.cached

import utopia.logos.model.enumeration.DisplayStyle

object PreparedWordOrLinkPlacement
{
	/**
	 * @param id A link id
	 * @return Prepared link placement
	 */
	def link(id: Int) = apply(id, isLink = true)
}

/**
 * Represents an individual placement of a word or a link.
 * Used for representing this data before it is connected to a statement.
 *
 * @author Mikko Hilpinen
 * @since 31.12.2024, v0.4
 *
 * @param id Id of this word or link
 * @param style Style in which this word appears in this context
 * @param isLink Whether this represents a link
 */
case class PreparedWordOrLinkPlacement(id: Int, style: DisplayStyle = DisplayStyle.default, isLink: Boolean = false)
