package utopia.scribe.api.database.access.logging.error.stack

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.StackTraceElementRecordDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual stack trace element record values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessStackTraceElementRecordValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing stack trace element record database properties
	  */
	val model = StackTraceElementRecordDbModel
	
	/**
	  * Access to stack trace element record id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Name of the file in which this event was recorded
	  */
	lazy val fileName = apply(model.fileName) { v => v.getString }
	
	/**
	  * Name of the class in which this event was recorded. 
	  * Empty if the class name is identical with the file name.
	  */
	lazy val className = apply(model.className) { v => v.getString }
	
	/**
	  * Name of the method where this event was recorded. Empty if unknown.
	  */
	lazy val methodName = apply(model.methodName) { v => v.getString }
	
	/**
	  * The code line number where this event was recorded. None if not available.
	  */
	lazy val lineNumber = apply(model.lineNumber).optional { v => v.int }
	
	/**
	  * Id of the stack trace element that originated this element. I.e. the element directly before 
	  * this element. 
	  * None if this is the root element.
	  */
	lazy val causeId = apply(model.causeId).optional { v => v.int }
}

