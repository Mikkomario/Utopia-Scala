package utopia.logos.database.storable.text

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.props.text.{StatementPlacementDbProps, StatementPlacementDbPropsWrapper}
import utopia.logos.model.partial.text.StatementPlacementData
import utopia.logos.model.stored.text.StatementPlacement
import utopia.vault.model.immutable.{DbPropertyDeclaration, Table}

object StatementPlacementDbModelFactory
{
	// OTHER	--------------------
	
	/**
	  * @return A factory for constructing statement placement database models
	  */
	def apply(table: Table, dbProps: StatementPlacementDbProps) = 
		StatementPlacementDbModelFactoryImpl(table, dbProps)
	
	
	// NESTED	--------------------
	
	/**
	  * Used for constructing StatementPlacementDbModel instances and for inserting statement 
	  * placements to the database
	  * @param table                     Table targeted by these models
	  * @param statementPlacementDbProps Properties which specify how the database interactions are 
	  *                                  performed
	  * @author Mikko Hilpinen
	  * @since 30.12.2024, v0.4
	  */
	case class StatementPlacementDbModelFactoryImpl(table: Table, statementPlacementDbProps: StatementPlacementDbProps)
		extends StatementPlacementDbModelFactory with StatementPlacementDbPropsWrapper
	{
		// ATTRIBUTES	--------------------
		
		override lazy val id = DbPropertyDeclaration("id", index)
		
		
		// IMPLEMENTED	--------------------
		
		override def apply(data: StatementPlacementData): StatementPlacementDbModel = 
			apply(None, Some(data.parentId), Some(data.statementId), Some(data.orderIndex))
		
		override def withId(id: Int) = apply(id = Some(id))
		/**
		  * @param orderIndex 0-based index that indicates the specific location of the placed text
		  * @return A model containing only the specified order index
		  */
		override def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
		/**
		  * @param parentId Id of the text where the placed text appears
		  * @return A model containing only the specified parent id
		  */
		override def withParentId(parentId: Int) = apply(parentId = Some(parentId))
		/**
		  * @param statementId Id of the statement which appears within the linked text
		  * @return A model containing only the specified statement id
		  */
		override def withStatementId(statementId: Int) = apply(statementId = Some(statementId))
		
		override protected def complete(id: Value, data: StatementPlacementData) = StatementPlacement(id.getInt, data)
		
		
		// OTHER	--------------------
		
		/**
		  * @param id statement placement database id
		  * @return Constructs a new statement placement database model with the specified properties
		  */
		def apply(id: Option[Int] = None, parentId: Option[Int] = None, statementId: Option[Int] = None,
		          orderIndex: Option[Int] = None): StatementPlacementDbModel =
			_StatementPlacementDbModel(table, statementPlacementDbProps, id, parentId, statementId, 
				orderIndex)
	}
	
	/**
	  * Used for interacting with StatementPlacements in the database
	  * @param table   Table interacted with when using this model
	  * @param dbProps Configurations of the interacted database properties
	  * @param id      statement placement database id
	  * @author Mikko Hilpinen
	  * @since 30.12.2024, v0.4
	  */
	private case class _StatementPlacementDbModel(table: Table, dbProps: StatementPlacementDbProps,
	                                              id: Option[Int] = None, parentId: Option[Int] = None,
	                                              statementId: Option[Int] = None, orderIndex: Option[Int] = None)
		extends StatementPlacementDbModel
	{
		// IMPLEMENTED	--------------------
		
		override def placedId = statementId
		
		/**
		  * @param id         Id to assign to the new model (default = currently assigned id)
		  * @param parentId   parent id to assign to the new model (default = currently assigned value)
		  * @param placedId   placed id to assign to the new model (default = currently assigned value)
		  * @param orderIndex order index to assign to the new model (default = currently assigned value)
		  */
		override def copyTextPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
			placedId: Option[Int] = placedId, orderIndex: Option[Int] = orderIndex) = 
			copy(id = id, parentId = parentId, statementId = placedId, orderIndex = orderIndex)
		
		/**
		  * @param id          Id to assign to the new model (default = currently assigned id)
		  * @param parentId    parent id to assign to the new model (default = currently assigned value)
		  * @param statementId statement id to assign to the new model (default = currently assigned 
		  *                    value)
		  * @param orderIndex  order index to assign to the new model (default = currently assigned value)
		  * @return Copy of this model with the specified statement placement properties
		  */
		override protected def copyStatementPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
			statementId: Option[Int] = statementId, orderIndex: Option[Int] = orderIndex) = 
			copy(id = id, parentId = parentId, statementId = statementId, orderIndex = orderIndex)
	}
}

/**
  * Common trait for factories yielding statement placement database models
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacementDbModelFactory 
	extends StatementPlacementDbModelFactoryLike[StatementPlacementDbModel, StatementPlacement, StatementPlacementData] 
		with TextPlacementDbModelFactoryLike[StatementPlacementDbModel, StatementPlacement, StatementPlacementData]

