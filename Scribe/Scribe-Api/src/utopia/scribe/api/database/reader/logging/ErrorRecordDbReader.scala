package utopia.scribe.api.database.reader.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.logging.ErrorRecordDbModel
import utopia.scribe.core.model.partial.logging.ErrorRecordData
import utopia.scribe.core.model.stored.logging.ErrorRecord
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading error record data from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object ErrorRecordDbReader 
	extends DbRowReader[ErrorRecord] with ParseTableModel[ErrorRecord] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = ErrorRecordDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(ErrorRecord(valid(this.model.id.name).getInt, 
			ErrorRecordData(valid(this.model.exceptionType.name).getString, 
			valid(this.model.stackTraceId.name).getInt, valid(this.model.causeId.name).int)))
}

