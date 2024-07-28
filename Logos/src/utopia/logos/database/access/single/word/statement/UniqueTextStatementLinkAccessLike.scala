package utopia.logos.database.access.single.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.logos.model.template.StatementLinkFactory
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
 * Common trait for access points that return individual text statement links at a time
 *
 * @author Mikko Hilpinen
 * @since 14/03/2024, v0.2
 */
trait UniqueTextStatementLinkAccessLike[+A, +Repr]
	extends StatementLinkFactory[Repr] with SingleRowModelAccess[A] with FilterableView[Repr]
		with DistinctModelAccess[A, Option[A], Value] with Indexed
{
	// ABSTRACT -------------------------------
	
	protected def config: StatementLinkDbConfig
	
	
	// COMPUTED -------------------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return Id of the accessible link. None if no item was accessible.
	 */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	 * @param connection Implicit DB connection
	 * @return Id of linked text. None if no item was accessible.
	 */
	def textId(implicit connection: Connection) = pullColumn(config.textIdColumn).int
	/**
	 * @param connection Implicit DB connection
	 * @return Id of the linked statement. None if no item was accessible.
	 */
	def statementId(implicit connection: Connection) = pullColumn(config.statementIdColumn).int
	/**
	 * @param connection Implicit DB connection
	 * @return Index that specifies the linked statement's position within the text. None if no item was accessible.
	 */
	def orderIndex(implicit connection: Connection) = pullColumn(config.orderIndexColumn).int
	
	
	// IMPLEMENTED  ---------------------------
	
	override def withTextId(textId: Int): Repr = filter(config.textIdColumn <=> textId)
	override def withStatementId(statementId: Int): Repr = filter(config.statementIdColumn <=> statementId)
	override def at(orderIndex: Int): Repr = filter(config.orderIndexColumn <=> orderIndex)
}
