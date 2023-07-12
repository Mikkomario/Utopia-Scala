package utopia.scribe.core.model.cached.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType, StringType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.parse.string.Regex
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.MutatingOnce

import scala.util.Try

object StackTrace extends FromModelFactory[StackTrace]
{
	// TYPES    ---------------------
	
	/**
	  * The Java version of the StackTraceElement class
	  */
	type JStackTraceElement = java.lang.StackTraceElement
	
	
	// ATTRIBUTES   -----------------
	
	private val methodSplitter = Regex.escape('$')
	
	private lazy val schema = ModelDeclaration(
		PropertyDeclaration("class", StringType, Vector("className", "class_name"), "CouldNotParse"),
		PropertyDeclaration("method", StringType, Vector("methodName", "method_name", "function"), "couldNotParse"),
		PropertyDeclaration("line", IntType, Vector("lineNumber", "line_number"), -1),
		PropertyDeclaration("cause", ModelType, isOptional = true)
	)
	
	
	// IMPLEMENTED  ----------------
	
	override def apply(model: ModelLike[Property]): Try[StackTrace] =
		schema.validate(model).toTry.map { model =>
			apply(model("class"), model("method"), model("line"), model("cause").model.flatMap { apply(_).toOption })
		}
	
	
	// OTHER    --------------------
	
	/**
	  * @param t A throwable
	  * @return The stack trace from that throwable item.
	  *         None if no stack trace could be extracted.
	  */
	def from(t: Throwable): Option[StackTrace] = from(t.getStackTrace)
	/**
	  * Converts a Java stack trace into a StackTraceElement
	  * @param elements The Java stack trace elements to convert
	  * @return A stack trace element from the specified Java versions
	  */
	def from(elements: IterableOnce[JStackTraceElement]) = {
		val elementIterator = elements.iterator
		elementIterator.nextOption().map { top =>
			_from(top) { elementIterator.nextOption() }
		}
	}
	// nextElement will be called until it returns None
	private def _from(element: JStackTraceElement)(nextElement: => Option[JStackTraceElement]): StackTrace = {
		// Converts the dollar-sign -ending class names to those that don't end with a dollar sign
		// Removes the package part, also
		val className = NotEmpty(element.getClassName.split('.').takeRightWhile { _.head.isUpper }.toVector) match {
			case Some(elements) => elements.map { _.dropRightWhile { _ == '$' } }.mkString(".")
			case None => element.getClassName.dropRightWhile { _ == '$' }
		}
		// Splits method names by dollar sign, if applicable
		val methodName = element.getMethodName.split(methodSplitter)
			// Doesn't use digit method names (e.g. "apply$1" would otherwise become "1")
			// Also ignores empty names (e.g. "flatMap$" now becomes "flatMap" instead of "")
			.reverseIterator.filterNot { _.forall { _.isDigit } }
			.nextOption().getOrElse("")
		apply(className, methodName, element.getLineNumber, nextElement.map { _from(_)(nextElement) })
	}
}

/**
  * Contains information about an exception's stack trace.
  * See the Java version of this class for more detailed information.
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  * @constructor Creates a new stack trace element
  * @param className Name of the class where this event occurred
  * @param methodName Name of the method where this event occurred
  * @param lineNumber Index of the line (1-based) where this event occurred
  * @param cause Stack trace element / event that occurred before this element. None if this was the first occurrence.
  */
// TODO: LineNumber should be optional
case class StackTrace(className: String, methodName: String, lineNumber: Int, cause: Option[StackTrace] = None)
	extends ModelConvertible
{
	// COMPUTED -------------------------
	
	/**
	  * @return A string that represents this individual stack trace element
	  */
	def logLine = s"$className.$methodName (line $lineNumber)"
	/**
	  * @return An iterator that returns one line for each stack trace element in this stack
	  */
	def logLinesIterator = {
		// Groups by class, then by method (consecutive only)
		topToBottomIterator.groupBy { _.className }.flatMap { case (className, elements) =>
			val prefix = MutatingOnce(className)("\t")
			elements.iterator.groupBy { _.methodName }.map { case (methodName, elements) =>
				// Groups multiple line number instances to a single set
				val lineNumberStr = elements.oneOrMany match {
					case Left(element) => element.lineNumber.toString
					case Right(elements) => s"[${elements.map { _.lineNumber }.mkString(", ")}]"
				}
				s"${prefix.value}.$methodName: $lineNumberStr"
			}
		}
	}
	
	/**
	  * @return An iterator that returns stack trace (elements) from the top element (this) to the bottom element.
	  *         Contains at least 1 item.
	  */
	def topToBottomIterator = OptionsIterator.iterate(Some(this)) { _.cause }
	
	/**
	  * @return A vector that contains all elements in this stack from the top (this) to the bottom.
	  *         Always contains at least 1 element.
	  */
	def topToBottom = topToBottomIterator.toVector
	/**
	  * @return A vector that contains all elements in this stack from the bottom to the top (this).
	  *         Always contains at least 1 element.
	  */
	def bottomToTop = topToBottom.reverse
	
	
	// IMPLEMENTED  --------------------
	
	override def toString = logLinesIterator.mkString("\n")
	
	override def toModel: Model =
		Model.from("class" -> className, "method" -> methodName, "line" -> lineNumber, "cause" -> cause)
}
