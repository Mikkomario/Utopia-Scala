package utopia.logos.model.stored.text

import utopia.logos.model.factory.text.StatementPlacementFactoryWrapper
import utopia.logos.model.partial.text.{StatementPlacementDataLike, TextPlacementData}

/**
  * Common trait for statement placements which have been stored in the database
  * @tparam Data Type of the wrapped data
  * @tparam Repr Implementing type
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StoredStatementPlacementLike[Data <: StatementPlacementDataLike[Data], +Repr <: TextPlacementData] 
	extends StatementPlacementFactoryWrapper[Data, Repr] with StatementPlacementDataLike[Repr] 
		with StoredTextPlacementLike[Data, Repr]
{
	// IMPLEMENTED	--------------------
	
	override def statementId = data.statementId
	
	override protected def wrappedFactory = data
	
	override def copyStatementPlacement(parentId: Int, statementId: Int, orderIndex: Int) = 
		wrap(data.copyStatementPlacement(parentId, statementId, orderIndex))
}

