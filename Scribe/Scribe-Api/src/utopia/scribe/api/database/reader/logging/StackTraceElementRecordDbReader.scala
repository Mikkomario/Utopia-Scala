package utopia.scribe.api.database.reader.logging

import utopia.flow.generic.model.immutable.Model
import utopia.scribe.api.database.storable.logging.StackTraceElementRecordDbModel
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel

import scala.util.Success

/**
  * Used for reading stack trace element record data from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object StackTraceElementRecordDbReader 
	extends DbRowReader[StackTraceElementRecord] with ParseTableModel[StackTraceElementRecord] 
		with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = StackTraceElementRecordDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(StackTraceElementRecord(valid(this.model.id.name).getInt, 
			StackTraceElementRecordData(valid(this.model.fileName.name).getString, 
			valid(this.model.className.name).getString, valid(this.model.methodName.name).getString, 
			valid(this.model.lineNumber.name).int, valid(this.model.causeId.name).int)))
}

