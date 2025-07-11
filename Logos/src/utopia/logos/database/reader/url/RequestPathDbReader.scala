package utopia.logos.database.reader.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading request path data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object RequestPathDbReader 
	extends DbRowReader[RequestPath] with ParseTableModel[RequestPath] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = RequestPathDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(RequestPath(valid(this.model.id.name).getInt, 
			RequestPathData(valid(this.model.domainId.name).getInt, valid(this.model.path.name).getString, 
			valid(this.model.created.name).getInstant)))
}

