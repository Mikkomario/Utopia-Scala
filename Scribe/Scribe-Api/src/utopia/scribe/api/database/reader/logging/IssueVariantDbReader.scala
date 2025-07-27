package utopia.scribe.api.database.reader.logging

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.api.database.storable.logging.IssueVariantDbModel
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading issue variant data from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueVariantDbReader 
	extends DbRowReader[IssueVariant] with ParseTableModel[IssueVariant] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = IssueVariantDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(IssueVariant(valid(this.model.id.name).getInt, 
			IssueVariantData(valid(this.model.issueId.name).getInt, 
			Version(valid(this.model.version.name).getString), valid(this.model.errorId.name).int, 
			valid(this.model.details.name).notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }, 
			valid(this.model.created.name).getInstant)))
}

