package utopia.scribe.core.model.cached.logging

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{ModelType, StringType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.operator.ApproxEquals

import scala.util.Try

object RecordableError extends FromModelFactory[RecordableError]
{
	// ATTRIBUTES   -------------------
	
	private lazy val schema = ModelDeclaration(
		PropertyDeclaration("className", StringType, Vector("class", "name", "type")),
		PropertyDeclaration("stackTrace", ModelType, Vector("stack", "stack_trace")),
		PropertyDeclaration("message", StringType, isOptional = true),
		PropertyDeclaration("cause", ModelType, isOptional = true)
	)
	
	
	// IMPLEMENTED  -------------------
	
	override def apply(model: ModelLike[Property]): Try[RecordableError] =
		schema.validate(model).toTry.flatMap { model =>
			StackTrace(model("stackTrace").getModel).map { stack =>
				apply(model("className"), stack, model("cause").model.flatMap { apply(_).toOption }, model("message"))
			}
		}
	
	
	// OTHER    -----------------------
	
	/**
	  * @param t A throwable item
	  * @return An error based on that item
	  */
	def apply(t: Throwable): Option[RecordableError] = StackTrace.from(t).map { stackTrace =>
		apply(t.getClass.getSimpleName, stackTrace, Option(t.getCause).flatMap(apply),
			Option(t.getMessage).getOrElse(""))
	}
}

/**
  * Represents an error or some other throwable item
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  * @constructor Creates a new error
  * @param className Name of this error type / class
  * @param stackTrace Stack trace associated with this error
  * @param cause The cause of this error (if applicable)
  * @param message Message associated with this error
  */
case class RecordableError(className: String, stackTrace: StackTrace, cause: Option[RecordableError] = None,
                           message: String = "")
	extends ModelConvertible with ApproxEquals[RecordableError]
{
	// COMPUTED ---------------------------
	
	/**
	  * @return An iterator that returns nested errors from the highest to the lowest (i.e. root cause),
	  *         starting with this error.
	  */
	def topToBottomIterator = OptionsIterator.iterate(Some(this)) { _.cause }
	
	/**
	  * @return A vector that contains this error and all the causing errors
	  *         from top (i.e. this) to bottom (i.e. root cause)
	  */
	def topToBottom = topToBottomIterator.toVector
	/**
	  * @return A vector that contains this error and all the causing errors
	  *         from bottom (i.e. root cause) to top (i.e. this)
	  */
	def bottomToTop = topToBottom.reverse
	
	/**
	  * @return An iterator that returns the listed distinct messages associated with these errors
	  */
	def messagesIterator = topToBottomIterator.map { _.message }.filter { _.nonEmpty }.distinct
	/**
	  * @return The distinct messages listed in this error stack, from top to bottom
	  */
	def messages = messagesIterator.toVector
	
	
	// IMPLEMENTED  -----------------------
	
	override def ~==(other: RecordableError): Boolean =
		className == other.className && stackTrace == other.stackTrace &&
			((cause.isEmpty && other.cause.isEmpty) || cause.exists { a => other.cause.exists { a ~== _ } })
	
	override def toModel: Model =
		Model.from("className" -> className, "stackTrace" -> stackTrace, "message" -> message,
			"cause" -> cause)
}