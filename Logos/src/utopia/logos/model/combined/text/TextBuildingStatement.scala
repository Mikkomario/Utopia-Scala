package utopia.logos.model.combined.text

import utopia.logos.model.stored.text.StoredStatement

/**
 * Attaches word text information to a statement.
 * Used for building full text statements.
 * @author Mikko Hilpinen
 * @since 10.07.2025, v0.6
 */
case class TextBuildingStatement(statement: StoredStatement, indexedWords: Seq[(Int, String)])
	extends CombinedStatement[TextBuildingStatement]
{
	// COMPUTED   -------------------------
	
	/**
	 * @return The word-based text content of this statement.
	 *         Doesn't contain any links, nor the delimiter.
	 */
	def wordsText = indexedWords.iterator.map { _._2 }.mkString(" ")
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = wordsText
	
	override protected def wrap(factory: StoredStatement): TextBuildingStatement = copy(statement = factory)
}