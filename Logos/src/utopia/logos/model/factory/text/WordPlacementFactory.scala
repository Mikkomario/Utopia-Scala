package utopia.logos.model.factory.text

import utopia.logos.model.enumeration.DisplayStyle

/**
  * Common trait for word placement-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait WordPlacementFactory[+A] extends TextPlacementFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param statementId New statement id to assign
	  * @return Copy of this item with the specified statement id
	  */
	def withStatementId(statementId: Int): A
	
	/**
	  * @param style New style to assign
	  * @return Copy of this item with the specified style
	  */
	def withStyle(style: DisplayStyle): A
	
	/**
	  * @param wordId New word id to assign
	  * @return Copy of this item with the specified word id
	  */
	def withWordId(wordId: Int): A
	
	
	// IMPLEMENTED	--------------------
	
	override def withParentId(parentId: Int) = withStatementId(parentId)
	
	override def withPlacedId(placedId: Int) = withWordId(placedId)
}

