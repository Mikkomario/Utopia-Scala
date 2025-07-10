package utopia.logos.model.combined.text

import utopia.logos.model.stored.text.StoredStatement

/**
 * Attaches word text information to a statement.
 * Used for building full text statements.
 * @author Mikko Hilpinen
 * @since 10.07.2025, v0.6
 */
case class TextBuildingStatement(statement: StoredStatement, wordsText: String)
	extends CombinedStatement[TextBuildingStatement]
{
	override protected def wrap(factory: StoredStatement): TextBuildingStatement = copy(statement = factory)
}