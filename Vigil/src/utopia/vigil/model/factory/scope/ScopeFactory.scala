package utopia.vigil.model.factory.scope

/**
  * Common trait for scope-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param key New key to assign
	  * @return Copy of this item with the specified key
	  */
	def withKey(key: String): A
	
	/**
	  * @param parentId New parent id to assign
	  * @return Copy of this item with the specified parent id
	  */
	def withParentId(parentId: Int): A
}

