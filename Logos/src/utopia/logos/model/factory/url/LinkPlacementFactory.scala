package utopia.logos.model.factory.url

/**
  * Common trait for link placement-related factories which allow construction with individual properties
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
trait LinkPlacementFactory[+A]
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
}

