package utopia.logos.database.reader.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosContext
import utopia.logos.database.storable.url.LinkDbModel
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading link data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object LinkDbReader extends DbRowReader[StoredLink] with ParseTableModel[StoredLink] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = LinkDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(StoredLink(valid(this.model.id.name).getInt, LinkData(valid(this.model.pathId.name).getInt,
			valid(this.model.queryParameters.name).notEmpty match {
				case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel
				case None => Model.empty
			},
			valid(this.model.created.name).getInstant)))
}

