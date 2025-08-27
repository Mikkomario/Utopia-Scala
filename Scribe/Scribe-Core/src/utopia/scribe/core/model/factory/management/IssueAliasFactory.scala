package utopia.scribe.core.model.factory.management

import java.time.Instant

/**
  * Common trait for issue alias-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait IssueAliasFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param alias New alias to assign
	  * @return Copy of this item with the specified alias
	  */
	def withAlias(alias: String): A
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param issueId New issue id to assign
	  * @return Copy of this item with the specified issue id
	  */
	def withIssueId(issueId: Int): A
	
	/**
	  * @param newSeverity New new severity to assign
	  * @return Copy of this item with the specified new severity
	  */
	def withNewSeverity(newSeverity: Int): A
}

