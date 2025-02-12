package utopia.logos.database.factory.word

import utopia.flow.generic.model.immutable.Model
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.logos.model.partial.word.TextStatementLinkData
import utopia.logos.model.stored.word.TextStatementLink

@deprecated("Replaced with PlacedStatementDbFactory", "v0.3")
object TextStatementLinkDbFactory
{
	// OTHER    ---------------------------
	
	/**
	 * @param config The configuration that determines the targeted table and columns
	 * @return A factory used for reading text statement links from the specified table
	 */
	def apply(config: StatementLinkDbConfig): TextStatementLinkDbFactory = _TextStatementLinkDbFactory(config)
	
	
	// NESTED   ---------------------------
	
	private case class _TextStatementLinkDbFactory(config: StatementLinkDbConfig) extends TextStatementLinkDbFactory
}

/**
 * Used for pulling text statement link data from the DB
 *
 * @author Mikko Hilpinen
 * @since 15/03/2024, v1.0
 */
@deprecated("Replaced with PlacedStatementDbFactory", "v0.3")
trait TextStatementLinkDbFactory extends TextStatementLinkDbFactoryLike[TextStatementLink]
{
	override protected def fromValidatedModel(id: Int, textId: Int, statementId: Int, orderIndex: Int,
	                                          model: Model): TextStatementLink =
		TextStatementLink(id, TextStatementLinkData(textId, statementId, orderIndex))
}