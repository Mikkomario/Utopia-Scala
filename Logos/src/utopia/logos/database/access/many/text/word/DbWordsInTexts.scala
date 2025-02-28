package utopia.logos.database.access.many.text.word

import utopia.logos.database.factory.text.WordInTextDbFactory
import utopia.logos.database.props.text.StatementPlacementDbProps

/**
 * An interface for accessing placed word data
 *
 * @author Mikko Hilpinen
 * @since 28.02.2025, v0.5
 */
object DbWordsInTexts
{
	/**
	 * @param linkModel Model used for interacting with the text links
	 * @return Access to texts linked using the specified model
	 */
	def apply(linkModel: StatementPlacementDbProps) = ManyWordsInTextsAccess(linkModel, None)
	/**
	 * @param factory A factory used for reading linked word data
	 * @return Access to words using the specified factory
	 */
	def apply(factory: WordInTextDbFactory) = ManyWordsInTextsAccess(factory, None)
}