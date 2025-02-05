package utopia.logos.database.access.many.text.statement

import utopia.logos.model.combined.text.{DetailedPlacedStatement, PlacedStatement}
import utopia.vault.database.Connection

/**
 * Common trait for access points which yield generic "placed" statements
 * @tparam Repr Type of the implementing access point
 * @author Mikko Hilpinen
 * @since 05.02.2025, v0.1
 */
trait ManyPlacedStatementsAccess[+Repr]
	extends ManyPlacedStatementsAccessLike[PlacedStatement, Repr]
{
	// COMPUTED --------------------------
	
	/**
	 * Pulls all accessible statements, including their textual contents
	 * @param connection Implicit connection
	 * @return Accessible placed statements, including textual details
	 */
	def pullDetailed(implicit connection: Connection) = {
		val statements = pull
		val detailedMap = DbStatements.attachDetailsTo(statements.map { _.statement }).view.map { s => s.id -> s }.toMap
		statements.map { s => DetailedPlacedStatement(detailedMap(s.id), s.placement) }
	}
}
