package utopia.vigil.model.factory.scope

import java.time.Instant

/**
  * Common trait for scope right-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param scopeId New scope id to assign
	  * @return Copy of this item with the specified scope id
	  */
	def withScopeId(scopeId: Int): A
	
	/**
	  * @param usable New usable to assign
	  * @return Copy of this item with the specified usable
	  */
	def withUsable(usable: Boolean): A
}

