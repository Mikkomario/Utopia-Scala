package utopia.logos.model.combined.text

import utopia.logos.model.partial.text.TextPlacementData
import utopia.logos.model.stored.text.{StoredStatement, TextPlacement}

object PlacedStatement
{
	// OTHER    -----------------------
	
	/**
	  * @param statement Statement to wrap
	  * @param placement Placement to attach to that statement
	  * @return A combination of the specified statement and placement
	  */
	def apply(statement: StoredStatement, placement: TextPlacement): PlacedStatement = _PlacedStatement(statement, placement)
	
	
	// NESTED   -----------------------
	
	private case class _PlacedStatement(statement: StoredStatement, placement: TextPlacement) extends PlacedStatement
	{
		override protected def wrap(factory: StoredStatement): PlacedStatement = copy(statement = factory)
	}
}

/**
  * Common trait for combinations that link statements individual text placements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait PlacedStatement
	extends CombinedStatement[PlacedStatement]
		with PlacedStatementLike[PlacedStatement, TextPlacement, TextPlacementData]
