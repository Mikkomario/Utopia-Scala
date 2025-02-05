package utopia.logos.database.factory.text

import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.logos.model.combined.text.PlacedStatement
import utopia.logos.model.stored.text.{StatementPlacement, StoredStatement, TextPlacement}
import utopia.vault.model.template.HasTable
import utopia.vault.nosql.factory.row.FromRowFactory

object PlacedStatementDbFactory
{
	// OTHER    ---------------------
	
	/**
	 * @param placementModel Model for interacting with the placement links
	 * @return A factory for reading placed statements
	 */
	def apply(placementModel: StatementPlacementDbProps with HasTable): PlacedStatementDbFactory =
		apply(StatementPlacementDbFactory(placementModel.table, placementModel))
	/**
	  * @param placementFactory Text placement factory used for reading the statement placements
	  * @return A factory that pulls placed statements
	  */
	def apply(placementFactory: FromRowFactory[StatementPlacement]): PlacedStatementDbFactory =
		_PlacedStatementDbFactory(placementFactory)
	
	
	// NESTED   ---------------------
	
	private case class _PlacedStatementDbFactory(childFactory: FromRowFactory[StatementPlacement])
		extends PlacedStatementDbFactory
}

/**
  * Common trait for factories that yield placed statements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait PlacedStatementDbFactory extends PlacedStatementDbFactoryLike[PlacedStatement, StatementPlacement]
{
	override def apply(parent: StoredStatement, child: StatementPlacement): PlacedStatement =
		PlacedStatement(parent, child)
}
