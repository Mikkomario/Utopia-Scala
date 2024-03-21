package utopia.logos.database.access.many.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.FilterableView
import utopia.logos.database.storable.word.StatementLinkedModel

/**
 * Common trait for access points that target items that may be placed within statements
 * @author Mikko Hilpinen
 * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
 */
trait ManyStatementPlacedAccess[+Sub] extends FilterableView[Sub]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Model used for interacting with statement-linked items
	 */
	protected def model: StatementLinkedModel
	
	
	// OTHER    --------------------------
	
	/**
	 * @param index Targeted position index
	 * @return Access to items at that position
	 */
	def atPosition(index: Int) = filter(model.orderIndexColumn <=> index)
	
	/**
	 * @param statementIds Ids of tha targeted statements
	 * @return Access to items placed within the specified statements
	 */
	def inStatements(statementIds: Iterable[Int]) = filter(model.statementIdColumn.in(statementIds))
}
