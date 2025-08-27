package utopia.scribe.api.database.access.management.resolution

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.ResolutionDbModel
import utopia.vault.nosql.view.{TimeDeprecatableView, TimelineView}

/**
  * Common trait for access points which may be filtered based on resolution properties
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait FilterResolutions[+Repr] extends TimelineView[Repr] with TimeDeprecatableView[Repr]
{
	// IMPLEMENTED	--------------------
	
	/**
	 * Model that defines resolution database properties
	 */
	override def model = ResolutionDbModel
	
	override def timestampColumn = model.created
	
	
	// OTHER	--------------------
	
	/**
	  * @param resolvedIssueId resolved issue id to target
	  * @return Copy of this access point that only includes resolutions with the specified resolved issue id
	  */
	def ofIssue(resolvedIssueId: Int) = filter(model.resolvedIssueId.column <=> resolvedIssueId)
	/**
	  * @param resolvedIssueIds Targeted resolved issue ids
	  * @return Copy of this access point that only includes resolutions where resolved issue id is within 
	  * the specified value set
	  */
	def ofIssues(resolvedIssueIds: IterableOnce[Int]) = 
		filter(model.resolvedIssueId.column.in(IntSet.from(resolvedIssueIds)))
	
	/**
	  * @param commentId comment id to target
	  * @return Copy of this access point that only includes resolutions with the specified comment id
	  */
	def withComment(commentId: Int) = filter(model.commentId.column <=> commentId)
	/**
	  * @param commentIds Targeted comment ids
	  * @return Copy of this access point that only includes resolutions where comment id is within the 
	  * specified value set
	  */
	def withComments(commentIds: IterableOnce[Int]) = 
		filter(model.commentId.column.in(IntSet.from(commentIds)))
	
	/**
	  * @param notifies notifies to target
	  * @return Copy of this access point that only includes resolutions with the specified notifies
	  */
	def withNotifies(notifies: Boolean) = filter(model.notifies.column <=> notifies)
}

