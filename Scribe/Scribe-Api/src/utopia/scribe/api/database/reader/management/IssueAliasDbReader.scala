package utopia.scribe.api.database.reader.management

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.management.IssueAliasDbModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.management.IssueAliasData
import utopia.scribe.core.model.stored.management.IssueAlias
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading issue alias data from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
object IssueAliasDbReader 
	extends DbRowReader[IssueAlias] with ParseTableModel[IssueAlias] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = IssueAliasDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(IssueAlias(valid(this.model.id.name).getInt, 
			IssueAliasData(issueId = valid(this.model.issueId.name).getInt, 
			alias = valid(this.model.alias.name).getString, 
			newSeverity = Severity.findForValue(valid(this.model.newSeverity.name)), 
			created = valid(this.model.created.name).getInstant)))
}

