package utopia.vault.model.immutable

import utopia.flow.datastructure.template.Model
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.{Model, Property}

/**
  * Represents an event caused by an update targeting a table
  * @author Mikko Hilpinen
  * @since 25.11.2021, v1.12
  */
sealed trait TableUpdateEvent
{
	/**
	  * @return Name of the table affected by this update
	  */
	def tableName: String
}

object TableUpdateEvent
{
	/**
	  * An event generated when new data is inserted to a table
	  * @param table Table where new data was inserted to
	  * @param insertedModels Models that were inserted to the table (property names as keys)
	  * @param generatedKeys Row ids that were generated for the new data (if table uses auto-increment indexing)
	  */
	case class DataInserted(table: Table, insertedModels: Seq[Model[Property]], generatedKeys: Vector[Value])
		extends TableUpdateEvent
	{
		override def tableName = table.name
	}
	/**
	  * An event generated when some table rows are updated
	  * @param tableName Name of the table where data may have been modified
	  * @param updatedRowCount Number of targeted rows
	  */
	case class RowsUpdated(tableName: String, updatedRowCount: Int) extends TableUpdateEvent
	/**
	  * An event generated when some data is or may have been deleted from a table
	  * @param table Table that may have been affected
	  */
	case class DataDeleted(table: Table) extends TableUpdateEvent
	{
		override def tableName = table.name
	}
}
