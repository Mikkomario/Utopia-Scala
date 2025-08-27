package utopia.scribe.api.database.access.management.comment

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.CommentDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual comment values from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class AccessCommentValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing comment database properties
	  */
	val model = CommentDbModel
	
	/**
	  * Access to comment id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * ID of the commented issue
	  */
	lazy val issueId = apply(model.issueId).optional { v => v.int }
	
	/**
	  * The text contents of this comment
	  */
	lazy val text = apply(model.text) { v => v.getString }
	
	/**
	  * Time when this comment was recorded
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

