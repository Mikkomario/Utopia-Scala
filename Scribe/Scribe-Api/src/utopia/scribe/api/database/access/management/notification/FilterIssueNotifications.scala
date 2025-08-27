package utopia.scribe.api.database.access.management.notification

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.IssueNotificationDbModel
import utopia.vault.nosql.view.{NullDeprecatableView, TimelineView}

/**
  * Common trait for access points which may be filtered based on issue notification properties
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait FilterIssueNotifications[+Repr] extends NullDeprecatableView[Repr] with TimelineView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines issue notification database properties
	  */
	def model = IssueNotificationDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def timestampColumn = model.created
	
	
	// OTHER	--------------------
	
	/**
	  * @param resolutionId resolution id to target
	  * @return Copy of this access point that only includes issue notifications with the specified 
	  * resolution id
	  */
	def fromResolution(resolutionId: Int) = filter(model.resolutionId.column <=> resolutionId)
	
	/**
	  * @param resolutionIds Targeted resolution ids
	  * @return Copy of this access point that only includes issue notifications where resolution id is 
	  * within the specified value set
	  */
	def fromResolutions(resolutionIds: IterableOnce[Int]) = 
		filter(model.resolutionId.column.in(IntSet.from(resolutionIds)))
}

