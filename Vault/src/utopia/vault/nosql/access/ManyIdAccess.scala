package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Condition, OrderBy, Select, Where}

/**
 * Used for accessing multiple ids at a time
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyIdAccess[+ID] extends IdAccess[ID, Vector[ID]] with ManyAccess[ID, ManyIdAccess[ID]]
{
	// COMPUTED ---------------------------
	
	/**
	 * @param connection Implicit database connection
	 * @return An iterator that returns all ids accessible from this access point. The iterator is usable
	 *         only while the connection is kept open.
	 */
	def iterator(implicit connection: Connection) =
		connection.iterator(Select.index(target, table) + globalCondition.map { Where(_) })
			.flatMap { _.rowValues.flatMap(valueToId) }
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(implicit connection: Connection) =
	{
		val statement = Select.index(target, table) + condition.map { Where(_) } + order
		connection(statement).rowValues.flatMap(valueToId).distinct
	}
	
	override def filter(additionalCondition: Condition): ManyIdAccess[ID] = new Filtered(additionalCondition)
	
	
	// NESTED	---------------------------
	
	private class Filtered(condition: Condition) extends ManyIdAccess[ID]
	{
		override def target = ManyIdAccess.this.target
		
		override def valueToId(value: Value) = ManyIdAccess.this.valueToId(value)
		
		override def table = ManyIdAccess.this.table
		
		override def globalCondition = Some(ManyIdAccess.this.mergeCondition(condition))
	}
}

object ManyIdAccess
{
	// OTHER	--------------------------
	
	/**
	  * Wraps a factory into an id access point
	  * @param factory Factory to wrap
	  * @param valueToId Function for converting values to ids
	  * @tparam ID Target id type
	  * @return An access point to ids accessible from that factory
	  */
	def wrap[ID](factory: FromResultFactory[_])(valueToId: Value => Option[ID]): ManyIdAccess[ID] =
		new FactoryIdAccess[ID](factory, valueToId)
	
	
	// NESTED	--------------------------
	
	private class FactoryIdAccess[+ID](factory: FromResultFactory[_], valToId: Value => Option[ID]) extends ManyIdAccess[ID]
	{
		override def target = factory.target
		
		override def valueToId(value: Value) = valToId(value)
		
		override def table = factory.table
		
		override def globalCondition = None
	}
}
