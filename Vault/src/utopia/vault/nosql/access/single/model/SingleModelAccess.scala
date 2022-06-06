package utopia.vault.nosql.access.single.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.single.SingleAccess
import utopia.vault.nosql.access.template.model.ModelAccess
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, Limit, OrderBy, Select, Where}

/**
 * Used for accessing individual models from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleModelAccess[+A] extends SingleAccess[A] with ModelAccess[A, Option[A], Value]
{
	// IMPLEMENTED  -------------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy], joins: Seq[Joinable],
	                            joinType: JoinType)
	                           (implicit connection: Connection) =
	{
		condition match {
			case Some(condition) => factory.find(condition, order, joins, joinType)
			case None =>
				factory match {
					case rowFactory: FromRowFactory[A] =>
						order match {
							case Some(order) => rowFactory.firstUsing(order)
							case None => rowFactory.any
						}
					case _ => factory.getAll(order).headOption // This is not recommended
				}
		}
	}
	
	override protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                                  order: Option[OrderBy] = None, joins: Seq[Joinable] = Vector(),
	                                  joinType: JoinType = Inner)
	                                 (implicit connection: Connection) =
	{
		// Forms the query first
		val statement = Select(joins.foldLeft(factory.target) { _.join(_, joinType) }, column) +
			mergeCondition(additionalCondition).map { Where(_) } + order.orElse(factory.defaultOrdering) + Limit(1)
		// Applies the query and parses results
		connection(statement).firstValue
	}
}