package utopia.scribe.api.database.access.management.resolution

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.management.ResolutionDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing resolution values from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessResolutionValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing resolution database properties
	  */
	val model = ResolutionDbModel
	
	/**
	  * Access to resolution ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * ID of the resolved issue
	  */
	lazy val resolvedIssueIds = apply(model.resolvedIssueId) { v => v.getInt }
	/**
	  * ID of the comment added to this resolution, if applicable
	  */
	lazy val commentIds = apply(model.commentId).flatten { v => v.int }
	/**
	  * The last version number (inclusive), to which silencing may apply, and for which 
	  * notifications are NOT generated. 
	  * None if not restricted by version.
	  */
	lazy val versionThresholds = 
		apply(model.versionThreshold).flatten { v => v.string.map(Version.apply) } { _.toString }
	/**
	  * Time when this resolution was registered
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	/**
	  * Time when this resolution expires, was removed or was broken. May be in the future.
	  */
	lazy val deprecateAt = apply(model.deprecates).flatten { v => v.instant }
	/**
	  * Whether the issue should not be reported while this resolution is active.
	  */
	lazy val silence = apply(model.silences) { v => v.getBoolean }
	/**
	  * Whether a notification should be generated if this resolution is broken. 
	  * Note: Only one notification may be generated in total.
	  */
	lazy val notifyWhenBroken = apply(model.notifies) { v => v.getBoolean }
}

