package utopia.logos.model.stored.word

import utopia.logos.model.partial.word.TextStatementLinkData

object TextStatementLink
{
	// OTHER    ----------------------
	
	/**
	 * @param id Id to assign to this link
	 * @param data Link data to wrap
	 * @return A new text statement link
	 */
	def apply(id: Int, data: TextStatementLinkData): TextStatementLink = _TextStatementLink(id, data)
	
	
	// NESTED   ----------------------
	
	private case class _TextStatementLink(id: Int, data: TextStatementLinkData) extends TextStatementLink
}

/**
 * A DB-stored model that links statements with the text where it appears
 * @author Mikko Hilpinen
 * @since 14/03/2024, v0.2
 */
trait TextStatementLink
	extends TextStatementLinkLike[TextStatementLinkData, TextStatementLink]
{
	override def withId(id: Int) = TextStatementLink(id, data)
	override protected def buildCopy(textId: Int, statementId: Int, orderIndex: Int) =
		TextStatementLink(id, TextStatementLinkData(textId, statementId, orderIndex))
}
