package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.{FromResultFactory, FromRowFactory}
import utopia.vault.nosql.access.SingleModelAccess.FactoryWrapper
import utopia.vault.sql.{Condition, OrderBy}

/**
 * Used for accessing individual models from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleModelAccess[+A] extends SingleAccess[A, SingleModelAccess[A]] with ModelAccess[A, Option[A]]
{
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(implicit connection: Connection) =
	{
		condition match
		{
			case Some(condition) => factory.get(condition, order)
			case None =>
				factory match
				{
					case rowFactory: FromRowFactory[A] => rowFactory.getAny()
					case _ => factory.getAll().headOption // This is not recommended
				}
		}
	}
	
	override def filter(additionalCondition: Condition): SingleModelAccess[A] =
		new FactoryWrapper(factory, Some(mergeCondition(additionalCondition)))
}

object SingleModelAccess
{
	// OTHER	-----------------------
	
	/**
	 * Creates a new single model access
	 * @param factory Wrapped model factory
	 * @tparam A Type of model returned
	 * @return An access point that uses the specified factory
	 */
	def apply[A](factory: FromResultFactory[A]): SingleModelAccess[A] = new FactoryWrapper(factory, None)
	
	/**
	 * Creates a new single model access
	 * @param factory Wrapped model factory
	 * @param condition A search condition
	 * @tparam A Type of model returned
	 * @return An access point that uses the specified factory and search condition
	 */
	def conditional[A](factory: FromResultFactory[A], condition: Condition): SingleModelAccess[A] =
		new FactoryWrapper(factory, Some(condition))
	
	
	// NESTED	-----------------------
	
	private class FactoryWrapper[+A](val factory: FromResultFactory[A], condition: Option[Condition]) extends SingleModelAccess[A]
	{
		override def globalCondition = condition
	}
}