package utopia.scribe.core.model.partial.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.util.StringExtensions._

object StackTraceElementRecordData extends FromModelFactoryWithSchema[StackTraceElementRecordData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = ModelDeclaration(Vector(
		PropertyDeclaration("fileName", StringType, Vector("file_name")),
		PropertyDeclaration("className", StringType, Vector("class_name"), isOptional = true),
		PropertyDeclaration("methodName", StringType, Vector("method_name"), isOptional = true),
		PropertyDeclaration("lineNumber", IntType, Vector("line_number"), isOptional = true),
		PropertyDeclaration("causeId", IntType, Vector("cause_id"), isOptional = true)
	))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) =
		StackTraceElementRecordData(valid("fileName").getString, valid("className").getString,
			valid("methodName").getString, valid("lineNumber").int, valid("causeId").int)
}

/**
  * Represents a single error stack trace line.
  * A stack trace indicates how an error propagated through the program flow before it was recorded.
  * @param fileName Name of the file in which this event was recorded
  * @param className Name of the class in which this event was recorded. 
  * Empty if the class name is identical with the file name.
  * @param methodName Name of the method where this event was recorded. Empty if unknown.
  * @param lineNumber The code line number where this event was recorded. None if not available.
  * @param causeId Id of the stack trace element that originated this element. I.e. the element directly before
  *  this element. 
  * None if this is the root element.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElementRecordData(fileName: String, className: String = "", methodName: String = "", 
	lineNumber: Option[Int] = None, causeId: Option[Int] = None) 
	extends ModelConvertible
{
	// COMPUTED ------------------------
	
	/**
	  * @return Name of the file and class of this stack trace element.
	  *         If the two are identical, only returns the file name.
	  */
	def fileAndClassName = if (className.isEmpty) fileName else s"$fileName: $className"
	/**
	  * @return A displayable class name fot this stack trace record.
	  */
	def nonEmptyClassName = className.nonEmptyOrElse(fileName)
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("fileName" -> fileName, "className" -> className, "methodName" -> methodName, 
			"lineNumber" -> lineNumber, "causeId" -> causeId))
}

