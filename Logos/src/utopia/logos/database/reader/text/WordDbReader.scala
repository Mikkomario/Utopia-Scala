package utopia.logos.database.reader.text

import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.storable.text.WordDbModel
import utopia.logos.model.partial.text.WordData
import utopia.logos.model.stored.text.StoredWord
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading word data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object WordDbReader extends DbRowReader[StoredWord] with ParseTableModel[StoredWord] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = WordDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(StoredWord(valid(this.model.id.name).getInt, WordData(valid(this.model.text.name).getString, 
			valid(this.model.created.name).getInstant)))
}

