package utopia.logos.model.combined.text

import utopia.logos.model.stored.text.{Statement, TextStatementLink}

object LinkedStatement
{
	// OTHER    ------------------------------
	
	/**
	 * @param statement The statement to wrap
	 * @param link Link to the text where the statement appears
	 * @return A new linked statement
	 */
	def apply(statement: Statement, link: TextStatementLink): LinkedStatement = _LinkedStatement(statement, link)
	
	
	// NESTED   ------------------------------
	
	private case class _LinkedStatement(statement: Statement, link: TextStatementLink) extends LinkedStatement
}

/**
 * Attaches a text link to a statement
 * @author Mikko Hilpinen
 * @since 14/03/2024, v0.2
 */
trait LinkedStatement extends LinkedStatementLike[TextStatementLink]
