package utopia.vault.nosql.access.many.column

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.access.template.column.IdAccess
import utopia.vault.nosql.factory.FromResultFactory

/**
 * Used for accessing multiple ids at a time
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyIdAccess[+ID] extends IdAccess[ID, Vector[ID]] with ManyColumnAccess[ID]

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
	def wrap[ID](factory: FromResultFactory[_])(valueToId: Value => ID): ManyIdAccess[ID] =
		new FactoryIdAccess[ID](factory, valueToId)
	
	
	// NESTED	--------------------------
	
	private class FactoryIdAccess[+ID](factory: FromResultFactory[_], valToId: Value => ID) extends ManyIdAccess[ID]
	{
		override def target = factory.target
		
		override def parseValue(value: Value) = valToId(value)
		
		override def table = factory.table
		
		override def accessCondition = None
	}
}
