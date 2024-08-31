package utopia.logos.model.combined.text

import utopia.logos.model.partial.text.{StatementData, TextPlacementDataLike}
import utopia.logos.model.stored.text.{StoredStatement, StoredTextPlacementLike}

/**
  * Common trait for implementations that link a statement with its placement in some text
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait PlacedStatementLike[+Repr, +Placement <: StoredTextPlacementLike[PlacementData, Placement],
	PlacementData <: TextPlacementDataLike[PlacementData]]
	extends PlacedTextLike[Repr, StoredStatement, StatementData, Placement, PlacementData]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The wrapped statement
	  */
	def statement: StoredStatement
	
	
	// IMPLEMENTED  ------------------
	
	override def placedText: StoredStatement = statement
}