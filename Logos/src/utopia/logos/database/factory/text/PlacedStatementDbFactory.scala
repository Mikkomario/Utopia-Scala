package utopia.logos.database.factory.text

import utopia.logos.model.combined.text.PlacedStatement
import utopia.logos.model.stored.text.{StoredStatement, TextPlacement}
import utopia.vault.nosql.factory.row.FromRowFactory

object PlacedStatementDbFactory
{
	// OTHER    ---------------------
	
	/**
	  * @param placementFactory Text placement factory used for reading the statement placements
	  * @return A factory that pulls placed statements
	  */
	def apply(placementFactory: FromRowFactory[TextPlacement]): PlacedStatementDbFactory =
		_PlacedStatementDbFactory(placementFactory)
	
	
	// NESTED   ---------------------
	
	private case class _PlacedStatementDbFactory(childFactory: FromRowFactory[TextPlacement])
		extends PlacedStatementDbFactory
}

/**
  * Common trait for factories that yield placed statements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait PlacedStatementDbFactory extends PlacedStatementDbFactoryLike[PlacedStatement, TextPlacement]
{
	override def apply(parent: StoredStatement, child: TextPlacement): PlacedStatement = PlacedStatement(parent, child)
}
