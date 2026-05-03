package utopia.vigil.model.factory.token

/**
  * Common trait for token grant right-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenGrantRightFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param grantedTemplateId New granted template id to assign
	  * @return Copy of this item with the specified granted template id
	  */
	def withGrantedTemplateId(grantedTemplateId: Int): A
	
	/**
	  * @param ownerTemplateId New owner template id to assign
	  * @return Copy of this item with the specified owner template id
	  */
	def withOwnerTemplateId(ownerTemplateId: Int): A
	
	/**
	  * @param revokes New revokes to assign
	  * @return Copy of this item with the specified revokes
	  */
	def withRevokes(revokes: Boolean): A
}

