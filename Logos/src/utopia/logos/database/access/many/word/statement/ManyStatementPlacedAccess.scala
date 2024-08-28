package utopia.logos.database.access.many.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.word.StatementLinkedModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points that target items that may be placed within statements
  * @author Mikko Hilpinen
  * @since 31.07.2024
  */
@deprecated("Deprecated for removal", "v0.3")
trait ManyStatementPlacedAccess[+Sub] extends FilterableView[Sub]
{
	// ABSTRACT	--------------------
	
	/**
	  * Model used for interacting with statement-linked items
	  */
	protected def model: StatementLinkedModel
	
	
	// OTHER	--------------------
	
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

