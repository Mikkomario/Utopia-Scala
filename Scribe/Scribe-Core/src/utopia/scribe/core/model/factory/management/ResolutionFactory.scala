package utopia.scribe.core.model.factory.management

import utopia.flow.util.Version

import java.time.Instant

/**
  * Common trait for resolution-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait ResolutionFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param commentId New comment id to assign
	  * @return Copy of this item with the specified comment id
	  */
	def withCommentId(commentId: Int): A
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param deprecates New deprecates to assign
	  * @return Copy of this item with the specified deprecates
	  */
	def withDeprecates(deprecates: Instant): A
	
	/**
	  * @param notifies New notifies to assign
	  * @return Copy of this item with the specified notifies
	  */
	def withNotifies(notifies: Boolean): A
	
	/**
	  * @param resolvedIssueId New resolved issue id to assign
	  * @return Copy of this item with the specified resolved issue id
	  */
	def withResolvedIssueId(resolvedIssueId: Int): A
	
	/**
	  * @param silences New silences to assign
	  * @return Copy of this item with the specified silences
	  */
	def withSilences(silences: Boolean): A
	
	/**
	  * @param versionThreshold New version threshold to assign
	  * @return Copy of this item with the specified version threshold
	  */
	def withVersionThreshold(versionThreshold: Version): A
}

