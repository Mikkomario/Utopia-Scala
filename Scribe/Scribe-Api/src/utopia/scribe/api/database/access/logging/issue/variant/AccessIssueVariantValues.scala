package utopia.scribe.api.database.access.logging.issue.variant

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.logging.IssueVariantDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing issue variant values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueVariantValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue variant database properties
	  */
	val model = IssueVariantDbModel
	
	/**
	  * Access to issue variant ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * Id of the issue that occurred
	  */
	lazy val issueIds = apply(model.issueId) { v => v.getInt }
	/**
	  * The program version in which this issue (variant) occurred
	  */
	lazy val versions = apply(model.version) { v => Version(v.getString) } { v: Version => v.toString }
	/**
	  * Id of the error / exception that is associated with this issue (variant). None if not 
	  * applicable.
	  */
	lazy val errorIds = apply(model.errorId).flatten { v => v.int }
	/**
	  * Details about this case and/or setting.
	  */
	lazy val details = apply(model.details) { v =>
		v.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }
	}
	/**
	  * Time when this case or variant was first encountered
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	
	
	// COMPUTED --------------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return Latest accessible variant version per issue ID
	 */
	def latestVersionPerIssue(implicit connection: Connection) =
		access.streamColumns(model.issueId, model.version) { valuesIter =>
			valuesIter.groupMapReduce { _.head.getInt } { v => Version(v(1).getString) } { _ max _ }
		}
}

