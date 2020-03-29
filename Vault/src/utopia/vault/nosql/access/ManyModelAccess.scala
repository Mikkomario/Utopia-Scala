package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.access.ManyModelAccess.FactoryWrapper
import utopia.vault.sql.{Condition, OrderBy}

/**
 * Used for accessing multiple models at a time from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyModelAccess[+A] extends ManyAccess[A, ManyModelAccess[A]] with ModelAccess[A, Vector[A]]
{
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(implicit connection: Connection) = condition match
	{
		case Some(condition) => factory.getMany(condition, order)
		case None => factory.getAll(order)
	}
	
	override def filter(additionalCondition: Condition): ManyModelAccess[A] = new FactoryWrapper(factory,
		Some(mergeCondition(additionalCondition)))
}

object ManyModelAccess
{
	// OTHER	--------------------------
	
	/**
	 * Wraps a model factory
	 * @param factory A model factory
	 * @tparam A Type of model read from DB
	 * @return An access point
	 */
	def apply[A](factory: FromResultFactory[A]): ManyModelAccess[A] = new FactoryWrapper(factory, None)
	
	/**
	 * Wraps a model factory
	 * @param factory A model factory
	 * @param condition A search condition used
	 * @tparam A Type of model read from DB
	 * @return An access point
	 */
	def conditional[A](factory: FromResultFactory[A], condition: Condition): ManyModelAccess[A] =
		new FactoryWrapper(factory, Some(condition))
	
	
	// NESTED	--------------------------
	
	private class FactoryWrapper[A](val factory: FromResultFactory[A], condition: Option[Condition]) extends ManyModelAccess[A]
	{
		override def globalCondition = condition
	}
}