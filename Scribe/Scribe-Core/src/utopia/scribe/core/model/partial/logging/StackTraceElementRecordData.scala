package utopia.scribe.core.model.partial.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible

object StackTraceElementRecordData extends FromModelFactoryWithSchema[StackTraceElementRecordData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema =
		ModelDeclaration(Vector(PropertyDeclaration("className", StringType, Vector("class_name")),
			PropertyDeclaration("methodName", StringType, Vector("method_name")),
			PropertyDeclaration("lineNumber", IntType, Vector("line_number")), PropertyDeclaration("causeId",
			IntType, Vector("cause_id"), isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) =
		StackTraceElementRecordData(valid("className").getString, valid("methodName").getString,
			valid("lineNumber").getInt, valid("causeId").int)
}

/**
  * Represents a single error stack trace line.
  * A stack trace indicates how an error propagated through the program flow before it was recorded.
  * @param className The class where this event was recorded.
  * @param methodName The name of the class method where this event was recorded
  * @param lineNumber The code line number where this event was recorded
  * @param causeId Id of the stack trace element that originated this element. I.e. the element directly before
  *  this element. None if this is the root element.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class StackTraceElementRecordData(className: String, methodName: String, lineNumber: Int,
                                       causeId: Option[Int] = None)
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("className" -> className, "methodName" -> methodName, "lineNumber" -> lineNumber, 
			"causeId" -> causeId))
}

