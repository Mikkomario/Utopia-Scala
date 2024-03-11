package utopia.logos.database.access.many.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.FilterableView
import utopia.logos.database.model.text.StatementLinkModel

/**
 * Common trait for access points that return multiple statement links at a time
 * @author Mikko Hilpinen
 * @since 15.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
 * @tparam Sub Type of sub-views used by this access point
 */
trait ManyStatementLinksAccess[+Sub] extends FilterableView[Sub]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Model used for interacting with statement link data
	 */
	protected def model: StatementLinkModel
	
	
	// OTHER    -----------------------
	
	/**
	 * @param statementIds Ids of the targeted statements
	 * @return Access to subject-statement links concerning those statements
	 */
	def withStatements(statementIds: Iterable[Int]) =
		filter(model.statementIdColumn.in(statementIds))
	
	/**
	 * @param statementId Id of the targeted statement
	 * @param position    Targeted position / order index
	 * @return Access to subjects where the specified statement is at the specified location
	 */
	def withStatementAtPosition(statementId: Int, position: Int) =
		filter((model.statementIdColumn <=> statementId) && (model.orderIndexColumn <=> position))
	/**
	 * @param statementId Id of the targeted statement
	 * @return Access to subjects that start with the specified statement
	 */
	def startingWithStatement(statementId: Int) =
		withStatementAtPosition(statementId, 0)
}
