package utopia.logos.model.factory.text

import utopia.logos.model.enumeration.DisplayStyle

/**
  * Common trait for classes that implement WordPlacementFactory by wrapping a WordPlacementFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait WordPlacementFactoryWrapper[A <: WordPlacementFactory[A], +Repr] 
	extends WordPlacementFactory[Repr] with TextPlacementFactoryWrapper[A, Repr]
{
	// IMPLEMENTED	--------------------
	
	override def withStatementId(statementId: Int) = withParentId(statementId)
	
	override def withStyle(style: DisplayStyle) = mapWrapped { _.withStyle(style) }
	
	override def withWordId(wordId: Int) = withPlacedId(wordId)
}

