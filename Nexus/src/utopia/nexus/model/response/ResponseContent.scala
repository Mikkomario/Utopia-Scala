package utopia.nexus.model.response

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.MaybeEmpty
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View

import scala.language.implicitConversions

object ResponseContent
{
	// ATTRIBUTES   ---------------------------
	
	/**
	 * An empty response content instance
	 */
	lazy val empty = apply(Value.empty, "")
	
	
	// IMPLICIT -------------------------------
	
	/**
	 * Implicitly wraps a [[Value]] into a ResponseContent
	 * @param value Value to wrap
	 * @return ResponseContent based on that value
	 */
	implicit def wrap(value: Value): ResponseContent = apply(value, "")
	/**
	 * Implicitly converts an item into a [[Value]], wrapping it
	 * @param value An item to convert into a value and a ResponseContent
	 * @param convert An implicit function for converting the item into a value
	 * @tparam V Type of the specified item
	 * @return ResponseContent wrapping the specified item, as a value
	 */
	implicit def autoConvert[V](value: V)(implicit convert: V => ValueConvertible): ResponseContent =
		apply(value.toValue, "")
	/**
	 * Implicitly wraps a Value + description -pair
	 * @param describedValue A [[Value]], plus a description of that value
	 * @return Response content containing both the specified value and the specified description
	 */
	implicit def apply(describedValue: (Value, String)): ResponseContent = apply(describedValue._1, describedValue._2)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param description A description to wrap as response content
	 * @return A response content that only contains the specified description
	 */
	def description(description: String) = apply(Value.empty, description)
}

/**
 * Contains data (a value) and/or a description, which may be served for the client in some format
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
case class ResponseContent(value: Value, description: String) extends View[Value] with MaybeEmpty[ResponseContent]
{
	// IMPLEMENTED  --------------------
	
	override def self: ResponseContent = this
	override def isEmpty: Boolean = value.isEmpty && description.isEmpty
	
	
	// OTHER    ------------------------
	
	/**
	 * @param value New value to assign
	 * @return Copy of this content with the specified value
	 */
	def withValue(value: Value) = copy(value = value)
	/**
	 * @param description A description of this content
	 * @return A copy of this content with the specified description
	 */
	def withDescription(description: String) = copy(description = description)
	
	/**
	 * @param f A mapping function applied to this content's value
	 * @return A copy of this content with a mapped value
	 */
	def mapValue(f: Mutate[Value]) = withValue(f(value))
	/**
	 * @param f A mapping function applied to this content's description
	 * @return A copy of this content with a mapped description
	 */
	def mapDescription(f: Mutate[String]) = withDescription(f(description))
}