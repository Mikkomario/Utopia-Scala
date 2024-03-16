package utopia.logos.model.partial.text

object TextStatementLinkData
{
	// OTHER    ---------------------
	
	/**
	 * @param textId Id of the text where the linked statement appears
	 * @param statementId Id of the appearing statement
	 * @param orderIndex 0-based index that determines the location at which the statement appears
	 * @return A new text statement link data instance
	 */
	def apply(textId: Int, statementId: Int, orderIndex: Int): TextStatementLinkData =
		_TextStatementLinkData(textId, statementId, orderIndex)
	
	
	// NESTED   ---------------------
	
	private case class _TextStatementLinkData(textId: Int, statementId: Int, orderIndex: Int)
		extends TextStatementLinkData
}

/**
 * A data model that links a statement to its correct position in a text where it appears
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
trait TextStatementLinkData extends TextStatementLinkDataLike[TextStatementLinkData]
{
	override protected def buildCopy(textId: Int, statementId: Int, orderIndex: Int) =
		TextStatementLinkData(textId, statementId, orderIndex)
}