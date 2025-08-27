package utopia.scribe.core.model.factory.management

import java.time.Instant

/**
  * Common trait for comment-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
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
	  * @param issueVariantId New issue variant id to assign
	  * @return Copy of this item with the specified issue variant id
	  */
	def withIssueVariantId(issueVariantId: Int): A
	
	/**
	  * @param text New text to assign
	  * @return Copy of this item with the specified text
	  */
	def withText(text: String): A
}

