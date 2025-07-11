package utopia.logos.model.stored.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.model.partial.text.{StatementPlacementData, TextPlacementData}
import utopia.vault.model.template.StoredFromModelFactory

object StatementPlacement extends StoredFromModelFactory[StatementPlacementData, StatementPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = StatementPlacementData
	
	override protected def complete(model: AnyModel, data: StatementPlacementData) = 
		model("id").tryInt.map { apply(_, data) }
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new statement placement
	  * @param id   id of this statement placement in the database
	  * @param data Wrapped statement placement data
	  * @return statement placement with the specified id and wrapped data
	  */
	def apply(id: Int, data: StatementPlacementData): StatementPlacement = _StatementPlacement(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the statement placement trait
	  * @param id   id of this statement placement in the database
	  * @param data Wrapped statement placement data
	  * @author Mikko Hilpinen
	  * @since 30.12.2024
	  */
	private case class _StatementPlacement(id: Int, data: StatementPlacementData) extends StatementPlacement
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: StatementPlacementData) = copy(data = data)
	}
}

/**
  * Represents a statement placement that has already been stored in the database
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacement 
	extends StoredStatementPlacementLike[StatementPlacementData, StatementPlacement] 
		with StatementPlacementData with TextPlacementData 
		with StoredTextPlacementLike[StatementPlacementData, StatementPlacement]

