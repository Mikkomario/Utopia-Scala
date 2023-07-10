package utopia.scribe.api.model.enumeration

import utopia.flow.collection.immutable.Pair

/**
  * An enumeration for different approaches of (log) cleaning processes
  * @author Mikko Hilpinen
  * @since 9.7.2023, v1.0
  */
sealed trait CleanupOperation

object CleanupOperation
{
	// ATTRIBUTES   ------------------
	
	/**
	  * All available cleanup operation type values
	  */
	val values = Pair[CleanupOperation](Merge, Delete)
	
	
	// VALUES   ----------------------
	
	/**
	  * Operation where similar and/or consecutive entries are merged together in order to conserve space
	  */
	case object Merge extends CleanupOperation
	/**
	  * Operation where old entries are deleted
	  */
	case object Delete extends CleanupOperation
}