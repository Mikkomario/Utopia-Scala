package utopia.logos.model.combined.text

import utopia.logos.model.partial.text.StatementPlacementData
import utopia.logos.model.stored.text.{StatementPlacement, StoredStatement}

object PlacedStatement
{
	// OTHER    -----------------------
	
	/**
	  * @param statement Statement to wrap
	  * @param placement Placement to attach to that statement
	  * @return A combination of the specified statement and placement
	  */
	def apply(statement: StoredStatement, placement: StatementPlacement): PlacedStatement =
		_PlacedStatement(statement, placement)
	
	
	// NESTED   -----------------------
	
	private case class _PlacedStatement(statement: StoredStatement, placement: StatementPlacement)
		extends PlacedStatement
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
	extends PlacedStatementLike[PlacedStatement, StatementPlacement, StatementPlacementData]
		with CombinedStatement[PlacedStatement]
