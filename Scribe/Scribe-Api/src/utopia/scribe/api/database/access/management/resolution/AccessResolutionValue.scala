package utopia.scribe.api.database.access.management.resolution

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.management.ResolutionDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual resolution values from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessResolutionValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing resolution database properties
	  */
	val model = ResolutionDbModel
	
	/**
	  * Access to resolution id
	  */
	lazy val id = apply(model.index).optional { _.int }
	/**
	  * ID of the resolved issue
	  */
	lazy val resolvedIssueId = apply(model.resolvedIssueId).optional { v => v.int }
	/**
	  * ID of the comment added to this resolution, if applicable
	  */
	lazy val commentId = apply(model.commentId).optional { v => v.int }
	/**
	  * The last version number (inclusive), to which silencing may apply, and for which 
	  * notifications are NOT generated. 
	  * None if not restricted by version.
	  */
	lazy val versionThreshold =
		apply(model.versionThreshold).optional { v => v.string.map(Version.apply) } { _.toString }
	/**
	  * Time when this resolution was registered
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
	/**
	  * Time when this resolution expires, was removed or was broken. May be in the future.
	  */
	lazy val deprecates = apply(model.deprecates).optional { v => v.instant }
	/**
	  * Whether the issue should not be reported while this resolution is active.
	  */
	lazy val silences = apply(model.silences).optional { v => v.boolean }
	/**
	  * Whether a notification should be generated if this resolution is broken. 
	  * Note: Only one notification may be generated in total.
	  */
	lazy val notifies = apply(model.notifies).optional { v => v.boolean }
}

