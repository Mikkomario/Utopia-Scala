package utopia.scribe.api.database.model.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.StackTraceElementRecordFactory
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * 
	Used for constructing StackTraceElementRecordModel instances and for inserting stack trace element records to
  *  the database
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object StackTraceElementRecordModel 
	extends DataInserter[StackTraceElementRecordModel, StackTraceElementRecord, StackTraceElementRecordData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains stack trace element record file name
	  */
	val fileNameAttName = "fileName"
	
	/**
	  * Name of the property that contains stack trace element record class name
	  */
	val classNameAttName = "className"
	
	/**
	  * Name of the property that contains stack trace element record method name
	  */
	val methodNameAttName = "methodName"
	
	/**
	  * Name of the property that contains stack trace element record line number
	  */
	val lineNumberAttName = "lineNumber"
	
	/**
	  * Name of the property that contains stack trace element record cause id
	  */
	val causeIdAttName = "causeId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains stack trace element record file name
	  */
	def fileNameColumn = table(fileNameAttName)
	
	/**
	  * Column that contains stack trace element record class name
	  */
	def classNameColumn = table(classNameAttName)
	
	/**
	  * Column that contains stack trace element record method name
	  */
	def methodNameColumn = table(methodNameAttName)
	
	/**
	  * Column that contains stack trace element record line number
	  */
	def lineNumberColumn = table(lineNumberAttName)
	
	/**
	  * Column that contains stack trace element record cause id
	  */
	def causeIdColumn = table(causeIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = StackTraceElementRecordFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: StackTraceElementRecordData) = 
		apply(None, data.fileName, data.className, data.methodName, data.lineNumber, data.causeId)
	
	override protected def complete(id: Value, data: StackTraceElementRecordData) = 
		StackTraceElementRecord(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * 
		@param causeId Id of the stack trace element that originated this element. I.e. the element directly before
	  *  this element. 
	  * None if this is the root element.
	  * @return A model containing only the specified cause id
	  */
	def withCauseId(causeId: Int) = apply(causeId = Some(causeId))
	
	/**
	  * @param className Name of the class in which this event was recorded. 
	  * Empty if the class name is identical with the file name.
	  * @return A model containing only the specified class name
	  */
	def withClassName(className: String) = apply(className = className)
	
	/**
	  * @param fileName Name of the file in which this event was recorded
	  * @return A model containing only the specified file name
	  */
	def withFileName(fileName: String) = apply(fileName = fileName)
	
	/**
	  * @param id A stack trace element record id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param lineNumber The code line number where this event was recorded. None if not available.
	  * @return A model containing only the specified line number
	  */
	def withLineNumber(lineNumber: Int) = apply(lineNumber = Some(lineNumber))
	
	/**
	  * @param methodName Name of the method where this event was recorded. Empty if unknown.
	  * @return A model containing only the specified method name
	  */
	def withMethodName(methodName: String) = apply(methodName = methodName)
}

/**
  * Used for interacting with StackTraceElementRecords in the database
  * @param id stack trace element record database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElementRecordModel(id: Option[Int] = None, fileName: String = "", 
	className: String = "", methodName: String = "", lineNumber: Option[Int] = None, 
	causeId: Option[Int] = None) 
	extends StorableWithFactory[StackTraceElementRecord]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementRecordModel.factory
	
	override def valueProperties = {
		import StackTraceElementRecordModel._
		Vector("id" -> id, fileNameAttName -> fileName, classNameAttName -> className, 
			methodNameAttName -> methodName, lineNumberAttName -> lineNumber, causeIdAttName -> causeId)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * 
		@param causeId Id of the stack trace element that originated this element. I.e. the element directly before
	  *  this element. 
	  * None if this is the root element.
	  * @return A new copy of this model with the specified cause id
	  */
	def withCauseId(causeId: Int) = copy(causeId = Some(causeId))
	
	/**
	  * @param className Name of the class in which this event was recorded. 
	  * Empty if the class name is identical with the file name.
	  * @return A new copy of this model with the specified class name
	  */
	def withClassName(className: String) = copy(className = className)
	
	/**
	  * @param fileName Name of the file in which this event was recorded
	  * @return A new copy of this model with the specified file name
	  */
	def withFileName(fileName: String) = copy(fileName = fileName)
	
	/**
	  * @param lineNumber The code line number where this event was recorded. None if not available.
	  * @return A new copy of this model with the specified line number
	  */
	def withLineNumber(lineNumber: Int) = copy(lineNumber = Some(lineNumber))
	
	/**
	  * @param methodName Name of the method where this event was recorded. Empty if unknown.
	  * @return A new copy of this model with the specified method name
	  */
	def withMethodName(methodName: String) = copy(methodName = methodName)
}

