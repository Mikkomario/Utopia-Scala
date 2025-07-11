package utopia.logos.model.partial.text

import utopia.logos.model.factory.text.StatementPlacementFactory

/**
  * Common trait for classes which provide read and copy access to statement placement properties
  * @tparam Repr Implementing data class or data wrapper class
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacementDataLike[+Repr <: TextPlacementData] 
	extends HasStatementPlacementProps with StatementPlacementFactory[Repr] with TextPlacementDataLike[Repr] 
		with TextPlacementData
{
	// ABSTRACT	--------------------
	
	/**
	  * Builds a modified copy of this statement placement
	  * @param parentId    New parent id to assign. Default = current value.
	  * @param statementId New statement id to assign. Default = current value.
	  * @param orderIndex  New order index to assign. Default = current value.
	  * @return A copy of this statement placement with the specified properties
	  */
	def copyStatementPlacement(parentId: Int = parentId, statementId: Int = statementId, 
		orderIndex: Int = orderIndex): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def copyTextPlacement(parentId: Int, placedId: Int, orderIndex: Int) = 
		copyStatementPlacement(parentId = parentId, statementId = placedId, orderIndex = orderIndex)
	
	override def withOrderIndex(orderIndex: Int) = copyStatementPlacement(orderIndex = orderIndex)
	override def withParentId(parentId: Int) = copyStatementPlacement(parentId = parentId)
	override def withStatementId(statementId: Int) = copyStatementPlacement(statementId = statementId)
}

