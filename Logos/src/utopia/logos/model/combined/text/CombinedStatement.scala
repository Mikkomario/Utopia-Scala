package utopia.logos.model.combined.text

import utopia.flow.view.template.Extender
import utopia.logos.model.factory.text.StatementFactoryWrapper
import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.model.template.HasId

/**
 * Common trait for combinations where the primary part is a (stored) statement
 * @tparam Repr Implementing (concrete) type
 * @author Mikko Hilpinen
 * @since 17.12.2024, v0.3.1
 */
trait CombinedStatement[+Repr]
	extends Extender[StatementData] with HasId[Int] with StatementFactoryWrapper[StoredStatement, Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The wrapped statement
	 */
	def statement: StoredStatement
	
	
	// IMPLEMENTED  --------------------
	
	override def id: Int = statement.id
	
	override def wrapped: StatementData = statement.data
	override protected def wrappedFactory: StoredStatement = statement
}
