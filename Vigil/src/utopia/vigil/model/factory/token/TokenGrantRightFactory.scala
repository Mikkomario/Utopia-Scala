package utopia.vigil.model.factory.token

import utopia.flow.util.UncertainBoolean

/**
  * Common trait for token grant right-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
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
	  * @param revokesEarlier New revokes earlier to assign
	  * @return Copy of this item with the specified revokes earlier
	  */
	def withRevokesEarlier(revokesEarlier: UncertainBoolean): A
	
	/**
	  * @param revokesOriginal New revokes original to assign
	  * @return Copy of this item with the specified revokes original
	  */
	def withRevokesOriginal(revokesOriginal: Boolean): A
}

