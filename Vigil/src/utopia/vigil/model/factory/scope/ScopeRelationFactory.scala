package utopia.vigil.model.factory.scope

/**
  * Common trait for scope relation-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
trait ScopeRelationFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param grantedScopeId New granted scope id to assign
	  * @return Copy of this item with the specified granted scope id
	  */
	def withGrantedScopeId(grantedScopeId: Int): A
	
	/**
	  * @param parentScopeId New parent scope id to assign
	  * @return Copy of this item with the specified parent scope id
	  */
	def withParentScopeId(parentScopeId: Int): A
}

