package utopia.vigil.model.factory.token

import utopia.vigil.model.factory.scope.ScopeRightFactory

/**
  * Common trait for token template scope-related factories which allow construction with 
  * individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenTemplateScopeFactory[+A] extends ScopeRightFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param templateId New template id to assign
	  * @return Copy of this item with the specified template id
	  */
	def withTemplateId(templateId: Int): A
}

