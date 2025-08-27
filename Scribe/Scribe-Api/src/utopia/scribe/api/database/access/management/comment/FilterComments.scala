package utopia.scribe.api.database.access.management.comment

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.CommentDbModel
import utopia.vault.nosql.view.TimelineView

/**
  * Common trait for access points which may be filtered based on comment properties
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
trait FilterComments[+Repr] extends TimelineView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines comment database properties
	  */
	def model = CommentDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def timestampColumn = model.created
	
	
	// OTHER	--------------------
	
	/**
	  * @param issueId issue id to target
	  * @return Copy of this access point that only includes comments with the specified issue id
	  */
	def onIssue(issueId: Int) = filter(model.issueId.column <=> issueId)
	/**
	  * @param issueIds Targeted issue ids
	  * @return Copy of this access point that only includes comments where issue id is within the specified 
	  * value set
	  */
	def onIssues(issueIds: IterableOnce[Int]) = filter(model.issueId.column.in(IntSet.from(issueIds)))
}

