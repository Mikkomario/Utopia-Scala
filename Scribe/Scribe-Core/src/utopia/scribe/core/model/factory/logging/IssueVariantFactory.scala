package utopia.scribe.core.model.factory.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version

import java.time.Instant

/**
  * Common trait for issue variant-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
trait IssueVariantFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param details New details to assign
	  * @return Copy of this item with the specified details
	  */
	def withDetails(details: Model): A
	
	/**
	  * @param errorId New error id to assign
	  * @return Copy of this item with the specified error id
	  */
	def withErrorId(errorId: Int): A
	
	/**
	  * @param issueId New issue id to assign
	  * @return Copy of this item with the specified issue id
	  */
	def withIssueId(issueId: Int): A
	
	/**
	  * @param version New version to assign
	  * @return Copy of this item with the specified version
	  */
	def withVersion(version: Version): A
}

