package utopia.logos.model.factory.word

import utopia.logos.model.enumeration.DisplayStyle

/**
  * Common trait for word placement-related factories which allow construction with individual properties
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
trait WordPlacementFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param orderIndex New order index to assign
	  * @return Copy of this item with the specified order index
	  */
	def withOrderIndex(orderIndex: Int): A
	
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
}

