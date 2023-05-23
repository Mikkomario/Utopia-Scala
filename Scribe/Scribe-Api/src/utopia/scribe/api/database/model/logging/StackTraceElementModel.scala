package utopia.scribe.api.database.model.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.factory.logging.StackTraceElementFactory
import utopia.scribe.core.model.partial.logging.StackTraceElementData
import utopia.scribe.core.model.stored.logging.StackTraceElement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * 
	Used for constructing StackTraceElementModel instances and for inserting stack trace elements to the database
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object StackTraceElementModel 
	extends DataInserter[StackTraceElementModel, StackTraceElement, StackTraceElementData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains stack trace element class name
	  */
	val classNameAttName = "className"
	
	/**
	  * Name of the property that contains stack trace element method name
	  */
	val methodNameAttName = "methodName"
	
	/**
	  * Name of the property that contains stack trace element line number
	  */
	val lineNumberAttName = "lineNumber"
	
	/**
	  * Name of the property that contains stack trace element cause id
	  */
	val causeIdAttName = "causeId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains stack trace element class name
	  */
	def classNameColumn = table(classNameAttName)
	
	/**
	  * Column that contains stack trace element method name
	  */
	def methodNameColumn = table(methodNameAttName)
	
	/**
	  * Column that contains stack trace element line number
	  */
	def lineNumberColumn = table(lineNumberAttName)
	
	/**
	  * Column that contains stack trace element cause id
	  */
	def causeIdColumn = table(causeIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = StackTraceElementFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: StackTraceElementData) = 
		apply(None, data.className, data.methodName, Some(data.lineNumber), data.causeId)
	
	override protected def complete(id: Value, data: StackTraceElementData) = StackTraceElement(id.getInt, 
		data)
	
	
	// OTHER	--------------------
	
	/**
	  * 
		@param causeId Id of the stack trace element that originated this element. I.e. the element directly before
	  *  this element. None if this is the root element.
	  * @return A model containing only the specified cause id
	  */
	def withCauseId(causeId: Int) = apply(causeId = Some(causeId))
	
	/**
	  * @param className The class where this event was recorded.
	  * @return A model containing only the specified class name
	  */
	def withClassName(className: String) = apply(className = className)
	
	/**
	  * @param id A stack trace element id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param lineNumber The code line number where this event was recorded
	  * @return A model containing only the specified line number
	  */
	def withLineNumber(lineNumber: Int) = apply(lineNumber = Some(lineNumber))
	
	/**
	  * @param methodName The name of the class method where this event was recorded
	  * @return A model containing only the specified method name
	  */
	def withMethodName(methodName: String) = apply(methodName = methodName)
}

/**
  * Used for interacting with StackTraceElements in the database
  * @param id stack trace element database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElementModel(id: Option[Int] = None, className: String = "", methodName: String = "", 
	lineNumber: Option[Int] = None, causeId: Option[Int] = None) 
	extends StorableWithFactory[StackTraceElement]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StackTraceElementModel.factory
	
	override def valueProperties = {
		import StackTraceElementModel._
		Vector("id" -> id, classNameAttName -> className, methodNameAttName -> methodName, 
			lineNumberAttName -> lineNumber, causeIdAttName -> causeId)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * 
		@param causeId Id of the stack trace element that originated this element. I.e. the element directly before
	  *  this element. None if this is the root element.
	  * @return A new copy of this model with the specified cause id
	  */
	def withCauseId(causeId: Int) = copy(causeId = Some(causeId))
	
	/**
	  * @param className The class where this event was recorded.
	  * @return A new copy of this model with the specified class name
	  */
	def withClassName(className: String) = copy(className = className)
	
	/**
	  * @param lineNumber The code line number where this event was recorded
	  * @return A new copy of this model with the specified line number
	  */
	def withLineNumber(lineNumber: Int) = copy(lineNumber = Some(lineNumber))
	
	/**
	  * @param methodName The name of the class method where this event was recorded
	  * @return A new copy of this model with the specified method name
	  */
	def withMethodName(methodName: String) = copy(methodName = methodName)
}

