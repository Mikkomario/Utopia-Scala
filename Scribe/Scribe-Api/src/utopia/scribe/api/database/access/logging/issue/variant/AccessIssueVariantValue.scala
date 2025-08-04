package utopia.scribe.api.database.access.logging.issue.variant

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.logging.IssueVariantDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual issue variant values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueVariantValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue variant database properties
	  */
	val model = IssueVariantDbModel
	
	/**
	  * Access to issue variant id
	  */
	lazy val id = apply(model.index).optional { _.int }
	/**
	  * Id of the issue that occurred
	  */
	lazy val issueId = apply(model.issueId).optional { v => v.int }
	/**
	  * The program version in which this issue (variant) occurred
	  */
	lazy val version =
		apply(model.version).custom { v => v.string.map { v => Version(v) } } { v: Version => v.toString }.concrete
	/**
	  * Id of the error / exception that is associated with this issue (variant). None if not 
	  * applicable.
	  */
	lazy val errorId = apply(model.errorId).optional { v => v.int }
	/**
	  * Details about this case and/or setting.
	  */
	lazy val details = apply(model.details) { v =>
		v.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }
	}
	/**
	  * Time when this case or variant was first encountered
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

