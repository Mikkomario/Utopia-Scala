package utopia.logos.model.combined.text

import utopia.flow.operator.ordering.CombinedOrdering
import utopia.logos.model.enumeration.DisplayStyle

object WordInText
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * An ordering used for placing these words in the correct order within a text instance
	 */
	implicit lazy val ordering: Ordering[WordInText] = CombinedOrdering(
		Ordering.by { _.indexOfStatement }, Ordering.by { _.indexInStatement })
}

/**
 * Represents a word that appears within a statement that appears within some text.
 *
 * @param id Id of this word
 * @param standardizedText Standardized text representation of this word
 * @param style Style in which this word appears in this context
 * @param statementId Id of the statement in which this word appears
 * @param indexInStatement 0-based index of this word within its statement
 * @param textId Id of the text in which this word (and statement) appears
 * @param indexOfStatement 0-based index of the statement within the parent text
 * @author Mikko Hilpinen
 * @since 28.02.2025, v0.5
 */
case class WordInText(id: Int, standardizedText: String, style: DisplayStyle,
                      statementId: Int, indexInStatement: Int, textId: Int, indexOfStatement: Int)
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * Text format of this word in this context
	 */
	lazy val text = style(standardizedText)
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = text
}