package utopia.logos.model.combined.word

import utopia.logos.model.partial.word.StatementData
import utopia.logos.model.stored.word.{Statement, TextStatementLink}
import utopia.logos.model.template.Placed
import utopia.vault.model.template.Stored

/**
 * Common trait for combined models that join a statement with a text link
 * @tparam Link Type of the text link used
 * @author Mikko Hilpinen
 * @since 15/03/2024, v0.2
 */
trait LinkedStatementLike[+Link <: TextStatementLink] extends Stored[StatementData, Int] with Placed
{
	// ABSTRACT -------------------------
	
	/**
	 * @return The wrapped statement
	 */
	def statement: Statement
	/**
	 * @return Link to the text where this statement appears
	 */
	def link: Link
	
	
	// IMPLEMENTED  ---------------------
	
	override def id: Int = statement.id
	override def data: StatementData = statement.data
	override def orderIndex: Int = link.orderIndex
}
