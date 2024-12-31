package utopia.logos.model.factory.text

/**
  * Common trait for statement placement-related factories which allow construction with 
  * individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.3.1
  */
trait StatementPlacementFactory[+A] extends TextPlacementFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param statementId New statement id to assign
	  * @return Copy of this item with the specified statement id
	  */
	def withStatementId(statementId: Int): A
	
	
	// IMPLEMENTED	--------------------
	
	override def withPlacedId(placedId: Int) = withStatementId(placedId)
}

