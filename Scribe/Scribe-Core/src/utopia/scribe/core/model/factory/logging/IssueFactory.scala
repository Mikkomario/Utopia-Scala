package utopia.scribe.core.model.factory.logging

import utopia.scribe.core.model.enumeration.Severity

import java.time.Instant

/**
  * Common trait for issue-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param context New context to assign
	  * @return Copy of this item with the specified context
	  */
	def withContext(context: String): A
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param severity New severity to assign
	  * @return Copy of this item with the specified severity
	  */
	def withSeverity(severity: Severity): A
}

