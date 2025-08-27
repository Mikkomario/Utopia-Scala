package utopia.scribe.api.database.access.management.comment

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.CommentDbModel
import utopia.vault.nosql.view.TimelineView

/**
  * Common trait for access points which may be filtered based on comment properties
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
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
	  * @param issueVariantId issue variant id to target
	  * @return Copy of this access point that only includes comments with the specified issue variant id
	  */
	def onVariant(issueVariantId: Int) = filter(model.issueVariantId.column <=> issueVariantId)
	
	/**
	  * @param issueVariantIds Targeted issue variant ids
	  * @return Copy of this access point that only includes comments where issue variant id is within the 
	  * specified value set
	  */
	def onVariants(issueVariantIds: IterableOnce[Int]) = 
		filter(model.issueVariantId.column.in(IntSet.from(issueVariantIds)))
}

