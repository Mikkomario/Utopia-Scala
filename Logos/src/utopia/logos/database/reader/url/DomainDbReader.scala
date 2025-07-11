package utopia.logos.database.reader.url

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading domain data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object DomainDbReader extends DbRowReader[Domain] with ParseTableModel[Domain] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = DomainDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Domain(valid(this.model.id.name).getInt, DomainData(valid(this.model.url.name).getString, 
			valid(this.model.created.name).getInstant)))
}

