package utopia.scribe.api.database.storable.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.logging.StackTraceElementRecordFactory
import utopia.scribe.core.model.partial.logging.StackTraceElementRecordData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

/**
  * Used for constructing StackTraceElementRecordDbModel instances and for inserting stack trace 
  * element records to the database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object StackTraceElementRecordDbModel 
	extends StorableFactory[StackTraceElementRecordDbModel, StackTraceElementRecord, StackTraceElementRecordData] 
		with FromIdFactory[Int, StackTraceElementRecordDbModel] with HasIdProperty 
		with StackTraceElementRecordFactory[StackTraceElementRecordDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with file names
	  */
	lazy val fileName = property("fileName")
	
	/**
	  * Database property used for interacting with class names
	  */
	lazy val className = property("className")
	
	/**
	  * Database property used for interacting with method names
	  */
	lazy val methodName = property("methodName")
	
	/**
	  * Database property used for interacting with line numbers
	  */
	lazy val lineNumber = property("lineNumber")
	
	/**
	  * Database property used for interacting with cause ids
	  */
	lazy val causeId = property("causeId")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.stackTraceElementRecord
	
	override def apply(data: StackTraceElementRecordData): StackTraceElementRecordDbModel = 
		apply(None, data.fileName, data.className, data.methodName, data.lineNumber, data.causeId)
	
	/**
	  * @param causeId Id of the stack trace element that originated this element. I.e. the element 
	  *                directly before this element. 
	  *                None if this is the root element.
	  * @return A model containing only the specified cause id
	  */
	override def withCauseId(causeId: Int) = apply(causeId = Some(causeId))
	
	/**
	  * @param className Name of the class in which this event was recorded. 
	  *                  Empty if the class name is identical with the file name.
	  * @return A model containing only the specified class name
	  */
	override def withClassName(className: String) = apply(className = className)
	
	/**
	  * @param fileName Name of the file in which this event was recorded
	  * @return A model containing only the specified file name
	  */
	override def withFileName(fileName: String) = apply(fileName = fileName)
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param lineNumber The code line number where this event was recorded. None if not available.
	  * @return A model containing only the specified line number
	  */
	override def withLineNumber(lineNumber: Int) = apply(lineNumber = Some(lineNumber))
	
	/**
	  * @param methodName Name of the method where this event was recorded. Empty if unknown.
	  * @return A model containing only the specified method name
	  */
	override def withMethodName(methodName: String) = apply(methodName = methodName)
	
	override protected def complete(id: Value, data: StackTraceElementRecordData) = 
		StackTraceElementRecord(id.getInt, data)
}

/**
  * Used for interacting with StackTraceElementRecords in the database
  * @param id stack trace element record database id
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
case class StackTraceElementRecordDbModel(id: Option[Int] = None, fileName: String = "", 
	className: String = "", methodName: String = "", lineNumber: Option[Int] = None, 
	causeId: Option[Int] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, StackTraceElementRecordDbModel] 
		with StackTraceElementRecordFactory[StackTraceElementRecordDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(StackTraceElementRecordDbModel.id.name -> id, 
			StackTraceElementRecordDbModel.fileName.name -> fileName, 
			StackTraceElementRecordDbModel.className.name -> className, 
			StackTraceElementRecordDbModel.methodName.name -> methodName, 
			StackTraceElementRecordDbModel.lineNumber.name -> lineNumber, 
			StackTraceElementRecordDbModel.causeId.name -> causeId)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = StackTraceElementRecordDbModel.table
	
	/**
	  * @param causeId Id of the stack trace element that originated this element. I.e. the element 
	  *                directly before this element. 
	  *                None if this is the root element.
	  * @return A new copy of this model with the specified cause id
	  */
	override def withCauseId(causeId: Int) = copy(causeId = Some(causeId))
	
	/**
	  * @param className Name of the class in which this event was recorded. 
	  *                  Empty if the class name is identical with the file name.
	  * @return A new copy of this model with the specified class name
	  */
	override def withClassName(className: String) = copy(className = className)
	
	/**
	  * @param fileName Name of the file in which this event was recorded
	  * @return A new copy of this model with the specified file name
	  */
	override def withFileName(fileName: String) = copy(fileName = fileName)
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param lineNumber The code line number where this event was recorded. None if not available.
	  * @return A new copy of this model with the specified line number
	  */
	override def withLineNumber(lineNumber: Int) = copy(lineNumber = Some(lineNumber))
	
	/**
	  * @param methodName Name of the method where this event was recorded. Empty if unknown.
	  * @return A new copy of this model with the specified method name
	  */
	override def withMethodName(methodName: String) = copy(methodName = methodName)
}

