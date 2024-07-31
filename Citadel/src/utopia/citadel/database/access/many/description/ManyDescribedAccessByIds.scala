package utopia.citadel.database.access.many.description

import utopia.flow.generic.casting.ValueConversions._

/**
  * A common trait for access points that target multiple items that use descriptions and where
  *  the search condition
  * is based on a set of ids
  * @author Mikko Hilpinen
  * @since 13.10.2021, v1.3
  */
trait ManyDescribedAccessByIds[A, +D] extends ManyDescribedAccess[A, D]
{
	// ABSTRACT	--------------------
	
	/**
	  * Ids of the accessible items
	  */
	def ids: Set[Int]
	
	
	// COMPUTED	--------------------
	
	/**
	  * The search condition used by this access point
	  */
	def condition = index in ids
	
	/**
	  * An access point to the descriptions belonging to these items
	  */
	def descriptions = manyDescriptionsAccess(ids)
	
	
	// IMPLEMENTED	--------------------
	
	override def accessCondition = Some(condition)
}

