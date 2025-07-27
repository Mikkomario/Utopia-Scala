package utopia.scribe.api.database.access.logging.error.stack

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.StackTraceElementRecordDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing stack trace element record values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessStackTraceElementRecordValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing stack trace element record database properties
	  */
	val model = StackTraceElementRecordDbModel
	
	/**
	  * Access to stack trace element record ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Name of the file in which this event was recorded
	  */
	lazy val fileNames = apply(model.fileName) { v => v.getString }
	
	/**
	  * Name of the class in which this event was recorded. 
	  * Empty if the class name is identical with the file name.
	  */
	lazy val classNames = apply(model.className) { v => v.getString }
	
	/**
	  * Name of the method where this event was recorded. Empty if unknown.
	  */
	lazy val methodNames = apply(model.methodName) { v => v.getString }
	
	/**
	  * The code line number where this event was recorded. None if not available.
	  */
	lazy val lineNumbers = apply(model.lineNumber).flatten { v => v.int }
	
	/**
	  * Id of the stack trace element that originated this element. I.e. the element directly before 
	  * this element. 
	  * None if this is the root element.
	  */
	lazy val causeIds = apply(model.causeId).flatten { v => v.int }
}

