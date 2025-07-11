package utopia.logos.database.reader.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.logos.model.partial.text.StatementPlacementData
import utopia.logos.model.stored.text.StatementPlacement
import utopia.vault.model.immutable.Table
import utopia.vault.sql.OrderBy

object StatementPlacementDbReader
{
	// OTHER	--------------------
	
	/**
	  * @param table   Table from which data is read
	  * @param dbProps Database properties used when reading column data
	  * @return A factory used for parsing statement placements from database model data
	  */
	def apply(table: Table, dbProps: StatementPlacementDbProps): StatementPlacementDbReader = 
		_StatementPlacementDbReader(table, dbProps)
	
	
	// NESTED	--------------------
	
	/**
	  * @param table   Table from which data is read
	  * @param dbProps Database properties used when reading column data
	  */
	private case class _StatementPlacementDbReader(table: Table, dbProps: StatementPlacementDbProps) 
		extends StatementPlacementDbReader
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param model       Model from which additional data may be read
		  * @param id          Id to assign to the read/parsed statement placement
		  * @param parentId    parent id to assign to the new statement placement
		  * @param statementId statement id to assign to the new statement placement
		  * @param orderIndex  order index to assign to the new statement placement
		  */
		override protected def apply(model: AnyModel, id: Int, parentId: Int, statementId: Int, 
			orderIndex: Int) = 
			StatementPlacement(id, StatementPlacementData(parentId, statementId, orderIndex))
	}
}

/**
  * Common trait for factories which parse statement placement data from database-originated 
  * models
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
trait StatementPlacementDbReader extends StatementPlacementDbReaderLike[StatementPlacement]

