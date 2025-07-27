package utopia.scribe.api.database.reader.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.logging.IssueDbModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading issue data from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueDbReader extends DbRowReader[Issue] with ParseTableModel[Issue] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = IssueDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Issue(valid(this.model.id.name).getInt, IssueData(valid(this.model.context.name).getString, 
			Severity.fromValue(valid(this.model.severity.name)), valid(this.model.created.name).getInstant)))
}

