package utopia.scribe.core.model.factory.management

import java.time.Instant

/**
  * Common trait for comment-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
trait CommentFactory[+A]
{
	// ABSTRACT	--------------------
	
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
	  * @param text New text to assign
	  * @return Copy of this item with the specified text
	  */
	def withText(text: String): A
}

