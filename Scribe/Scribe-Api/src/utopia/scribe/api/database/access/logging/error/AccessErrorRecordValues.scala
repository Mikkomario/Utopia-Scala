package utopia.scribe.api.database.access.logging.error

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.ErrorRecordDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing error record values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
case class AccessErrorRecordValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing error record database properties
	  */
	val model = ErrorRecordDbModel
	
	/**
	  * Access to error record ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * The name of this exception type. Typically the exception class name.
	  */
	lazy val exceptionTypes = apply(model.exceptionType) { v => v.getString }
	
	/**
	  * Id of the topmost stack trace element that corresponds to this error record
	  */
	lazy val stackTraceIds = apply(model.stackTraceId) { v => v.getInt }
	
	/**
	  * Id of the underlying error that caused this error/failure. None if this error represents the 
	  * root problem.
	  */
	lazy val causeIds = apply(model.causeId).flatten { v => v.int }
}

