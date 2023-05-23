package utopia.scribe.core.model.partial.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible

object ErrorRecordData extends FromModelFactoryWithSchema[ErrorRecordData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("exceptionType", StringType, Vector("exception_type")), 
			PropertyDeclaration("stackTraceId", IntType, Vector("stack_trace_id")), 
			PropertyDeclaration("causeId", IntType, Vector("cause_id"), isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		ErrorRecordData(valid("exceptionType").getString, valid("stackTraceId").getInt, valid("causeId").int)
}

/**
  * Represents a single error or exception thrown during program runtime
  * @param exceptionType The name of this exception type. Typically the exception class name.
  * @param stackTraceId Id of the topmost stack trace element that corresponds to this error record
  * 
	@param causeId Id of the underlying error that caused this error/failure. None if this error represents the
  *  root problem.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class ErrorRecordData(exceptionType: String, stackTraceId: Int, causeId: Option[Int] = None) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("exceptionType" -> exceptionType, "stackTraceId" -> stackTraceId, "causeId" -> causeId))
}

