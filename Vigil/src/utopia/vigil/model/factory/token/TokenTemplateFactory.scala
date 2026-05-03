package utopia.vigil.model.factory.token

import utopia.flow.time.Duration
import utopia.vigil.model.enumeration.ScopeGrantType

import java.time.Instant

/**
  * Common trait for token template-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenTemplateFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param duration New duration to assign
	  * @return Copy of this item with the specified duration
	  */
	def withDuration(duration: Duration): A
	
	/**
	  * @param name New name to assign
	  * @return Copy of this item with the specified name
	  */
	def withName(name: String): A
	
	/**
	  * @param scopeGrantType New scope grant type to assign
	  * @return Copy of this item with the specified scope grant type
	  */
	def withScopeGrantType(scopeGrantType: ScopeGrantType): A
}

