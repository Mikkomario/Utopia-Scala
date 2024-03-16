package utopia.logos.database.access.many.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.template.PlacedAccessLike
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

/**
 * Common trait for access points which include both statement and text link information
 *
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
// WET WET (from ManyStatementLinksAccessLike)
trait ManyLinkedStatementsAccessLike[+A, +Repr] extends ManyStatementsAccessLike[A, Repr] with PlacedAccessLike[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Configuration used for interacting with text links
	 */
	protected def linkConfig: StatementLinkDbConfig
	
	
	// COMPUTED -----------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return Ids of the linked texts
	 */
	def textIds(implicit connection: Connection) =
		pullColumn(linkConfig.textIdColumn).map { _.getInt }
	/**
	 * @param connection Implicit DB connection
	 * @return Statement order/position indices
	 */
	def orderIndices(implicit connection: Connection) =
		pullColumn(linkConfig.orderIndexColumn).map { _.getInt }
	
	
	// IMPLEMENTED  -------------------
	
	override protected def orderIndexColumn: Column = linkConfig.orderIndexColumn
	
	
	// OTHER    -----------------------
	
	/**
	 * @param textId Id of the targeted text
	 * @return Access to statements within that text
	 */
	def inText(textId: Int) = filter(linkConfig.textIdColumn <=> textId)
	/**
	 * @param textIds Ids of the targeted texts
	 * @return Access to statements within those texts
	 */
	def inTexts(textIds: Iterable[Int]) = filter(linkConfig.textIdColumn.in(textIds))
}
