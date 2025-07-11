package utopia.logos.database.reader.text

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.logos.model.partial.text.DelimiterData
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading delimiter data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object DelimiterDbReader extends DbRowReader[Delimiter] with ParseTableModel[Delimiter] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = DelimiterDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Delimiter(valid(this.model.id.name).getInt, 
			DelimiterData(valid(this.model.text.name).getString, valid(this.model.created.name).getInstant)))
}

