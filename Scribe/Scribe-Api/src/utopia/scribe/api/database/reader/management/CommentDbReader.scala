package utopia.scribe.api.database.reader.management

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.management.CommentDbModel
import utopia.scribe.core.model.partial.management.CommentData
import utopia.scribe.core.model.stored.management.Comment
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading comment data from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
object CommentDbReader extends DbRowReader[Comment] with ParseTableModel[Comment] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = CommentDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Comment(valid(this.model.id.name).getInt, 
			CommentData(issueId = valid(this.model.issueId.name).getInt, 
			text = valid(this.model.text.name).getString, 
			created = valid(this.model.created.name).getInstant)))
}

