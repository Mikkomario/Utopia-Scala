package utopia.logos.model.factory.text

/**
  * Common trait for classes that implement StatementPlacementFactory by wrapping a 
  * StatementPlacementFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.3.1
  */
trait StatementPlacementFactoryWrapper[A <: StatementPlacementFactory[A], +Repr] 
	extends StatementPlacementFactory[Repr] with TextPlacementFactoryWrapper[A, Repr]
{
	// IMPLEMENTED	--------------------
	
	override def withStatementId(statementId: Int) = withPlacedId(statementId)
}

