package utopia.vault.nosql.access.single.column

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.template.column.IdAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual ids in a table
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait SingleIdAccess[+ID]
	extends IdAccess[ID, Option[ID]] with SingleColumnAccess[ID] with FilterableView[SingleIdAccess[ID]]
{
	// IMPLEMENTED	---------------------------
	
	override def filter(additionalCondition: Condition): SingleIdAccess[ID] = new Filtered(additionalCondition)
	
	
	// NESTED	------------------------------
	
	private class Filtered(condition: Condition) extends SingleIdAccess[ID]
	{
		override def target = SingleIdAccess.this.target
		
		override def valueToId(value: Value) = SingleIdAccess.this.valueToId(value)
		
		override def table = SingleIdAccess.this.table
		
		override def globalCondition = Some(SingleIdAccess.this.mergeCondition(condition))
	}
}
