package utopia.logos.model.partial.word

import utopia.logos.model.template.{Placed, StatementLinkFactory}

/**
 * Common trait for data models that link a statement to its correct position in a text.
 * @author Mikko Hilpinen
 * @since 14/03/2024, v0.2
 */
trait TextStatementLinkDataLike[+Repr] extends Placed with StatementLinkFactory[Repr]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Id of the text where linked the statement appears
	 */
	def textId: Int
	/**
	 * @return Id of the statement that appears in the linked text
	 */
	def statementId: Int
	
	/**
	 * @param textId New text id to assign (default = current)
	 * @param statementId New statement id to assign (default = current)
	 * @param orderIndex New order index to assign (default = current)
	 * @return Copy of this item with the specified properties
	 */
	protected def buildCopy(textId: Int = textId, statementId: Int = statementId, orderIndex: Int = orderIndex): Repr
	
	
	// IMPLEMENTED  ------------------------
	
	override def withTextId(textId: Int): Repr = buildCopy(textId = textId)
	override def withStatementId(statementId: Int): Repr = buildCopy(statementId = statementId)
	override def at(orderIndex: Int): Repr = buildCopy(orderIndex = orderIndex)
}