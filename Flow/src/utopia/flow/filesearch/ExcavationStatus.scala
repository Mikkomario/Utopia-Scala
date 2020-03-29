package utopia.flow.filesearch

/**
 * Common trait for various statuses an excavation (file search) may have
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
sealed trait ExcavationStatus extends Ordered[ExcavationStatus]
{
	// ABSTRACT	----------------------
	
	/**
	 * @return Index used for ordering various statuses
	 */
	def orderIndex: Int
	
	/**
	 * @return Whether anyone has even passed through this area yet
	 */
	def isTraversed: Boolean
	
	/**
	 * @return Whether the excavation has been started already
	 */
	def isStarted: Boolean
	
	/**
	 * @return Whether the excavation has been completed already
	 */
	def isCompleted: Boolean
	
	
	// IMPLEMENTED	-----------------
	
	override def compare(that: ExcavationStatus) = orderIndex - that.orderIndex
}

object ExcavationStatus
{
	/**
	 * Status used while excavation hasn't been started yet
	 */
	case object Unexplored extends ExcavationStatus
	{
		override val orderIndex = 1
		override val isTraversed = false
		override val isStarted = false
		override val isCompleted = false
	}
	
	case object Passed extends ExcavationStatus
	{
		override val orderIndex = 2
		override val isTraversed = true
		override val isStarted = false
		override val isCompleted = false
	}
	
	/**
	 * Status used while excavation has been started but no results have been found
	 */
	case object Started extends ExcavationStatus
	{
		override val orderIndex = 3
		override val isTraversed = true
		override val isStarted = true
		override val isCompleted = false
	}
	
	/**
	 * Status used when excavation has been completed with results
	 */
	case object Finished extends ExcavationStatus
	{
		override val orderIndex = 6
		override val isTraversed = true
		override val isStarted = true
		override val isCompleted = true
	}
}
