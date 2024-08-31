package utopia.logos.model.combined.word

import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.StoredStatement
import utopia.logos.model.stored.word.TextStatementLink
import utopia.logos.model.template.Placed
import utopia.vault.model.template.Stored

/**
 * Common trait for combined models that join a statement with a text link
 * @tparam Link Type of the text link used
 * @author Mikko Hilpinen
 * @since 15/03/2024, v0.2
 */
@deprecated("Replaced with PlacedStatementLike", "v0.3")
trait LinkedStatementLike[+Link <: TextStatementLink] extends Stored[StatementData, Int] with Placed
{
	// ABSTRACT -------------------------
	
	/**
	 * @return The wrapped statement
	 */
	def statement: StoredStatement
	/**
	 * @return Link to the text where this statement appears
	 */
	def link: Link
	
	
	// IMPLEMENTED  ---------------------
	
	override def id: Int = statement.id
	override def data: StatementData = statement.data
	override def orderIndex: Int = link.orderIndex
}
