package utopia.scribe.api.database.access.management.comment

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.CommentDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing comment values from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessCommentValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing comment database properties
	  */
	val model = CommentDbModel
	
	/**
	  * Access to comment ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * ID of the commented issue variant
	  */
	lazy val issueVariantIds = apply(model.issueVariantId) { v => v.getInt }
	
	/**
	  * The text contents of this comment
	  */
	lazy val texts = apply(model.text) { v => v.getString }
	
	/**
	  * Time when this comment was recorded
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

