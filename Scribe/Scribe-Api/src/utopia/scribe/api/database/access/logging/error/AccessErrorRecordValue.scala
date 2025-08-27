package utopia.scribe.api.database.access.logging.error

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.ErrorRecordDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual error record values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
case class AccessErrorRecordValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing error record database properties
	  */
	val model = ErrorRecordDbModel
	
	/**
	  * Access to error record id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * The name of this exception type. Typically the exception class name.
	  */
	lazy val exceptionType = apply(model.exceptionType) { v => v.getString }
	
	/**
	  * Id of the topmost stack trace element that corresponds to this error record
	  */
	lazy val stackTraceId = apply(model.stackTraceId).optional { v => v.int }
	
	/**
	  * Id of the underlying error that caused this error/failure. None if this error represents the 
	  * root problem.
	  */
	lazy val causeId = apply(model.causeId).optional { v => v.int }
}

