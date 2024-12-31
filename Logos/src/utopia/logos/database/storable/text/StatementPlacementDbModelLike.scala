package utopia.logos.database.storable.text

import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.logos.model.factory.text.StatementPlacementFactory

/**
  * Common trait for database models used for interacting with statement placement data in the 
  * database
  * @tparam Repr Type of this DB model
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.3.1
  */
trait StatementPlacementDbModelLike[+Repr] 
	extends TextPlacementDbModelLike[Repr] with StatementPlacementFactory[Repr]
{
	// ABSTRACT	--------------------
	
	def statementId: Option[Int]
	
	/**
	  * Access to the database properties which are utilized in this model
	  */
	override def dbProps: StatementPlacementDbProps
	
	/**
	  * @param id          Id to assign to the new model (default = currently assigned id)
	  * @param parentId    parent id to assign to the new model (default = currently assigned value)
	  * @param statementId statement id to assign to the new model (default = currently assigned 
	  *                    value)
	  * @param orderIndex  order index to assign to the new model (default = currently assigned value)
	  * @return Copy of this model with the specified statement placement properties
	  */
	protected def copyStatementPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
		statementId: Option[Int] = statementId, orderIndex: Option[Int] = orderIndex): Repr
	
	
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
		copyStatementPlacement(id = id, parentId = parentId, statementId = placedId, orderIndex = orderIndex)
	
	override def withPlacedId(placedId: Int) = withStatementId(placedId)
	
	/**
	  * @param statementId Id of the statement which appears within the linked text
	  * @return A new copy of this model with the specified statement id
	  */
	override def withStatementId(statementId: Int) = copyStatementPlacement(statementId = Some(statementId))
}

