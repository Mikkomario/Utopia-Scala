package utopia.logos.database.access.many.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.template.PlacedAccessLike
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
 * Common trait for access points that target multiple text statement links or similar instances at a time
 * @tparam A Type of read items
 * @tparam Repr Type of the implementing access point
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
trait ManyTextStatementLinksAccessLike[+A, +Repr]
	extends ManyModelAccess[A] with FilterableView[Repr] with Indexed with PlacedAccessLike[Repr]
{
	// ABSTRACT -----------------------
	
	protected def config: StatementLinkDbConfig
	
	
	// COMPUTED -----------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return Ids of the accessible items
	 */
	def ids(implicit connection: Connection) = pullColumn(index).map { _.getInt }
	
	/**
	 * @param connection Implicit DB connection
	 * @return Ids of the linked texts
	 */
	def textIds(implicit connection: Connection) = pullColumn(config.textIdColumn).map { _.getInt }
	/**
	 * @param connection Implicit DB connection
	 * @return Ids of the linked statements
	 */
	def statementIds(implicit connection: Connection) = pullColumn(config.statementIdColumn).map { _.getInt }
	/**
	 * @param connection Implicit DB connection
	 * @return Statement order/position indices
	 */
	def orderIndices(implicit connection: Connection) = pullColumn(config.orderIndexColumn).map { _.getInt }
	
	
	// IMPLEMENTED  -------------------
	
	override protected def orderIndexColumn: Column = config.orderIndexColumn
	
	
	// OTHER    -----------------------
	
	/**
	 * @param textId Id of the targeted text
	 * @return Access to links to that text
	 */
	def inText(textId: Int) = filter(config.textIdColumn <=> textId)
	/**
	 * @param textIds Ids of the targeted texts
	 * @return Access to links to those texts
	 */
	def inTexts(textIds: Iterable[Int]) = filter(config.textIdColumn.in(textIds))
	
	/**
	 * @param statementId Id of the targeted statement
	 * @return Access to links to that statement
	 */
	def toStatement(statementId: Int) = filter(config.statementIdColumn <=> statementId)
	/**
	 * @param statementIds Ids of the targeted statements
	 * @return Access to links to those statements
	 */
	def toStatements(statementIds: Iterable[Int]) = filter(config.statementIdColumn.in(statementIds))
}
