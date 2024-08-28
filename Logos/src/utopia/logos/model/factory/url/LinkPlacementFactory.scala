package utopia.logos.model.factory.url

import utopia.logos.model.factory.text.TextPlacementFactory

/**
  * Common trait for link placement-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait LinkPlacementFactory[+A] extends TextPlacementFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param linkId New link id to assign
	  * @return Copy of this item with the specified link id
	  */
	def withLinkId(linkId: Int): A
	
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
	
	
	// IMPLEMENTED	--------------------
	
	override def withParentId(parentId: Int) = withStatementId(parentId)
	
	override def withPlacedId(placedId: Int) = withLinkId(placedId)
}

